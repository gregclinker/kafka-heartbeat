package com.essexboy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;

public class App {

    final static Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                final HeatBeatCron heatbeatCron = new HeatBeatCron(new FileInputStream(args[0]));
                heatbeatCron.cron();
            } else {
                LOGGER.error("usage : java -jar kafka-heartbeat.jar <config file>");
            }
        } catch (Exception e) {
            LOGGER.error("error", e);
        }
    }
}
