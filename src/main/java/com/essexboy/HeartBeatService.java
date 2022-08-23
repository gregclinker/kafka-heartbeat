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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public class HeartBeatService {

    final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatService.class);

    private final HeartBeatConfig config;
    private boolean sucessfulSwitch = true;

    public HeartBeatService(HeartBeatConfig config) {
        this.config = config;
    }

    private AdminClient getAdminClient() {
        final Properties kafkaProperties = config.getKafkaProperties();
        if (kafkaProperties == null) {
            LOGGER.error("kafkaProperties are null");
        }
        return AdminClient.create(kafkaProperties);
    }

    public void switchDown() {
        sucessfulSwitch = true;
        LOGGER.info("switchIsrDown");
        LOGGER.info("switching minISR to 1");
        switchIsr(1);
        LOGGER.info("rebalancing down to min {} replicas", config.getReducedIsr());
        rebalance(config.getReducedIsr(), false, config.getRebalanceDownDelay(), config.getReducedIsr());
        LOGGER.info("switchIsrDown finished, success={}", sucessfulSwitch);
    }

    public void switchBack() {
        sucessfulSwitch = true;
        LOGGER.info("switchIsrBack");
        LOGGER.info("rebalancing up to min {} replicas", config.getReplicationFactor());
        rebalance(config.getReplicationFactor(), true, config.getRebalanceUpDelay(), config.getStandardIsr());
        LOGGER.info("switchIsrBack finished, success={}", sucessfulSwitch);
    }

    public void rebalance(int requiredReplicas, boolean extend, int delay, int newMinIsr) {
        for (String topic : config.getTopics()) {
            if (rebalance(topic, requiredReplicas, extend, delay)) {
                switchIsr(topic, newMinIsr);
            }
        }
    }

    private boolean rebalance(String topic, int requiredReplicas, boolean extend, int delay) {
        final TopicData topicData = getTopicData(topic);
        boolean successfulRebalance = true;
        for (PartitionData partition : topicData.getPartitions()) {
            final int isrCount = partition.getIsrs().size();
            if (isrCount >= requiredReplicas) {
                LOGGER.info("SKIPPING rebalancing topic {}, partition {} already has {} ISRs", topic, partition.getId(), isrCount);
            } else if (partition.getLeader() == null) {
                LOGGER.info("SKIPPING rebalancing topic {}, partition {}, no leader set", topic, partition.getId(), isrCount);
            } else {
                try {
                    final List<Integer> replicas = getRebalanceReplicas(partition.getLeader(), partition.getId(), extend);
                    partitionReassignment(topic, partition.getId(), replicas);
                    Thread.sleep(delay * 1000);
                } catch (Exception e) {
                    sucessfulSwitch = false;
                    successfulRebalance = false;
                    LOGGER.error("ERROR rebalancing topic {}, partition {}", topic, partition.getId(), e);
                }
            }
        }
        return successfulRebalance;
    }

    private void switchIsr(int newIsr) {
        for (String topic : config.getTopics()) {
            switchIsr(topic, newIsr);
        }
    }

    private void switchIsr(String topic, int newIsr) {
        LOGGER.info("switchIsr, topic {} to {}", topic, newIsr);
        try {
            if (getMinIsr(topic) != newIsr) {
                setMinIsr(topic, newIsr);
            } else {
                LOGGER.info("SKIPPING topic {} already has minIsr of {}", topic, newIsr);
            }
        } catch (Exception e) {
            sucessfulSwitch = false;
            LOGGER.error("ERROR in switchIsr to {}, for topic {}", newIsr, topic, e);
        }
    }

    public void setMinIsr(String topic, int minIsr) throws Exception {
        try (AdminClient adminClient = getAdminClient()) {
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
        try (AdminClient adminClient = getAdminClient()) {
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
        final AdminClient adminClient = getAdminClient();
        final List<Integer> brokers = getAdminClient().describeCluster().nodes().get().stream().map(Node::id).sorted().collect(Collectors.toList());
        adminClient.close();
        return brokers;
    }

    private void partitionReassignment(String topicName, Integer partition, List<Integer> replicas) throws Exception {
        try (AdminClient adminClient = getAdminClient()) {
            Map<TopicPartition, Optional<NewPartitionReassignment>> topicPartitionOptionalHashMap = new HashMap<>();
            topicPartitionOptionalHashMap.put(new TopicPartition(topicName, partition), Optional.of(new NewPartitionReassignment(replicas)));
            adminClient.alterPartitionReassignments(topicPartitionOptionalHashMap).all().get();
            LOGGER.info("replicas set to {} for topic {}, partition {}", replicas, topicName, partition);
        }
    }

    public TopicData getTopicData(String topicName) {
        TopicData topicData = new TopicData();
        topicData.setName(topicName);
        try (AdminClient adminClient = getAdminClient()) {
            final TopicDescription topicDescription = adminClient.describeTopics(List.of(topicName)).topicNameValues().get(topicName).get();
            topicData.setPartitions(topicDescription.partitions().stream().map(PartitionData::new).collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.error("ERROR getTopicData, topic {}", topicName, e);
        }
        return topicData;
    }

    private List<Integer> orderLeaderFirst(List<Integer> replicas, Integer leader) {
        final List<Integer> newReplicas = replicas.stream().filter(i -> !i.equals(leader)).collect(Collectors.toList());
        newReplicas.add(0, leader);
        return newReplicas;
    }
}