package com.essexboy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class HeartBeatConfig {
    private int numberOfBrokers;
    private int interval;
    private int standardIsr;
    private int reducedIsr;
    private int failuresToSwitch;
    private Properties kafkaProperties;
    private List<String> topics;

    public HeartBeatConfig(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        final HeartBeatConfig heartBeatConfig = mapper.readValue(inputStream, HeartBeatConfig.class);
        this.numberOfBrokers = heartBeatConfig.getNumberOfBrokers();
        this.interval = heartBeatConfig.getInterval();
        this.standardIsr = heartBeatConfig.getStandardIsr();
        this.reducedIsr = heartBeatConfig.getReducedIsr();
        this.failuresToSwitch = heartBeatConfig.getFailuresToSwitch();
        this.kafkaProperties = heartBeatConfig.getKafkaProperties();
        this.topics = heartBeatConfig.getTopics();

        for (Object key : System.getProperties().keySet()) {
            String property = key.toString();
            if (property.startsWith("KAFKA_")) {
                if (kafkaProperties == null) {
                    kafkaProperties = new Properties();
                }
                kafkaProperties.put(property.replaceAll("KAFKA_", "").replaceAll("_", ".").toLowerCase(), System.getProperty(key.toString()));
            }
        }
    }
}
