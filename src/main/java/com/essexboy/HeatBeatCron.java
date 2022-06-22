package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@ToString
public class HeatBeatCron extends TimerTask {

    final static Logger LOGGER = LoggerFactory.getLogger(HeatBeatCron.class);

    private final Timer timer = new Timer();
    private final HeartBeatConfig heartBeatConfig;
    private final HeartBeatService heartBeatService;
    private int failCount = 0;
    private int passCount = 0;
    private boolean switchedDown = false;

    public HeatBeatCron(InputStream inputStream) throws IOException {
        this.heartBeatConfig = new HeartBeatConfig(inputStream);
        this.heartBeatService = new HeartBeatService(heartBeatConfig);
    }

    /**
     * start the cron
     */
    public void cron() {
        LOGGER.info("starting cron with {}", heartBeatConfig);
        timer.scheduleAtFixedRate(this, 0, heartBeatConfig.getInterval() * 1000);
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
            final boolean up = heartBeatService.isUp();
            LOGGER.debug("run, isUp {}", up);
            if (up) {
                passCount++;
                failCount = 0;
            } else {
                failCount++;
                passCount = 0;
            }
            if (failCount == heartBeatConfig.getCountToSwitch()) {
                heartBeatService.switchIsrDown();
            } else if (passCount == heartBeatConfig.getCountToSwitch()) {
                heartBeatService.switchIsrBack();
            }
        } catch (Exception e) {
            LOGGER.error("cron error, stopping heartbeat", e);
            stop();
        }
    }
}
