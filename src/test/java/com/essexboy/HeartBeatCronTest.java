package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

class HeartBeatCronTest {

    private HeartBeatCron heartBeatCron;

    @BeforeEach
    private void init() throws Exception {
        heartBeatCron = new HeartBeatCron();
        heartBeatCron.getHeartBeatService().setMinIsr("greg-test1", 2);
    }

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
    public void config() {
        assertNotNull(heartBeatCron.getConfig());
    }

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":1,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void isDown() throws Exception {
        assertEquals(2, heartBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heartBeatCron.getConfig().setNumberOfBrokers(4);
        heartBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heartBeatCron.getFailCount() > 0);
        assertEquals(0, heartBeatCron.getPassCount());
        Thread.sleep(10000);
        assertEquals(1, heartBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heartBeatCron.stop();
        assertEquals(1, heartBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
    }

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":1,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void isUp() throws Exception {
        heartBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heartBeatCron.getPassCount() > 0);
        assertEquals(0, heartBeatCron.getFailCount());
        heartBeatCron.stop();
    }

    @Test
    @SetEnvironmentVariable(key = "KAFKA_BOOTSTRAP_SERVERS", value = "172.31.0.6:29092,172.31.0.5:29093,172.31.0.7:29094")
    @SetEnvironmentVariable(key = "KAFKA_SECURITY_PROTOCOL", value = "SSL")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_TRUSTSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_LOCATION", value = "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEYSTORE_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_KEY_PASSWORD", value = "confluent")
    @SetEnvironmentVariable(key = "KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", value = " ")
    @SetEnvironmentVariable(key = "HEART_BEAT_CONFIG", value = "{\"numberOfBrokers\":3,\"interval\":1,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}")
    public void isUpThenDown() throws Exception {
        assertEquals(2, heartBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heartBeatCron.cron();
        Thread.sleep(2000);
        heartBeatCron.getConfig().setNumberOfBrokers(4);
        Thread.sleep(3000);
        assertEquals(0, heartBeatCron.getPassCount());
        assertEquals(3, heartBeatCron.getFailCount());
        assertEquals(1, heartBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heartBeatCron.stop();
    }
}