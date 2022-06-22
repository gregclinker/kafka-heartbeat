package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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

    public boolean isUp() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final int brokerCount = adminClient.describeCluster().nodes().get().size();
        adminClient.close();
        return heartBeatConfig.getNumberOfBrokers() == brokerCount;
    }

    public boolean isRebalancing() {
        //TO DO
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
}