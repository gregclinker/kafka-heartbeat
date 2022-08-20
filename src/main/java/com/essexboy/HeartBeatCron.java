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
        passCount = heartBeatConfig.getCountToSwitch();
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
            final List<Integer> availableBrokers = heartBeatService.getAvailableBrokers();
            final boolean up = availableBrokers.size() >= heartBeatConfig.getNumberOfBrokers();
            LOGGER.debug("run, isUp {}, available brokers {}", up, availableBrokers);
            if (up) {
                passCount++;
                failCount = 0;
            } else {
                failCount++;
                passCount = 0;
            }
            if (failCount == heartBeatConfig.getCountToSwitch()) {
                heartBeatService.switchIsrDown();
                heartBeatService.rebalance();
            } else if (passCount == heartBeatConfig.getCountToSwitch()) {
                heartBeatService.switchIsrBack();
                heartBeatService.rebalance();
            }
        } catch (Exception e) {
            LOGGER.error("cron error, stopping heartbeat", e);
            stop();
        }
    }
}
