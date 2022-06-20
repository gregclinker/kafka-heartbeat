package com.essexboy;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class ProtoTest {

    @Test
    public void test1() throws Exception {
        System.out.println("minIsr=" + getMinIsr("greg-test1"));
        setMinIsr("greg-test1", 1);
        System.out.println("minIsr=" + getMinIsr("greg-test1"));
    }

    private AdminClient getAdminClient() throws IOException {
        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig.yaml"));
        final AdminClient client = AdminClient.create(heartBeatConfig.getKafkaProperties());
        return client;
    }

    private void setMinIsr(String topic, int minIsr) throws Exception {
        final AdminClient adminClient = getAdminClient();
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        Collection<ConfigEntry> entries = new ArrayList<>();
        entries.add(new ConfigEntry(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minIsr + ""));
        Config topicConfig = new Config(entries);
        Map<ConfigResource, Config> configs = new HashMap<>();
        configs.put(configResource, topicConfig);
        adminClient.alterConfigs(configs);
        adminClient.close();
    }

    private int getMinIsr(String topic) throws Exception {
        final AdminClient adminClient = getAdminClient();
        Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic));
        final Map<ConfigResource, Config> configResourceConfigMap = adminClient.describeConfigs(resources).all().get();
        final Object[] keys = configResourceConfigMap.keySet().toArray();
        final String minsIsr = configResourceConfigMap.get(keys[0]).entries().stream().filter(c ->
                c.name().equals(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)).findFirst().get().value();
        adminClient.close();
        return Integer.parseInt(minsIsr);
    }
}
