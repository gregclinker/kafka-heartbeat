package com.essexboy;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

@Getter
public class HeatBeatCron extends TimerTask {

    final static Logger LOGGER = LoggerFactory.getLogger(HeatBeatCron.class);

    private Timer timer = new Timer();
    private Config config;
    private HeatBeatService heatBeatService;
    private int count = 0;

    public HeatBeatCron(Config config) {
        this.config = config;
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
        count++;
        try {
            LOGGER.debug("run, isUp {}", heatBeatService.isUp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
