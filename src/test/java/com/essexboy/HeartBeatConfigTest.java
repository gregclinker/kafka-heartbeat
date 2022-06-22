package com.essexboy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeartBeatConfigTest {

    @BeforeAll
    private static void init() {
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "172.31.0.5:29092,172.31.0.6:29093,172.31.0.7:29094");
        System.setProperty("KAFKA_SECURITY_PROTOCOL", "SSL");
        System.setProperty("KAFKA_SSL_TRUSTSTORE_LOCATION", "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks");
        System.setProperty("KAFKA_SSL_TRUSTSTORE_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_KEYSTORE_LOCATION", "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks");
        System.setProperty("KAFKA_SSL_KEYSTORE_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_KEY_PASSWORD", "confluent");
        System.setProperty("KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", " ");
        System.setProperty("KAFKA_SSL_TEST_SYSTEM_PROPERTY", "test-from-system-property");
    }

    @Test
    public void formProperties() throws IOException {
        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig-no-kafka.yaml"));
        assertNotNull(heartBeatConfig);
        assertNotNull(heartBeatConfig.getKafkaProperties());
        assertEquals("test-from-system-property", heartBeatConfig.getKafkaProperties().get("ssl.test.system.property"));
    }

    @Test
    public void formYaml() throws IOException {
        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig.yaml"));
        assertNotNull(heartBeatConfig);
        assertNotNull(heartBeatConfig.getKafkaProperties());
        assertEquals("ssl-test-yaml-property", heartBeatConfig.getKafkaProperties().get("ssl.test.yaml.property"));
    }
}