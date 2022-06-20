package com.essexboy;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {

    @Test
    public void test1() throws Exception {

        final HeartBeatConfig heartBeatConfig = new HeartBeatConfig(getClass().getResourceAsStream("/testConfig.yaml"));
        AdminClient client = AdminClient.create(heartBeatConfig.getKafkaProperties());
        assertNotNull(client);

        final Collection<TopicListing> topicListings = client.listTopics().listings().get();
        for (TopicListing topicListing : topicListings) {
            System.out.println(topicListing.toString());
        }

        // get all the brokers
        final DescribeClusterResult describeClusterResult = client.describeCluster();
        describeClusterResult.nodes().get().stream().forEach(s -> {
            System.out.println(s);
        });

        System.out.println("nodes=" + describeClusterResult.nodes().get().size());

        System.out.println(describeClusterResult.controller().get());
    }
}
