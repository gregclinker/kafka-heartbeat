package com.essexboy;

import java.util.concurrent.ExecutionException;

public interface HeatBeatService {
    boolean isUp() throws Exception;

    boolean isRebalancing();

    boolean swicthIsrDown();

    boolean swicthIsrBack();
}
