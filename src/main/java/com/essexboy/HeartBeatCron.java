package com.essexboy;

import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@ToString
public class HeartBeatCron extends TimerTask {

    final static Logger LOGGER = LoggerFactory.getLogger(HeartBeatCron.class);

    private final Timer timer = new Timer();
    private final HeartBeatConfig heartBeatConfig;
    private final HeartBeatService heartBeatService;
    private int failCount = 0;
    private int passCount;
    private boolean switchedDown = false;

    public HeartBeatCron() throws IOException {
        this.heartBeatConfig = HeartBeatConfig.getConfig();
        this.heartBeatService = new HeartBeatService(heartBeatConfig);
        passCount=heartBeatConfig.getCountToSwitch();
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
