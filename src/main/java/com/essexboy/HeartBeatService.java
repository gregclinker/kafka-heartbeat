package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public class HeartBeatService {

    final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatService.class);

    private final HeartBeatConfig config;
    private final ProcessedStats processedStats = new ProcessedStats();
    private boolean sucessfulSwitch = true;

    public HeartBeatService(HeartBeatConfig config) {
        this.config = config;
    }

    public void switchDown() {
        sucessfulSwitch = true;
        processedStats.reset();
        LOGGER.info("started switchDown");
        LOGGER.info("started switching minISR to 1");
        switchIsrToOne();
        LOGGER.info("finished switchIsrToOne, success={}, stats={}", sucessfulSwitch, processedStats);
        processedStats.reset();
        LOGGER.info("started rebalancing down to min {} replicas", config.getReducedIsr());
        rebalance(config.getReducedIsr(), false, config.getRebalanceDownDelay(), config.getReducedIsr());
        LOGGER.info("finished switchDown, success={}, stats={}", sucessfulSwitch, processedStats);
    }

    public void switchBack() {
        sucessfulSwitch = true;
        processedStats.reset();
        LOGGER.info("started switchBack");
        LOGGER.info("started rebalancing up to min {} replicas", config.getReplicationFactor());
        rebalance(config.getReplicationFactor(), true, config.getRebalanceUpDelay(), config.getStandardIsr());
        LOGGER.info("finished switchBack, success={}, stats={}", sucessfulSwitch, processedStats);
    }

    public void rebalance(int requiredReplicas, boolean extend, int delay, int newMinIsr) {
        for (String topic : config.getTopics()) {
            processedStats.topicsRebalanceProcessed++;
            processedStats.topicsMinIsrProcessed++;
            if (rebalance(topic, requiredReplicas, extend, delay)) {
                if (newMinIsr > getTopicData(topic).getPartitionMinIsr()) {
                    processedStats.topicsMinIsrSkipped++;
                    LOGGER.info("SKIPPING switchIsr topic {} to minIsr={}, not enough ISRs", topic, newMinIsr);
                } else {
                    switchIsr(topic, newMinIsr);
                }
            }
        }
    }

    private boolean rebalance(String topic, int requiredReplicas, boolean extend, int delay) {
        final TopicInfo topicInfo = getTopicData(topic);
        boolean successfulRebalance = true;
        final int partitionMinIsr = getTopicData(topic).getPartitionMinIsr();
        if (partitionMinIsr >= requiredReplicas) {
            processedStats.topicsRebalanceSkipped++;
            LOGGER.info("SKIPPING rebalance topic {}, no ISRs less than {}", topic, requiredReplicas);
            return true;
        }
        for (PartitionInfo partition : topicInfo.getPartitions()) {
            processedStats.partitionsRebalanceProcessed++;
            final int isrCount = partition.getIsrs().size();
            if (isrCount >= requiredReplicas) {
                processedStats.partitionsRebalanceSkipped++;
                LOGGER.info("SKIPPING rebalancing topic {}, partition {} already has {} ISRs", topic, partition.getId(), isrCount);
            } else if (partition.getLeader() == null) {
                processedStats.partitionsRebalanceSkipped++;
                LOGGER.info("SKIPPING rebalancing topic {}, partition {}, no leader set", topic, partition.getId());
            } else {
                try {
                    final List<Integer> replicas = getRebalanceReplicas(partition.getLeader(), partition.getId(), extend);
                    partitionReassignment(topic, partition.getId(), replicas);
                    Thread.sleep(delay * 1000);
                } catch (Exception e) {
                    sucessfulSwitch = false;
                    successfulRebalance = false;
                    processedStats.errors++;
                    LOGGER.error("ERROR rebalancing topic {}, partition {}", topic, partition.getId(), e);
                }
            }
        }
        return successfulRebalance;
    }

    private void switchIsrToOne() {
        for (String topic : config.getTopics()) {
            processedStats.topicsMinIsrProcessed++;
            final int partitionMinIsr = getTopicData(topic).getPartitionMinIsr();
            if (partitionMinIsr <= 1) {
                switchIsr(topic, 1);
            } else {
                processedStats.topicsMinIsrSkipped++;
                LOGGER.info("SKIPPING switchIsrToOne topic {}, no ISRs less than {}", topic, partitionMinIsr);
            }
        }
    }

    private void switchIsr(String topic, int newIsr) {
        try {
            if (getMinIsr(topic) != newIsr) {
                setMinIsr(topic, newIsr);
                Thread.sleep(config.getSwitchMinIsrDelay() * 1000L);
            } else {
                processedStats.topicsMinIsrSkipped++;
                LOGGER.info("SKIPPING switchIsr, topic {} already has minIsr of {}", topic, newIsr);
            }
        } catch (Exception e) {
            sucessfulSwitch = false;
            processedStats.errors++;
            LOGGER.error("ERROR in switchIsr to {}, for topic {}", newIsr, topic, e);
        }
    }

    public void setMinIsr(String topic, int minIsr) throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            final ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
            final ConfigEntry configEntry = new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr + "");
            final AlterConfigOp alterConfigOp = new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET);
            final Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>(1);
            configs.put(configResource, Collections.singletonList(alterConfigOp));
            final KafkaFuture<Void> all = adminClient.incrementalAlterConfigs(configs).all();
            while (!all.isDone()) {
                Thread.sleep(100);
            }
            all.get();
            LOGGER.info("minIsr set to {}, for topic {}", minIsr, topic);
        }
    }

    public int getMinIsr(String topic) throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            final Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic));
            final KafkaFuture<Map<ConfigResource, Config>> all = adminClient.describeConfigs(resources).all();
            while (!all.isDone()) {
                Thread.sleep(100);
            }
            final Map<ConfigResource, Config> configResourceConfigMap = all.get();
            final Object[] keys = configResourceConfigMap.keySet().toArray();
            for (ConfigEntry configEntry : configResourceConfigMap.get(keys[0]).entries()) {
                if (configEntry.name().equals(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)) {
                    //LOGGER.debug("getMinIsr returned {}, for topic {}", configEntry.value(), topic);
                    return Integer.parseInt(configEntry.value());
                }
            }
        }
        return 0;
    }

    private List<Integer> getRebalanceReplicas(Integer leader, Integer partition, boolean extend) {
        List<Integer> brokers = new ArrayList<>();
        if (leader <= 3) {
            CollectionUtils.addAll(brokers, Arrays.asList(1, 2, 3));
            if (extend) {
                brokers.add(4 + (partition % 3));
            }
        } else {
            CollectionUtils.addAll(brokers, Arrays.asList(4, 5, 6));
            if (extend) {
                brokers.add(1 + (partition % 3));
            }
        }
        return orderLeaderFirst(brokers, leader);
    }

    public List<Integer> getAvailableBrokers() throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            return adminClient.describeCluster().nodes().get().stream().map(Node::id).sorted().collect(Collectors.toList());
        }
    }

    private void partitionReassignment(String topicName, Integer partition, List<Integer> replicas) throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            Map<TopicPartition, Optional<NewPartitionReassignment>> topicPartitionOptionalHashMap = new HashMap<>();
            topicPartitionOptionalHashMap.put(new TopicPartition(topicName, partition), Optional.of(new NewPartitionReassignment(replicas)));
            adminClient.alterPartitionReassignments(topicPartitionOptionalHashMap).all().get();
            LOGGER.info("replicas set to {} for topic {}, partition {}", replicas, topicName, partition);
        }
    }

    public TopicInfo getTopicData(String topicName) {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName(topicName);
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            final TopicDescription topicDescription = adminClient.describeTopics(List.of(topicName)).topicNameValues().get(topicName).get();
            topicInfo.setPartitions(topicDescription.partitions().stream().map(PartitionInfo::new).collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.error("ERROR getTopicData, topic {}", topicName, e);
        }
        return topicInfo;
    }

    private List<Integer> orderLeaderFirst(List<Integer> replicas, Integer leader) {
        final List<Integer> newReplicas = replicas.stream().filter(i -> !i.equals(leader)).collect(Collectors.toList());
        newReplicas.add(0, leader);
        return newReplicas;
    }

    private static class ProcessedStats {
        protected int topicsMinIsrProcessed;
        protected int topicsMinIsrSkipped;
        protected int topicsRebalanceProcessed;
        protected int topicsRebalanceSkipped;
        protected int partitionsRebalanceProcessed;
        protected int partitionsRebalanceSkipped;
        protected int errors;
        private long startTime;

        public void reset() {
            topicsMinIsrProcessed = 0;
            topicsMinIsrSkipped = 0;
            topicsRebalanceProcessed = 0;
            topicsRebalanceSkipped = 0;
            partitionsRebalanceProcessed = 0;
            partitionsRebalanceSkipped = 0;
            errors = 0;
            startTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ProcessedStats{");
            if (topicsMinIsrProcessed > 0) {
                sb.append("topicsMinIsrProcessed=").append(topicsMinIsrProcessed);
                sb.append(", topicsMinIsrSkipped=").append(topicsMinIsrSkipped);
            }
            if (topicsRebalanceProcessed > 0) {
                sb.append(", topicsRebalanceProcessed=").append(topicsRebalanceProcessed);
                sb.append(", topicsRebalanceSkipped=").append(topicsRebalanceSkipped);
            }
            if (partitionsRebalanceProcessed > 0) {
                sb.append(", partitionsRebalanceProcessed=").append(partitionsRebalanceProcessed);
                sb.append(", partitionsRebalanceSkipped=").append(partitionsRebalanceSkipped);
            }
            sb.append(", errors=").append(errors);
            sb.append(", elapsedMinutes=").append(new Interval(startTime, System.currentTimeMillis()).toPeriod().getMinutes());
            sb.append('}');
            return sb.toString();
        }
    }
}