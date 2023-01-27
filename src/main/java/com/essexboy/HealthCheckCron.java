package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@ToString
public class HealthCheckCron extends TimerTask {

    final static Logger LOGGER = LoggerFactory.getLogger(HealthCheckCron.class);

    private final Timer timer = new Timer();
    private final HeartBeatConfig config;
    private final HeartBeatService heartBeatService;

    public HealthCheckCron() throws IOException {
        this.config = HeartBeatConfig.getConfig();
        this.heartBeatService = new HeartBeatService(config);
    }

    /**
     * start the cron
     */
    public void cron() {
        LOGGER.info("starting cron with {}", config);
        timer.scheduleAtFixedRate(this, 0, 60000);
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
        } catch (Exception e) {
            LOGGER.error("cron error, stopping heartbeat", e);
            stop();
        }
    }
}
