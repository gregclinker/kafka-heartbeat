package com.essexboy;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.apache.kafka.common.config.ConfigResource.Type.TOPIC;

public class ProtoTest {

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.18.0.6:29092,172.18.0.9:29093,172.18.0.7:29094,172.18.0.8:29095,172.18.0.5:29096,172.18.0.10:29097")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":10,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void test2() throws Exception {
        final List<TopicInfo> topicInfos = getTopics();

        topicInfos.stream().forEach(t -> System.out.println(t.getName() + ", rf=" + t.getReplicationFactor() + ", minISR=" + t.getMinIsr()));

        topicInfos.stream().filter(t -> t.getReplicationFactor() == 4).collect(Collectors.toList()).forEach(s -> System.out.println(s.getName()));
    }

    private List<TopicInfo> getTopics() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final List<String> allTopics = adminClient.listTopics().listings().get().stream().map(TopicListing::name).collect(Collectors.toList());
        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(allTopics.stream().map(t -> new ConfigResource(TOPIC, t)).collect(Collectors.toSet())).all().get();
        final Map<String, TopicDescription> topicDescriptionMap = adminClient.describeTopics(allTopics).all().get();
        return topicDescriptionMap.values().stream().map(t -> new TopicInfo(t, configResourceConfigMap.get(new ConfigResource(TOPIC, t.name())))).collect(Collectors.toList());
    }

    private List<String> setMinIsr(List<String> topics, int minIsr) throws IOException, InterruptedException, ExecutionException {
        final AdminClient adminClient = getAdminClient();

        // filter out non confirmed topics
        final List<String> confirmedTopics = adminClient.listTopics().listings().get().stream().map(TopicListing::name).collect(Collectors.toList());
        topics = topics.stream().filter(t -> (confirmedTopics.contains(t))).collect(Collectors.toList());

        System.out.println("confirmed topics=" + topics);

        // filter out topics that already have correct minISR
        final Set<ConfigResource> resources = topics.stream().map(t -> new ConfigResource(TOPIC, t)).collect(Collectors.toSet());
        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(resources).all().get();
        final List<String> minISRAlreadySetTopics = configResourceConfigMap.entrySet().stream().filter(e -> (Integer.parseInt(e.getValue().get(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG).value()) == minIsr)).map(e -> e.getKey().name()).collect(Collectors.toList());
        System.out.println("topics with minISR already set=" + minISRAlreadySetTopics);

        topics = topics.stream().filter(t -> (!minISRAlreadySetTopics.contains(t))).collect(Collectors.toList());

        System.out.println("topics to process=" + topics);

        if (topics.size() > 0) {
            final Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>(1);
            for (String topic : topics) {
                configs.put(new ConfigResource(TOPIC, topic), singletonList(new AlterConfigOp(new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr + ""), AlterConfigOp.OpType.SET)));
            }
            adminClient.incrementalAlterConfigs(configs).all().get();
        } else {
            System.out.println("no topics found to switch");
        }

        adminClient.close();

        return topics;
    }

    private List<String> removeNonExistentTopics(List<String> topics) throws IOException, InterruptedException, ExecutionException {
        AdminClient adminClient = getAdminClient();
        final List<String> confirmedTopics = adminClient.listTopics().listings().get().stream().map(TopicListing::name).collect(Collectors.toList());
        return topics.stream().filter(t -> (confirmedTopics.contains(t))).collect(Collectors.toList());
    }

    private List<Integer> getReplicas(Integer leader, Integer partition, boolean extend) {
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

    private List<Integer> getAvailableBrokers() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final List<Integer> brokers = getAdminClient().describeCluster().nodes().get().stream().map(Node::id).sorted().collect(Collectors.toList());
        adminClient.close();
        return brokers;
    }

    private List<Integer> orderLeaderFirst(List<Integer> replicas, Integer leader) {
        final List<Integer> newReplicas = replicas.stream().filter(i -> !i.equals(leader)).collect(Collectors.toList());
        newReplicas.add(0, leader);
        return newReplicas;
    }

    private void partitionReassignment(String topicName, Integer partition, List<Integer> replicas) throws Exception {
        if (getReplicas(topicName, partition).equals(replicas)) {
            System.out.println("replicas already set to " + replicas + ", for topic " + topicName + ", partition " + partition);
            return;
        }
        final AdminClient adminClient = getAdminClient();
        Map<TopicPartition, Optional<NewPartitionReassignment>> topicPartitionOptionalHashMap = new HashMap<>();
        topicPartitionOptionalHashMap.put(new TopicPartition(topicName, partition), Optional.of(new NewPartitionReassignment(replicas)));
        adminClient.alterPartitionReassignments(topicPartitionOptionalHashMap).all().get();
        System.out.println("replicas set to " + replicas + ", for topic " + topicName + ", partition " + partition);
    }

    private void partitionReassignment(String topicName, List<Integer> replicas) throws Exception {
        for (Integer partition : getPartitions(topicName)) {
            partitionReassignment(topicName, partition, replicas);
        }
    }

    private List<Integer> getPartitions(String topicName) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(singletonList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().stream().map(TopicPartitionInfo::partition).sorted().collect(Collectors.toList());
    }

    private List<Integer> getReplicas(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(singletonList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().get(partition).replicas().stream().map(Node::id).sorted().collect(Collectors.toList());
    }

    private Integer getLeader(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final TopicDescription topicDescription = adminClient.describeTopics(singletonList(topicName)).topicNameValues().get(topicName).get();
        return topicDescription.partitions().get(partition).leader().id();
    }

    private void isPartitionReassignment(String topicName, Integer partition) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final Set<TopicPartition> topicPartitions = new HashSet<>();
        topicPartitions.add(new TopicPartition(topicName, partition));
        final Map<TopicPartition, PartitionReassignment> topicPartitionPartitionReassignmentMap = adminClient.listPartitionReassignments(topicPartitions).reassignments().get();
        System.out.println(topicPartitionPartitionReassignmentMap.keySet().size());
        for (TopicPartition topicPartition : topicPartitionPartitionReassignmentMap.keySet()) {
            System.out.println(topicPartition.toString());
        }
    }

    private AdminClient getAdminClient() throws IOException {
        final HeartBeatConfig heartBeatConfig = HeartBeatConfig.getConfig();
        final AdminClient client = AdminClient.create(heartBeatConfig.getKafkaProperties());
        return client;
    }

    private void setMinIsr(String topic, int minIsr) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final ConfigResource configResource = new ConfigResource(TOPIC, topic);
        final ConfigEntry configEntry = new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr + "");
        final AlterConfigOp alterConfigOp = new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET);
        final Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>(1);
        configs.put(configResource, singletonList(alterConfigOp));
        adminClient.incrementalAlterConfigs(configs).all().get();
        adminClient.close();
    }

    private int getMinIsr(String topic) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final Set<ConfigResource> resources = Collections.singleton(new ConfigResource(TOPIC, topic));
        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(resources).all().get();
        final Object[] keys = configResourceConfigMap.keySet().toArray();
        for (ConfigEntry configEntry : configResourceConfigMap.get(keys[0]).entries()) {
            if (configEntry.name().equals(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)) {
                return Integer.parseInt(configEntry.value());
            }
        }
        return 0;
    }
}
