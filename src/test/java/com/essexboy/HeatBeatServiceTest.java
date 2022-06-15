package com.essexboy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeatBeatServiceTest {

    private HeatBeatService heatBeatService;

    @BeforeEach
    private void init() throws IOException {
        final HeatBeatCron heatBeatCron = new HeatBeatCron(getClass().getResourceAsStream("/testConfig.yaml"));
        heatBeatService = new HeartBeatServiceImpl(heatBeatCron.getConfig());
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