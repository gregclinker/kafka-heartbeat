package com.essexboy;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeartBeatConfigTest {

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":10,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void formProperties() throws IOException {
        final HeartBeatConfig config = HeartBeatConfig.getConfig();
        assertNotNull(config);
        assertEquals(3, config.getNumberOfBrokers());
        assertEquals(10, config.getInterval());
        assertEquals(2, config.getStandardIsr());
        assertEquals(1, config.getReducedIsr());
        assertEquals(2, config.getTopics().size());
        assertEquals("greg-test1", config.getTopics().get(0));
        assertEquals("greg-test2", config.getTopics().get(1));
        assertEquals("172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094", config.getKafkaProperties().getProperty("bootstrap.servers"));
        assertEquals("SSL", config.getKafkaProperties().getProperty("security.protocol"));
        assertEquals("confluent", config.getKafkaProperties().getProperty("ssl.truststore.password"));
        assertEquals("confluent", config.getKafkaProperties().getProperty("ssl.keystore.password"));
        assertEquals("confluent", config.getKafkaProperties().getProperty("ssl.key.password"));
        assertEquals(" ", config.getKafkaProperties().getProperty("ssl.endpoint.identification.algorithm"));
        assertEquals("/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks", config.getKafkaProperties().getProperty("ssl.keystore.location"));
        assertEquals("/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks", config.getKafkaProperties().getProperty("ssl.truststore.location"));
    }
}