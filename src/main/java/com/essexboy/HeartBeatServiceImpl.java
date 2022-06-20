package com.essexboy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.admin.AdminClient;

@Getter
@AllArgsConstructor
@ToString
public class HeartBeatServiceImpl implements HeatBeatService {

    private HeartBeatConfig heartBeatConfig;

    private AdminClient getAdminClient() {
        return AdminClient.create(heartBeatConfig.getKafkaProperties());
    }

    @Override
    public boolean isUp() throws Exception {
        final AdminClient adminClient = getAdminClient();
        final int brokerCount = adminClient.describeCluster().nodes().get().size();
        adminClient.close();
        return heartBeatConfig.getNumberOfBrokers() == brokerCount;
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
