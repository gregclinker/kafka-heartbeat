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

    private final HeartBeatConfig heartBeatConfig;
    private boolean sucessfulSwitch = true;


    public HeartBeatService(HeartBeatConfig heartBeatConfig) {
        this.heartBeatConfig = heartBeatConfig;
    }

    private AdminClient getAdminClient() {
        final Properties kafkaProperties = heartBeatConfig.getKafkaProperties();
        if (kafkaProperties == null) {
            LOGGER.error("kafkaProperties are null");
        }
        return AdminClient.create(kafkaProperties);
    }

    public void switchDown() {
        LOGGER.info("swicthIsrDown");
        if (heartBeatConfig.isFastMinIsr()) {
            switchDown(1);
        } else {
            switchDown(heartBeatConfig.getReducedIsr());
        }
        rebalanceDown();
    }

    public void switchBack() {
        LOGGER.info("switchIsrBack");
        rebalanceUp();
        switchIsr(heartBeatConfig.getStandardIsr());
    }

    public void rebalanceDown() {
        sucessfulSwitch = true;
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("rebalancing down, topic {} down to available replicas", topic);
            try {
                for (Integer partition : getPartitions(topic)) {
                    final int isrCount = getIsr(topic, partition).size();
                    if (isrCount >= heartBeatConfig.getReducedIsr()) {
                        LOGGER.info("SKIPPING rebalancing down, topic {}, partition {} already has {} ISRs", topic, partition, isrCount);
                    } else {
                        final Integer leader = getLeader(topic, partition);
                        if (leader != null) {
                            try {
                                final List<Integer> replicas = getRebalanceReplicas(leader, partition, false);
                                partitionReassignment(topic, partition, replicas);
                                Thread.sleep(heartBeatConfig.getRebalanceDownDelay() * 1000);
                            } catch (Exception e) {
                                sucessfulSwitch = false;
                                LOGGER.error("ERROR rebalancing down topic {}, partition {}", topic, partition, e);
                            }
                        }
                    }
                }
                if (sucessfulSwitch && heartBeatConfig.isFastMinIsr()) {
                    switchIsr(heartBeatConfig.getReducedIsr());
                }
            } catch (Exception e) {
                sucessfulSwitch = false;
                LOGGER.error("ERROR rebalancing down topic {}", topic, e);
            }
        }
    }

    public void rebalanceUp() {
        sucessfulSwitch = true;
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("rebalancing up, topic {} to available replicas", topic);
            try {
                for (Integer partition : getPartitions(topic)) {
                    final Integer leader = getLeader(topic, partition);
                    if (leader != null) {
                        final int isrCount = getIsr(topic, partition).size();
                        if (isrCount >= heartBeatConfig.getReplicationFactor()) {
                            LOGGER.info("SKIPPING rebalancing up, topic {}, partition {} already has {} ISRs", topic, partition, isrCount);
                        } else {
                            try {
                                final List<Integer> replicas = getRebalanceReplicas(leader, partition, true);
                                partitionReassignment(topic, partition, replicas);
                                Thread.sleep(heartBeatConfig.getRebalanceUpDelay() * 1000);
                            } catch (Exception e) {
                                sucessfulSwitch = false;
                                LOGGER.error("ERROR rebalancing up topic {}, partition {}", topic, partition, e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                sucessfulSwitch = false;
                LOGGER.error("ERROR rebalancing up topic {}", topic, e);
            }
        }
    }

    private void switchDown(int newIsr) {
        sucessfulSwitch = true;
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("switchIsr, topic {} to {}", topic, newIsr);
            try {
                for (Integer partition : getPartitions(topic)) {
                    final int isrCount = getIsr(topic, partition).size();
                    if (newIsr < isrCount) {
                        LOGGER.info("switching minISR down to {}, for topic {}", newIsr, topic);
                        switchIsr(topic, newIsr);
                        break;
                    }
                }
            } catch (Exception e) {
                sucessfulSwitch = false;
                LOGGER.error("ERROR in switchIsr to {}, for topic {}", newIsr, topic, e);
            }
        }
    }

    private void switchIsr(int newIsr) {
        sucessfulSwitch = true;
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("switchIsr, topic {} to {}", topic, newIsr);
            try {
                switchIsr(topic, newIsr);
            } catch (Exception e) {
                sucessfulSwitch = false;
                LOGGER.error("ERROR in switchIsr to {}, for topic {}", newIsr, topic, e);
            }
        }
    }

    private void switchIsr(String topic, int newIsr) throws Exception {
        if (getMinIsr(topic) != newIsr) {
            setMinIsr(topic, newIsr);
        } else {
            LOGGER.info("SKIPPING topic {} already has minIsr of {}", topic, newIsr);
        }
    }

    public void setMinIsr(String topic, int minIsr) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
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
        } finally {
            adminClient.close();
        }
    }

    public int getMinIsr(String topic) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
            final Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic));
            final KafkaFuture<Map<ConfigResource, Config>> all = adminClient.describeConfigs(resources).all();
            while (!all.isDone()) {
                Thread.sleep(100);
            }
            final Map<ConfigResource, Config> configResourceConfigMap = all.get();
            final Object[] keys = configResourceConfigMap.keySet().toArray();
            for (ConfigEntry configEntry : configResourceConfigMap.get(keys[0]).entries()) {
                if (configEntry.name().equals(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)) {
                    LOGGER.debug("getMinIsr returned {}, for topic {}", configEntry.value(), topic);
                    return Integer.parseInt(configEntry.value());
                }
            }
        } finally {
            adminClient.close();
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
        final List<Integer> brokers = getAdminClient().describeCluster().nodes().get().stream().map(n -> n.id()).sorted().collect(Collectors.toList());
        adminClient.close();
        return brokers;
    }

    private void partitionReassignment(String topicName, Integer partition, List<Integer> replicas) throws
            Exception {
        if (CollectionUtils.containsAll(getReplicas(topicName, partition), replicas)) {
            LOGGER.info("replicas already set to {} for topic {}, partition {}", replicas, topicName, partition);
            return;
        }
        final AdminClient adminClient = getAdminClient();
        try {
            Map<TopicPartition, Optional<NewPartitionReassignment>> topicPartitionOptionalHashMap = new HashMap<>();
            topicPartitionOptionalHashMap.put(new TopicPartition(topicName, partition), Optional.of(new NewPartitionReassignment(replicas)));
            adminClient.alterPartitionReassignments(topicPartitionOptionalHashMap).all().get();
            LOGGER.info("replicas set to {} for topic {}, partition {}", replicas, topicName, partition);
        } finally {
            adminClient.close();
        }

    }

    private List<Integer> getPartitions(String topicName) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
            final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
            return topicDescription.partitions().stream().map(p -> p.partition()).sorted().collect(Collectors.toList());
        } finally {
            adminClient.close();
        }
    }

    private List<Integer> getReplicas(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
            final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
            return topicDescription.partitions().get(partition).replicas().stream().map(r -> r.id()).sorted().collect(Collectors.toList());
        } finally {
            adminClient.close();
        }
    }

    private List<Integer> getIsr(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
            final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
            return topicDescription.partitions().get(partition).isr().stream().map(r -> r.id()).sorted().collect(Collectors.toList());
        } finally {
            adminClient.close();
        }
    }

    private Integer getLeader(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        try {
            final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
            final Node leader = topicDescription.partitions().get(partition).leader();
            if (leader == null) {
                LOGGER.error("no leader found for {}, partition {}", topicName, partition);
                return null;
            }
            return leader.id();
        } finally {
            adminClient.close();
        }
    }

    private List<Integer> orderLeaderFirst(List<Integer> replicas, Integer leader) {
        final List<Integer> newReplicas = replicas.stream().filter(i -> !i.equals(leader)).collect(Collectors.toList());
        newReplicas.add(0, leader);
        return newReplicas;
    }
}