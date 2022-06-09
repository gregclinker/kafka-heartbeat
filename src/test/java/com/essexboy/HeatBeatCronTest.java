package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HeatBeatCronTest {

    private HeatBeatCron heatBeatCron;

    @BeforeEach
    private void init() {
        Config config = new Config();
        config.setBootStrapServers("172.26.0.7:9092,172.26.0.5:9093,172.26.0.6:9094");
        config.setNumberOfBrokers(3);
        config.setInterval(1);
        heatBeatCron = new HeatBeatCron(config);
    }

    @Test
    public void test1() throws InterruptedException {
        heatBeatCron.cron();
        Thread.sleep(5000);
        heatBeatCron.stop();
    }
}