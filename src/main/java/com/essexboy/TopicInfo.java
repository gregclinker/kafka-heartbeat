package com.essexboy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class TopicInfo {
    private String name;
    private List<PartitionInfo> partitions = Collections.emptyList();

    public int getPartitionMinIsr() {
        int minIsr = 10;
        for (PartitionInfo partitionInfo : partitions) {
            if (partitionInfo.getIsrs().size() < minIsr) {
                minIsr = partitionInfo.getIsrs().size();
            }
        }
        return minIsr;
    }
}
