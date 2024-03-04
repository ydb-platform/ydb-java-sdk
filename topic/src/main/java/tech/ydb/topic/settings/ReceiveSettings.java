package tech.ydb.topic.settings;

import tech.ydb.common.transaction.BaseTransaction;

/**
 * @author Nikolay Perfilov
 */
public class ReceiveSettings {
    private final BaseTransaction transaction;

    private ReceiveSettings(Builder builder) {
        this.transaction = builder.transaction;
    }

    public BaseTransaction getTransaction() {
        return transaction;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private BaseTransaction transaction;

        /**
         * Set transaction for receiving message.
         * When this transaction is committed, the message will be considered by server as read (committed)
         * If this transaction is rolled back, the reader will restart reading stream internally
         *
         * @param transaction Transaction to link a message with
         * @return Builder
         */
        public Builder setTransaction(BaseTransaction transaction) {
            this.transaction = transaction;
            return this;
        }

        public ReceiveSettings build() {
            return new ReceiveSettings(this);
        }

    }
}
