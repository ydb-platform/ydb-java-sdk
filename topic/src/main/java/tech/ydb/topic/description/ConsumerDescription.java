package tech.ydb.topic.description;


import java.util.List;
import java.util.stream.Collectors;

import tech.ydb.proto.topic.YdbTopic;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ConsumerDescription {
    private final Consumer consumer;
    private final List<ConsumerPartitionInfo> partitions;

    public ConsumerDescription(YdbTopic.DescribeConsumerResult result) {
        this.consumer = new Consumer(result.getConsumer());
        this.partitions = result.getPartitionsList().stream()
                .map(ConsumerPartitionInfo::new)
                .collect(Collectors.toList());
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public List<ConsumerPartitionInfo> getPartitions() {
        return partitions;
    }
}
