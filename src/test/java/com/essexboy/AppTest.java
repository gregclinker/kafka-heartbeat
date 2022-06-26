package com.essexboy;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest {

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
    public void test1() throws Exception {
        AdminClient client = AdminClient.create(HeartBeatConfig.getConfig().getKafkaProperties());
        assertNotNull(client);
    }
}
