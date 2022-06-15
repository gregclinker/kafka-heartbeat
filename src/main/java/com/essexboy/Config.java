package com.essexboy;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@ToString
public class Config {
    private int numberOfBrokers;
    private int interval;
    private int standardIsr;
    private int reducedIsr;
    private int failuresToSwitch;
    private Map<String, String> kafkaProperties;
    private List<String> topics;

    public Properties getKafkaProperties() {
        Properties properties = new Properties();
        properties.putAll(kafkaProperties);
        return properties;
    }
}
