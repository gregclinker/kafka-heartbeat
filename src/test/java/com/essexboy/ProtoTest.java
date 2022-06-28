package com.essexboy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.*;

public class ProtoTest {

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @ClearEnvironmentVariable(key = "HEART_BEAT_CONFIG")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":10,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void test2() throws JsonProcessingException {
        final HeartBeatConfig heartBeatConfig = new ObjectMapper().readValue(System.getenv("HEART_BEAT_CONFIG"), HeartBeatConfig.class);
        System.out.println(heartBeatConfig);
    }

    private AdminClient getAdminClient() throws IOException {
        final HeartBeatConfig heartBeatConfig = HeartBeatConfig.getConfig();
        final AdminClient client = AdminClient.create(heartBeatConfig.getKafkaProperties());
        return client;
    }

    private void setMinIsr(String topic, int minIsr) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        final ConfigEntry configEntry = new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr + "");
        final AlterConfigOp alterConfigOp = new AlterConfigOp(configEntry, AlterConfigOp.OpType.SET);
        final Map<ConfigResource, Collection<AlterConfigOp>> configs = new HashMap<>(1);
        configs.put(configResource, Arrays.asList(alterConfigOp));
        adminClient.incrementalAlterConfigs(configs).all().get();
        adminClient.close();
    }

    private int getMinIsr(String topic) throws Exception {
        final AdminClient adminClient = getAdminClient();
        final Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic));
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
