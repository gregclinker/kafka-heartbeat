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
    private final HeartBeatConfig config;
    private final HeartBeatService heartBeatService;
    private int failCount = 0;
    private int passCount;
    private final boolean switchedDown = false;

    public HeartBeatCron() throws IOException {
        this.config = HeartBeatConfig.getConfig();
        this.heartBeatService = new HeartBeatService(config);
        passCount = config.getCountToSwitch();
    }

    /**
     * start the cron
     */
    public void cron() {
        LOGGER.info("starting cron with {}", config);
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
            final List<Integer> availableBrokers = heartBeatService.getAvailableBrokers();
            final boolean up = availableBrokers.size() >= config.getNumberOfBrokers();
            final String health = up ? "good" : "sick";
            LOGGER.info("heartbeat {}, available brokers {}", health, availableBrokers);
            if (up) {
                passCount++;
                failCount = 0;
            } else {
                failCount++;
                passCount = 0;
            }
            if (failCount == config.getCountToSwitch() || (failCount > 0 && failCount % config.getCountToSwitch() == 0 && !heartBeatService.isSucessfulSwitch())) {
                heartBeatService.switchDown();
            } else if (passCount == config.getCountToSwitch() || (passCount > 0 && passCount % config.getCountToSwitch() == 0 && !heartBeatService.isSucessfulSwitch())) {
                heartBeatService.switchBack();
            }
        } catch (Exception e) {
            LOGGER.error("cron error, stopping heartbeat", e);
            stop();
        }
    }
}
