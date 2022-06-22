package com.essexboy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;

public class App {

    final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            final HeatBeatCron heatbeatCron = new HeatBeatCron(new FileInputStream("heartbeat-config.yaml"));
            heatbeatCron.cron();
        } catch (Exception e) {
            LOGGER.error("error", e);
            System.exit(-1);
        }
    }
}
