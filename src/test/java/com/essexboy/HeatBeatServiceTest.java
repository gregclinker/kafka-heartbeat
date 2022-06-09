package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatBeatServiceTest {

    private HeatBeatService heatBeatService;

    @BeforeEach
    private void init() {
        Config config = new Config();
        config.setBootStrapServers("172.26.0.7:9092,172.26.0.5:9093,172.26.0.6:9094");
        config.setNumberOfBrokers(3);
        heatBeatService = new HeartBeatServiceImpl(config);
    }

    @Test
    void isUp() throws Exception {
        assertTrue(heatBeatService.isUp());
    }

    @Test
    void isRebalancing() {
    }

    @Test
    void swicthIsrDown() {
    }

    @Test
    void swicthIsrBack() {
    }
}