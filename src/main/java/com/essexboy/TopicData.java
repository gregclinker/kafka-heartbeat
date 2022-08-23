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
public class TopicData {
    private String name;
    private List<PartitionData> partitions = Collections.emptyList();
}
