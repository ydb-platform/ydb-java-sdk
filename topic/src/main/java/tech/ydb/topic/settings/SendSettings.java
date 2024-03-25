package tech.ydb.topic.settings;

import tech.ydb.common.transaction.YdbTransaction;

/**
 * @author Nikolay Perfilov
 */
public class SendSettings {
    private final YdbTransaction transaction;

    private SendSettings(Builder builder) {
        this.transaction = builder.transaction;
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
        private YdbTransaction transaction;

        /**
         * Set transaction for sending message.
         * When this transaction is committed, the message will be considered by server as written
         * If this transaction is rolled back, the writer will be shut down
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

        public SendSettings build() {
            return new SendSettings(this);
        }

    }
}
