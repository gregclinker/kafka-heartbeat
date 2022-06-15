package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HeatBeatCronTest {

    private HeatBeatCron heatBeatCron;

    @BeforeEach
    private void init() throws IOException {
        heatBeatCron = new HeatBeatCron(getClass().getResourceAsStream("/testConfig.yaml"));
    }

    @Test
    public void config() {
        assertNotNull(heatBeatCron.getConfig());
    }

    @Test
    public void isDown() throws InterruptedException {
        heatBeatCron.getConfig().setNumberOfBrokers(4);
        heatBeatCron.cron();
        Thread.sleep(5000);
        assertEquals(5, heatBeatCron.getFailCount());
        heatBeatCron.stop();
    }

    @Test
    public void isUp() throws InterruptedException {
        heatBeatCron.cron();
        Thread.sleep(5000);
        assertEquals(5, heatBeatCron.getPassCount());
        heatBeatCron.stop();
    }

    @Test
    public void isUpThenDown() throws InterruptedException {
        heatBeatCron.cron();
        Thread.sleep(2000);
        heatBeatCron.getConfig().setNumberOfBrokers(4);
        Thread.sleep(3000);
        assertEquals(0, heatBeatCron.getPassCount());
        assertEquals(3, heatBeatCron.getFailCount());
        heatBeatCron.stop();
    }

}