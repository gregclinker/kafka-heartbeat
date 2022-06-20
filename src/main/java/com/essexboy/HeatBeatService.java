package com.essexboy;

public interface HeatBeatService {
    boolean isUp() throws Exception;

    boolean isRebalancing();

    boolean swicthIsrDown();

    boolean swicthIsrBack();
}
