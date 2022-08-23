package com.essexboy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class HeartBeatConfig {

    final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatConfig.class);

    private int numberOfBrokers;
    private int interval;
    private int standardIsr;
    private int replicationFactor = 4;
    private int reducedIsr;
    private int countToSwitch;
    private Properties kafkaProperties;
    private List<String> topics;
    private boolean rebalanceUp = false;
    private int rebalanceUpDelay = 1;
    private boolean rebalanceDown = false;
    private int rebalanceDownDelay = 1;
    private boolean fastMinIsr = false;

    public static HeartBeatConfig getConfig() throws JsonProcessingException {
        if (System.getenv("HEART_BEAT_CONFIG") == null) {
            throw new RuntimeException("ERROR HEART_BEAT_CONFIG env is not set");
        }
        final HeartBeatConfig heartBeatConfig = new ObjectMapper().readValue(System.getenv("HEART_BEAT_CONFIG"), HeartBeatConfig.class);
        Properties properties = new Properties();
        System.getenv().keySet().stream().filter(key -> key.toString().startsWith("KAFKA_")).forEach(key -> {
            String kafkaProperty = key.replace("KAFKA_", "").replace("_", ".").toLowerCase();
            properties.put(kafkaProperty, System.getenv(key));
        });
        heartBeatConfig.setKafkaProperties(properties);
        if (System.getenv("REBALANCE_DOWN") != null) {
            heartBeatConfig.setRebalanceDown(Boolean.parseBoolean(System.getenv("REBALANCE_DOWN").toLowerCase()));
        }
        if (System.getenv("REBALANCE_UP") != null) {
            heartBeatConfig.setRebalanceUp(Boolean.parseBoolean(System.getenv("REBALANCE_UP").toLowerCase()));
        }
        if (System.getenv("FAST_MIN_ISR") != null) {
            heartBeatConfig.setFastMinIsr(Boolean.parseBoolean(System.getenv("FAST_MIN_ISR").toLowerCase()));
        }
        LOGGER.debug("created config {}", heartBeatConfig);
        return heartBeatConfig;
    }
}

