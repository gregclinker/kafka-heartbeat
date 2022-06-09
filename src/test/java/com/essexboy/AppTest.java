package com.essexboy;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {
    @Test
    public void test1() throws Exception {
        assertTrue(true);

        Properties properties = new Properties();
        String bootstrapServers = "172.26.0.7:9092,172.26.0.5:9093,172.26.0.6:9094";
        properties.put("bootstrap.servers", bootstrapServers);
        properties.put("connections.max.idle.ms", 10000);
        properties.put("request.timeout.ms", 5000);
        AdminClient client = AdminClient.create(properties);
        assertNotNull(client);

        final Collection<TopicListing> topicListings = client.listTopics().listings().get();
        for (TopicListing topicListing : topicListings) {
            System.out.println(topicListing.toString());
        }

        // get all the brokers
        final DescribeClusterResult describeClusterResult = client.describeCluster();
        describeClusterResult.nodes().get().stream().forEach(s->{
            System.out.println(s);
        });

        System.out.println("nodes=" + describeClusterResult.nodes().get().size());

        System.out.println(describeClusterResult.controller().get());

    }
}
