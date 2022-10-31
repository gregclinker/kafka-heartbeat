package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@ToString
public class HeartBeatService {

    final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatService.class);

    private final HeartBeatConfig config;
    private final ProcessedStats processedStats = new ProcessedStats();
    private boolean sucessfulSwitch = true;

    public HeartBeatService(HeartBeatConfig config) {
        this.config = config;
    }

    public void switchDown() {
        sucessfulSwitch = true;
        processedStats.reset();
        LOGGER.info("started switching down minISR to {}", config.getReducedIsr());
        switchMinIsr(config.getReducedIsr());
        LOGGER.info("finished switching down, success={}, stats={}", sucessfulSwitch, processedStats);
    }

    public void switchBack() {
        sucessfulSwitch = true;
        processedStats.reset();
        LOGGER.info("started switching back minISR to {}", config.getStandardIsr());
        switchMinIsr(config.getStandardIsr());
        LOGGER.info("finished switching back, success={}, stats={}", sucessfulSwitch, processedStats);
    }

    private void switchMinIsr(int newIsr) {
        for (String topic : config.getTopics()) {
            processedStats.topicsMinIsrProcessed++;
            switchIsr(topic, newIsr);
        }
    }

    private void switchIsr(String topic, int newIsr) {
        try {
            final int partitionMinIsr = getTopicData(topic).getPartitionMinIsr();
            final int exisitingMinIsr = getMinIsr(topic);
            if (newIsr > partitionMinIsr) {
                processedStats.topicsMinIsrSkipped++;
                LOGGER.info("SKIPPING switchIsr, topic {}, only has {} ISRs, which is not enough for a minIsr of {}", topic, partitionMinIsr, newIsr);
            } else if (newIsr == exisitingMinIsr) {
                processedStats.topicsMinIsrSkipped++;
                LOGGER.info("SKIPPING switchIsr, topic {} already has minIsr of {}", topic, newIsr);
            } else {
                setMinIsr(topic, newIsr);
                Thread.sleep(config.getSwitchMinIsrDelay() * 1000L);
            }
        } catch (Exception e) {
            sucessfulSwitch = false;
            processedStats.errors++;
            LOGGER.error("ERROR in switchIsr to {}, for topic {}", newIsr, topic, e);
        }
    }

    public void setMinIsr(String topic, int minIsr) throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
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
        }
    }

    public int getMinIsr(String topic) throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            final Set<ConfigResource> resources = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topic));
            final KafkaFuture<Map<ConfigResource, Config>> all = adminClient.describeConfigs(resources).all();
            while (!all.isDone()) {
                Thread.sleep(100);
            }
            final Map<ConfigResource, Config> configResourceConfigMap = all.get();
            final Object[] keys = configResourceConfigMap.keySet().toArray();
            for (ConfigEntry configEntry : configResourceConfigMap.get(keys[0]).entries()) {
                if (configEntry.name().equals(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG)) {
                    //LOGGER.debug("getMinIsr returned {}, for topic {}", configEntry.value(), topic);
                    return Integer.parseInt(configEntry.value());
                }
            }
        }
        return 0;
    }

    public List<Integer> getAvailableBrokers() throws Exception {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            return adminClient.describeCluster().nodes().get().stream().map(Node::id).sorted().collect(Collectors.toList());
        }
    }

    public TopicInfo getTopicData(String topicName) {
        try (AdminClient adminClient = AdminClient.create(config.getKafkaProperties())) {
            final TopicDescription topicDescription = adminClient.describeTopics(List.of(topicName)).topicNameValues().get(topicName).get();
            return new TopicInfo(topicDescription);
        } catch (Exception e) {
            LOGGER.error("ERROR getTopicData, topic {}", topicName, e);
        }
        return null;
    }

    private static class ProcessedStats {
        protected int topicsMinIsrProcessed;
        protected int topicsMinIsrSkipped;
        protected int errors;
        private long startTime;

        public void reset() {
            topicsMinIsrProcessed = 0;
            topicsMinIsrSkipped = 0;
            errors = 0;
            startTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ProcessedStats{");
            if (topicsMinIsrProcessed > 0) {
                sb.append("topicsMinIsrProcessed=").append(topicsMinIsrProcessed);
                sb.append(", topicsMinIsrSkipped=").append(topicsMinIsrSkipped);
            }
            sb.append(", errors=").append(errors);
            sb.append(", elapsedMinutes=").append(new Interval(startTime, System.currentTimeMillis()).toPeriod().getMinutes());
            sb.append('}');
            return sb.toString();
        }
    }
}