package com.essexboy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.admin.AdminClient;

import java.util.Properties;

@Getter
@AllArgsConstructor
@ToString
public class HeartBeatServiceImpl implements HeatBeatService {

    private Config config;

    private AdminClient getAdminClient() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", config.getBootStrapServers());
        properties.put("connections.max.idle.ms", 10000);
        properties.put("request.timeout.ms", 5000);
        return AdminClient.create(properties);
    }

    @Override
    public boolean isUp() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final int brokerCount = adminClient.describeCluster().nodes().get().size();
        adminClient.close();
        return config.getNumberOfBrokers() == brokerCount;
    }

    @Override
    public boolean isRebalancing() {
        return false;
    }

    @Override
    public boolean swicthIsrDown() {
        return false;
    }

    @Override
    public boolean swicthIsrBack() {
        return false;
    }
}
