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
     * Returns write statistics associated with this write confirmation.
     * Note: The statistics may cover multiple messages confirmed together by the server.
     * Although this WriteAck corresponds to a single written message, the server may confirm several messages in a single response.
     * Therefore, the returned statistics may represent the combined data for all messages included in the same write confirmation from the server.
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

    /**
     * Messages batch statistics.
     * All messages within the batch are persisted together so write
     * statistics is for the whole messages batch.
     */
    public static class Statistics {
        private final Duration persistingTime;
        private final Duration partitionQuotaWaitTime;
        private final Duration topicQuotaWaitTime;
        private final Duration maxQueueWaitTime;
        private final Duration minQueueWaitTime;

        /**
         * Create the messages batch statistics object, for a single messages batch.
         *
         * @param persistingTime
         * @param partitionQuotaWaitTime
         * @param topicQuotaWaitTime
         * @param maxQueueWaitTime
         * @param minQueueWaitTime
         */
        public Statistics(Duration persistingTime,
                Duration partitionQuotaWaitTime, Duration topicQuotaWaitTime,
                Duration maxQueueWaitTime, Duration minQueueWaitTime) {
            this.persistingTime = persistingTime;
            this.partitionQuotaWaitTime = partitionQuotaWaitTime;
            this.topicQuotaWaitTime = topicQuotaWaitTime;
            this.maxQueueWaitTime = maxQueueWaitTime;
            this.minQueueWaitTime = minQueueWaitTime;
        }

        /**
         * @return Time spent in persisting of data.
         */
        public Duration getPersistingTime() {
            return persistingTime;
        }

        /**
         * @return Time spent awaiting for partition write quota.
         */
        public Duration getPartitionQuotaWaitTime() {
            return partitionQuotaWaitTime;
        }

        /**
         * @return Time spent awaiting for topic write quota.
         */
        public Duration getTopicQuotaWaitTime() {
            return topicQuotaWaitTime;
        }

        /**
         * @return Time spent in queue before persisting, maximal of all messages in response.
         */
        public Duration getMaxQueueWaitTime() {
            return maxQueueWaitTime;
        }

        /**
         * @return Time spent in queue before persisting, minimal of all messages in response.
         */
        public Duration getMinQueueWaitTime() {
            return minQueueWaitTime;
        }
    }

}
