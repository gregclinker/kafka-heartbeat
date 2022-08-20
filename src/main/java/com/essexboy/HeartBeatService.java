package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
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

    public List<Integer> getAvailableBrokers() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final List<Integer> brokers = getAdminClient().describeCluster().nodes().get().stream().map(n -> n.id()).sorted().collect(Collectors.toList());
        adminClient.close();
        return brokers;
    }

    public boolean rebalance() {
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("rebalancing, topic {} to available replicas", topic);
            try {
                partitionReassignment(topic, getAvailableBrokers());
            } catch (Exception e) {
                LOGGER.error("error rebalancing topic {}", topic, e);
            }
        }
        return false;
    }

    public void switchIsrDown() {
        LOGGER.info("swicthIsrDown");
        switchIsr(heartBeatConfig.getReducedIsr());
    }

    public void switchIsrBack() {
        LOGGER.info("switchIsrBack");
        switchIsr(heartBeatConfig.getStandardIsr());
    }

    private void switchIsr(int newIsr) {
        for (String topic : heartBeatConfig.getTopics()) {
            LOGGER.info("switchIsr, topic {} to {}", topic, newIsr);
            try {
                switchIsr(topic, newIsr);
            } catch (Exception e) {
                LOGGER.error("error in switchIsr to {}, for topic {}", newIsr, topic, e);
            }
        }
    }

    private void switchIsr(String topic, int newIsr) throws Exception {
        LOGGER.info("switching topic {} to {}", topic, newIsr);
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
                    LOGGER.info("getMinIsr returned {}, for topic {}", configEntry.value(), topic);
                    return Integer.parseInt(configEntry.value());
                }
            }
        } finally {
            adminClient.close();
        }
        return 0;
    }

    private void partitionReassignment(String topicName, Integer partition, List<Integer> replicas) throws Exception {
        if (getReplicas(topicName, partition).equals(replicas)) {
            LOGGER.info("replicas already set to {} for topic {}, partition {}", replicas, topicName, partition);
            return;
        }
        final AdminClient adminClient = getAdminClient();
        Map<TopicPartition, Optional<NewPartitionReassignment>> topicPartitionOptionalHashMap = new HashMap<>();
        topicPartitionOptionalHashMap.put(new TopicPartition(topicName, partition), Optional.of(new NewPartitionReassignment(replicas)));
        adminClient.alterPartitionReassignments(topicPartitionOptionalHashMap).all().get();
        LOGGER.info("replicas set to {} for topic {}, partition {}", replicas, topicName, partition);
    }

    private void partitionReassignment(String topicName, List<Integer> replicas) throws Exception {
        for (Integer partition : getPartitions(topicName)) {
            partitionReassignment(topicName, partition, replicas);
        }
    }

    private List<Integer> getPartitions(String topicName) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().stream().map(p -> p.partition()).sorted().collect(Collectors.toList());
    }

    private List<Integer> getReplicas(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().get(partition).replicas().stream().map(r -> r.id()).sorted().collect(Collectors.toList());
    }

    private Integer getLeader(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(Arrays.asList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().get(partition).leader().id();
    }


}