package tech.ydb.topic.write.impl.multi;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.TransactionalMultiWriter;
import tech.ydb.topic.write.WriteAck;

/** A transaction-bound facade. Transaction completion and its unknown outcome belong to the caller. */
public final class TransactionalMultiWriterImpl extends MultiWriterImpl implements TransactionalMultiWriter {
    private final TransactionView transaction;

    public TransactionalMultiWriterImpl(MultiWriterSettings settings, Codec codec,
            Function<WriterSettings, AsyncWriter> factory,
            Supplier<CompletableFuture<List<PartitionInfo>>> describer, YdbTransaction transaction) {
        this(settings, codec, factory, describer, new MultiWriterRuntime(settings), transaction);
    }

    TransactionalMultiWriterImpl(MultiWriterSettings settings, Codec codec,
            Function<WriterSettings, AsyncWriter> factory,
            Supplier<CompletableFuture<List<PartitionInfo>>> describer,
            MultiWriterRuntime runtime, YdbTransaction transaction) {
        super(settings, codec, factory, describer, runtime);
        this.transaction = new TransactionView(transaction);
        this.transaction.status.whenComplete((status, error) -> sessionEvent(() -> {
            if (error != null) {
                fail(error);
            } else if (status == null || !status.isSuccess()) {
                fail(status == null ? new IllegalStateException("Missing external transaction outcome")
                        : new UnexpectedResultException("External transaction did not commit", status));
            } else if (!isPrepared()) {
                fail(new IllegalStateException("External transaction committed before prepareCommit completed"));
            } else {
                shutdown();
            }
        }));
    }

    @Override
    public CompletableFuture<Void> prepareCommit() {
        return prepareCommitBarrier();
    }

    @Override
    protected void checkAdmission() {
        super.checkAdmission();
        try {
            transaction.verify();
        } catch (IllegalStateException error) {
            sessionEvent(() -> fail(error));
            throw error;
        }
    }

    @Override
    protected SendSettings beforeSubmit() {
        transaction.verify();
        return SendSettings.newBuilder().setTransaction(transaction).build();
    }

    @Override
    protected void validateAcknowledgement(WriteAck ack) {
        // Reconnect of an identical staged message can return SKIPPED/ALREADY_WRITTEN (YDB 25.1.4.7).
        // Exclusive prefix ownership and increasing SeqNo keep it in this captured transaction attempt.
        if (ack.getState() != WriteAck.State.WRITTEN_IN_TX && ack.getState() != WriteAck.State.ALREADY_WRITTEN) {
            throw new IllegalStateException("Ordinary written ACK for a transaction-bound message");
        }
    }

    @Override
    protected void checkRecoveryAllowed() {
        transaction.verify();
    }

    @Override
    protected boolean mayConfirmFromHistory() {
        // Ordinary producer history cannot confirm staging in this transaction, let alone its commit.
        // Resend unresolved messages in the SAME transaction after the parent becomes inactive.
        return false;
    }

    private static final class TransactionView implements YdbTransaction {
        private final YdbTransaction original;
        private final String id;
        private final String session;
        private final TxMode mode;
        private final CompletableFuture<Status> status;

        TransactionView(YdbTransaction original) {
            this.original = Objects.requireNonNull(original, "transaction");
            this.id = Objects.requireNonNull(original.getId(), "active transaction ID");
            this.session = Objects.requireNonNull(original.getSessionId(), "transaction session ID");
            this.mode = Objects.requireNonNull(original.getTxMode(), "transaction mode");
            this.status = Objects.requireNonNull(original.getStatusFuture(), "transaction status future");
            verify();
        }

        void verify() {
            if (status.isDone() || !id.equals(original.getId()) || !session.equals(original.getSessionId())
                    || original.getStatusFuture() != status) {
                throw new IllegalStateException("Transaction ended, changed, or commit/rollback already started");
            }
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getSessionId() {
            return session;
        }

        @Override
        public TxMode getTxMode() {
            return mode;
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return status;
        }
    }
}
