package com.essexboy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

@Getter
public class HeatBeatCron extends TimerTask {

    final static Logger LOGGER = LoggerFactory.getLogger(HeatBeatCron.class);

    private Timer timer = new Timer();
    private Config config;
    private HeatBeatService heatBeatService;
    private int failCount = 0;
    private int passCount = 0;

    public HeatBeatCron(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        config = mapper.readValue(inputStream, Config.class);
        this.heatBeatService = new HeartBeatServiceImpl(config);
    }

    /**
     * start the cron
     */
    public void cron() {
        timer.scheduleAtFixedRate(this, 0, config.getInterval() * 1000);
    }

    /**
     * stop the cron
     */
    public void stop() {
        timer.cancel();
    }

    @Override
    public void run() {
        try {
            final boolean up = heatBeatService.isUp();
            LOGGER.debug("run, isUp {}", up);
            if (up) {
                passCount++;
                failCount = 0;
            } else {
                failCount++;
                passCount = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
