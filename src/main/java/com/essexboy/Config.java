package com.essexboy;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Config {
    private String bootStrapServers;
    private int numberOfBrokers;
    private int interval;
}
