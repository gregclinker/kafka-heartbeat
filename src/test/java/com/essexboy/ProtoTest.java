package com.essexboy;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtoTest {

    @BeforeAll
    private static void init() {
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094");
        System.setProperty("KAFKA_SECURITY_PROTOCOL", "SSL");
        System.setProperty("KAFKA_SSL_TRUSTSTORE_LOCATION", "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks");
        System.setProperty("KAFKA_SSL_TRUSTSTORE_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_KEYSTORE_LOCATION", "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks");
        System.setProperty("KAFKA_SSL_KEYSTORE_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_KEY_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", " ");
        System.setProperty("KAFKA_SSL_TEST_SYSTEM_PROPERTY", "test-from-system-property");
    }

    //@Test
    public void test1() throws Exception {
        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig-no-kafka.yaml"));
        final HeartBeatService heartBeatService = new HeartBeatService(heartBeatConfig);

        //heartBeatService.setMinIsr("greg-test1", 2);
        //assertEquals(2, heartBeatService.getMinIsr("greg-test1"));
        //heartBeatService.setMinIsr("greg-test1", 1);
        //assertEquals(1, heartBeatService.getMinIsr("greg-test1"));
        //heartBeatService.setMinIsr("greg-test1", 2);
        //assertEquals(2, heartBeatService.getMinIsr("greg-test1"));
    }

    private AdminClient getAdminClient() throws IOException {
        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig-no-kafka.yaml"));
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
