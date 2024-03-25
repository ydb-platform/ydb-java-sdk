package tech.ydb.topic.settings;

import java.util.concurrent.TimeUnit;

import tech.ydb.common.transaction.YdbTransaction;

/**
 * @author Nikolay Perfilov
 */
public class ReceiveSettings {
    private Long timeout;
    private TimeUnit timeoutTimeUnit;
    private final YdbTransaction transaction;

    private ReceiveSettings(Builder builder) {
        this.timeout = builder.timeout;
        this.timeoutTimeUnit = builder.timeoutTimeUnit;
        this.transaction = builder.transaction;
    }

    public Long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeoutTimeUnit() {
        return timeoutTimeUnit;
    }

    public YdbTransaction getTransaction() {
        return transaction;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Long timeout;
        private TimeUnit timeoutTimeUnit;
        private YdbTransaction transaction;

        /**
         * Set timeout for receiving a message.
         *
         * @param timeout timeout for receiving a message
         * @param unit {@link TimeUnit} for timeout
         * @return Builder
         */
        public Builder setTimeout(long timeout, TimeUnit unit) {
            this.timeout = timeout;
            this.timeoutTimeUnit = unit;
            return this;
        }

        /**
         * Set transaction for receiving message.
         * When this transaction is committed, the message will be considered by server as read (committed)
         * If this transaction is rolled back, the reader will restart reading stream internally
         *
         * @param transaction Transaction to link a message with.
         *                    Transaction has to be active
         * @return Builder
         */
        public Builder setTransaction(YdbTransaction transaction) {
            if (!transaction.isActive()) {
                throw new IllegalArgumentException("Transaction is not active. " +
                        "Can only write topic messages in already running transactions from other services");
            }
            this.transaction = transaction;
            return this;
        }

        public ReceiveSettings build() {
            return new ReceiveSettings(this);
        }

    }
}
