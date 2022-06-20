package com.essexboy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatBeatServiceTest {

    @Mock
    private HeatBeatService heatBeatService;

    @Test
    void isUp() throws Exception {
        when(heatBeatService.isUp()).thenReturn(true);
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