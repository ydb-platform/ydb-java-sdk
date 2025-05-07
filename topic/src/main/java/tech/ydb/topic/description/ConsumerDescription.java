package tech.ydb.topic.description;


import java.util.List;
import java.util.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConsumerDescription that = (ConsumerDescription) o;
        return Objects.equals(consumer, that.consumer) && Objects.equals(partitions, that.partitions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumer, partitions);
    }
}
