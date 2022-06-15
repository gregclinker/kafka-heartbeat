package com.essexboy;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProtoTest {

    @Test
    public void test1() throws Exception {
        final AdminClient adminClient = AdminClient.create(getProperties());

        Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, "greg-test1"));

        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(resources).all().get();
        System.out.println(configResourceConfigMap);

        //new ConfigEntry(ConfigResource.)

        adminClient.close();
    }

    @Test
    public void ssl() throws Exception {
        final AdminClient adminClient = AdminClient.create(getProperties());
        assertNotNull(adminClient);
        Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, "greg-test1"));
        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(resources).all().get();
        assertNotNull(configResourceConfigMap);
        adminClient.close();
    }

    private Properties getProperties() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "172.31.0.5:29092,172.31.0.6:29093,172.31.0.7:29094");
        properties.put("connections.max.idle.ms", 10000);
        properties.put("request.timeout.ms", 5000);
        properties.put("security.protocol", "SSL");
        properties.put("ssl.truststore.location", "/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks");
        properties.put("ssl.truststore.password", "confluent");
        properties.put("ssl.keystore.location", "/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks");
        properties.put("ssl.keystore.password", "confluent");
        properties.put("ssl.key.password", "confluent");
        properties.put("ssl.endpoint.identification.algorithm", " ");
        return properties;
    }
}
