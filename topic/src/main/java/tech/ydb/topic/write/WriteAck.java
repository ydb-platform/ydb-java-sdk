package tech.ydb.topic.write;

import java.time.Duration;

/**
 * @author Nikolay Perfilov
 */
public class WriteAck {
    private final long seqNo;
    private final State state;
    private final Details details;
    private final Statistics statistics;

    public WriteAck(long seqNo, State state, Details details, Statistics statistics) {
        this.seqNo = seqNo;
        this.state = state;
        this.details = details;
        this.statistics = statistics;
    }

    public enum State {
        WRITTEN,
        ALREADY_WRITTEN,
        WRITTEN_IN_TX
    }

    public long getSeqNo() {
        return seqNo;
    }

    public State getState() {
        return state;
    }

    /**
     * Get details about written offsets
     * @return {@link Details} with written offsets if state is {@link State#WRITTEN} or null otherwise
     */
    public Details getDetails() {
        return details;
    }

    /**
     * Obtain message write statistics
     * @return {@link Statistics} with timings if statistics are available or null otherwise
     */
    public Statistics getStatistics() {
        return statistics;
    }

    public static class Details {
        private final long offset;

        public Details(long offset) {
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }
    }

    public static class Statistics {
        private final Duration persistingTime;
        private final Duration partitionQuotaWaitTime;
        private final Duration topicQuotaWaitTime;
        private final Duration maxQueueWaitTime;
        private final Duration minQueueWaitTime;

        public Statistics(Duration persistingTime,
                Duration partitionQuotaWaitTime, Duration topicQuotaWaitTime,
                Duration maxQueueWaitTime, Duration minQueueWaitTime) {
            this.persistingTime = persistingTime;
            this.partitionQuotaWaitTime = partitionQuotaWaitTime;
            this.topicQuotaWaitTime = topicQuotaWaitTime;
            this.maxQueueWaitTime = maxQueueWaitTime;
            this.minQueueWaitTime = minQueueWaitTime;
        }

        public Duration getPersistingTime() {
            return persistingTime;
        }

        public Duration getPartitionQuotaWaitTime() {
            return partitionQuotaWaitTime;
        }

        public Duration getTopicQuotaWaitTime() {
            return topicQuotaWaitTime;
        }

        public Duration getMaxQueueWaitTime() {
            return maxQueueWaitTime;
        }

        public Duration getMinQueueWaitTime() {
            return minQueueWaitTime;
        }
    }

}
