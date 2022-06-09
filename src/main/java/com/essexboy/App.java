package com.essexboy;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        Config config = new Config();
        config.setBootStrapServers("172.26.0.7:9092,172.26.0.5:9093,172.26.0.6:9094");
        config.setNumberOfBrokers(3);
        config.setInterval(5);

        final HeatBeatCron heatbeatCron = new HeatBeatCron(config);
        heatbeatCron.cron();
    }
}
