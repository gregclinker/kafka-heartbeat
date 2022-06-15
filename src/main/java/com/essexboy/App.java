package com.essexboy;

import java.io.FileInputStream;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

        try {
            if (args.length == 1) {
                final HeatBeatCron heatbeatCron = new HeatBeatCron(new FileInputStream(args[0]));
                heatbeatCron.cron();
            } else {
                help();
            }
        } catch (Exception e) {
            e.printStackTrace();
            help();
        }
    }

    private static void help() {
        System.out.println("");
        System.out.println("usage : java -jar kafka-heartbeat.jar <config file>");
        System.out.println("");
        System.exit(0);
    }
}
