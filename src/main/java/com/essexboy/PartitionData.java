package com.essexboy;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class PartitionData {
    private Integer id;
    private Integer leader;
    private List<Integer> isrs;
    private List<Integer> replicas;

    public PartitionData(TopicPartitionInfo topicPartitionInfo) {
        this.id = topicPartitionInfo.partition();
        if (topicPartitionInfo.leader() != null) {
            this.leader = topicPartitionInfo.leader().id();
        }
        this.isrs = topicPartitionInfo.isr().stream().map(Node::id).sorted().collect(Collectors.toList());
        this.replicas = topicPartitionInfo.replicas().stream().map(Node::id).sorted().collect(Collectors.toList());
    }
}
