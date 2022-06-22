package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeatBeatCronTest {

    private HeatBeatCron heatBeatCron;


    @BeforeEach
    private void init() throws Exception {
        heatBeatCron = new HeatBeatCron(getClass().getResourceAsStream("/testConfig.yaml"));
        ((HeartBeatService) heatBeatCron.getHeartBeatService()).setMinIsr("greg-test1", 2);
    }

    @Test
    public void config() {
        assertNotNull(heatBeatCron.getHeartBeatConfig());
    }

    @Test
    public void isDown() throws Exception {
        assertEquals(2, heatBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heatBeatCron.getHeartBeatConfig().setNumberOfBrokers(4);
        heatBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heatBeatCron.getFailCount() > 0);
        assertEquals(0, heatBeatCron.getPassCount());
        Thread.sleep(10000);
        assertEquals(1, heatBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heatBeatCron.stop();
        assertEquals(1, heatBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
    }

    @Test
    public void isUp() throws Exception {
        heatBeatCron.cron();
        Thread.sleep(3000);
        assertTrue(heatBeatCron.getPassCount() > 0);
        assertEquals(0, heatBeatCron.getFailCount());
        heatBeatCron.stop();
    }

    @Test
    public void isUpThenDown() throws Exception {
        assertEquals(2, heatBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heatBeatCron.cron();
        Thread.sleep(2000);
        heatBeatCron.getHeartBeatConfig().setNumberOfBrokers(4);
        Thread.sleep(3000);
        assertEquals(0, heatBeatCron.getPassCount());
        assertEquals(3, heatBeatCron.getFailCount());
        assertEquals(1, heatBeatCron.getHeartBeatService().getMinIsr("greg-test1"));
        heatBeatCron.stop();
    }

}