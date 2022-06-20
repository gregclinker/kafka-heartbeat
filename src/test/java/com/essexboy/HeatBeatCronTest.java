package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class HeatBeatCronTest {

    private HeatBeatCron heatBeatCron;

    @BeforeEach
    private void init() throws IOException {
        heatBeatCron = new HeatBeatCron(getClass().getResourceAsStream("/testConfig.yaml"));
    }

    @Test
    public void config() {
        assertNotNull(heatBeatCron.getHeartBeatConfig());
    }

    @Test
    public void isDown() throws InterruptedException {
        heatBeatCron.getHeartBeatConfig().setNumberOfBrokers(4);
        heatBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heatBeatCron.getFailCount() > 0);
        assertEquals(0, heatBeatCron.getPassCount());
        heatBeatCron.stop();
    }

    @Test
    public void isUp() throws InterruptedException {
        heatBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heatBeatCron.getPassCount() > 0);
        assertEquals(0, heatBeatCron.getFailCount());
        heatBeatCron.stop();
    }

    @Test
    public void isUpThenDown() throws InterruptedException {
        heatBeatCron.cron();
        Thread.sleep(2000);
        heatBeatCron.getHeartBeatConfig().setNumberOfBrokers(4);
        Thread.sleep(3000);
        assertEquals(0, heatBeatCron.getPassCount());
        assertEquals(3, heatBeatCron.getFailCount());
        heatBeatCron.stop();
    }

}