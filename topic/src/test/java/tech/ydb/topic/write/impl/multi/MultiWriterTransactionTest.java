package tech.ydb.topic.write.impl.multi;

import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.common.transaction.YdbTransaction;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.MultiWriteResult.ConfirmationSource;
import tech.ydb.topic.write.WriteAck;
import tech.ydb.topic.write.impl.multi.MultiWriterTestSupport.ControlledWriter;

/** Staging/lifecycle tests. Commit atomicity and physical visibility require the live integration suite. */
public class MultiWriterTransactionTest {
    @Test
    public void prepareCommitSealsAdmissionBeforeWaitingForStaging() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        CompletableFuture<Void> prepared = writer.prepareCommit();
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        CompletableFuture<Void> repeated = writer.prepareCommit();
        f.runUntilIdle();
        Assert.assertFalse(prepared.isDone());
        Assert.assertFalse(repeated.isDone());
        stage(f.active(0), 1);
        f.runUntilIdle();
        Assert.assertTrue(prepared.isDone());
        prepared.join();
        repeated.join();
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, sent.join().getState());
        Assert.assertFalse(tx.status.isDone());
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        shutdown(writer, f, false);
    }

    @Test
    public void sendAdmittedBeforePrepareIsIncludedInItsBarrier() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        writer.send(message(1));
        writer.send(message(2));
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        ControlledWriter lower = f.active(0);
        Assert.assertEquals(2, lower.sent.size());
        stage(lower, 1);
        f.runUntilIdle();
        Assert.assertFalse(prepared.isDone());
        stage(lower, 2);
        f.runUntilIdle();
        Assert.assertTrue(prepared.isDone());
        prepared.join();
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        shutdown(writer, f, false);
    }

    @Test
    public void flushDoesNotSealAdmissionAndStagedPayloadReleasesBudget() throws Exception {
        MultiWriterTestSupport f = new MultiWriterTestSupport(MultiWriterTestSupport.settings()
                .setBufferMessages(1).build());
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        writer.send(message(1));
        CompletableFuture<Void> flush = writer.flush();
        f.runUntilIdle();
        stage(f.active(0), 1);
        f.runUntilIdle();
        Assert.assertTrue(flush.isDone());
        flush.join();
        CompletableFuture<MultiWriteResult> second = writer.send(message(2));
        f.runUntilIdle();
        stage(f.active(0), 2);
        f.runUntilIdle();
        Assert.assertEquals(2, second.join().getSeqNo());
        CompletableFuture<Void> prepare = writer.prepareCommit();
        f.runUntilIdle();
        prepare.join();
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        shutdown(writer, f, false);
    }

    @Test
    public void deduplicatedSameTransactionRetryMayReturnAlreadyWritten() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        f.active(0).ack(new WriteAck(1, WriteAck.State.ALREADY_WRITTEN, null, null));
        f.runUntilIdle();
        Assert.assertEquals(WriteAck.State.ALREADY_WRITTEN, sent.join().getState());
        Assert.assertNull(sent.join().getOffset());
        Assert.assertFalse(tx.status.isDone());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        prepared.join();
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        shutdown(writer, f, false);
    }

    @Test
    public void regularWrittenAckIsNotAcceptedAsTransactionalStaging() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        f.active(0).ack(1, 0);
        f.runUntilIdle();
        Assert.assertTrue(sent.isCompletedExceptionally());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        Assert.assertTrue(prepared.isCompletedExceptionally());
        Assert.assertFalse("Closing the writer is not external rollback", tx.status.isDone());
        tx.status.complete(Status.of(StatusCode.ABORTED));
        f.runUntilIdle();
        shutdown(writer, f, true);
    }

    @Test
    public void lowerWriterReceivesImmutableTransactionView() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        CompletableFuture<Status> originalStatus = tx.status;
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        writer.send(message(1));
        f.runUntilIdle();
        YdbTransaction snapshot = f.active(0).sent.get(0).settings.getTransaction();
        Assert.assertNotSame(tx, snapshot);
        Assert.assertEquals("transaction-1", snapshot.getId());
        Assert.assertEquals("session-1", snapshot.getSessionId());
        Assert.assertSame(originalStatus, snapshot.getStatusFuture());
        tx.id = "transaction-2";
        tx.session = "session-2";
        tx.status = new CompletableFuture<>();
        Assert.assertEquals("transaction-1", snapshot.getId());
        Assert.assertEquals("session-1", snapshot.getSessionId());
        Assert.assertSame(originalStatus, snapshot.getStatusFuture());
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        originalStatus.complete(Status.of(StatusCode.ABORTED));
        f.runUntilIdle();
        shutdown(writer, f, true);
    }

    @Test
    public void identityChangeBetweenAdmissionAndSubmitFailsInsteadOfSendingNewTransaction() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        tx.id = "transaction-2";
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertTrue(f.writers.stream().allMatch(lower -> lower.sent.isEmpty()));
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        Assert.assertTrue(prepared.isCompletedExceptionally());
        Assert.assertFalse(tx.status.isDone());
        tx.status.complete(Status.of(StatusCode.ABORTED));
        f.runUntilIdle();
        shutdown(writer, f, true);
    }

    @Test
    public void rollbackFailsPendingWritesAndNeverStartsAnotherTransaction() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        int before = f.writers.size();
        tx.status.complete(Status.of(StatusCode.ABORTED));
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        Assert.assertEquals(before, f.writers.size());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        Assert.assertTrue(prepared.isCompletedExceptionally());
        Assert.assertThrows(IllegalStateException.class, () -> writer.send(message(2)));
        shutdown(writer, f, true);
    }

    @Test
    public void prematureExternalCommitDoesNotTurnUnacknowledgedMessageIntoSuccess() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> pending = writer.send(message(1));
        f.runUntilIdle();
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        Assert.assertTrue(pending.isCompletedExceptionally());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        Assert.assertTrue(prepared.isCompletedExceptionally());
        shutdown(writer, f, true);
    }

    @Test
    public void unknownCommitResultIsNotRollbackOrAutomaticBusinessRetry() throws Exception {
        MultiWriterTestSupport f = support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        writer.send(message(1));
        f.runUntilIdle();
        stage(f.active(0), 1);
        f.runUntilIdle();
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        prepared.join();
        int before = f.writers.size();
        tx.status.completeExceptionally(new IllegalStateException("Commit response lost; server outcome unknown"));
        f.runUntilIdle();
        Assert.assertTrue(tx.status.isCompletedExceptionally());
        Assert.assertEquals(before, f.writers.size());
        shutdown(writer, f, true);
    }

    @Test
    public void splitBeforeStagingResendsWithSameTransactionAndSequence() throws Exception {
        MultiWriterTestSupport f = MultiWriterRecoveryTest.support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        f.partitions = MultiWriterRecoveryTest.split();
        parent.fail(Status.of(StatusCode.OVERLOADED));
        MultiWriterRecoveryTest.refresh(f);
        ControlledWriter child = f.active(1);
        Assert.assertEquals(Long.valueOf(1), child.sent.get(0).message.getSeqNo());
        Assert.assertEquals("transaction-1", child.sent.get(0).settings.getTransaction().getId());
        Assert.assertEquals(parent.sent.get(0).message.getCreateTimestamp(),
                child.sent.get(0).message.getCreateTimestamp());
        stage(child, 1);
        f.runUntilIdle();
        Assert.assertEquals(ConfirmationSource.ACK, sent.join().getSource());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        prepared.join();
        tx.status.complete(Status.SUCCESS);
        f.runUntilIdle();
        shutdown(writer, f, false);
    }

    @Test
    public void splitAndLostStagingAckNeverUseOrdinaryProducerHistoryAsTransactionSuccess() throws Exception {
        MultiWriterTestSupport f = MultiWriterRecoveryTest.support();
        MutableTransaction tx = new MutableTransaction();
        TransactionalMultiWriterImpl writer = initialized(f, tx);
        CompletableFuture<MultiWriteResult> sent = writer.send(message(1));
        f.runUntilIdle();
        ControlledWriter parent = f.active(0);
        f.histories.put("owner-0", 1L);
        f.partitions = MultiWriterRecoveryTest.split();
        parent.fail(Status.of(StatusCode.OVERLOADED));
        MultiWriterRecoveryTest.refresh(f);
        Assert.assertFalse("Ordinary history must not acknowledge transactional staging", sent.isDone());
        ControlledWriter child = f.active(1);
        Assert.assertEquals(Long.valueOf(1), child.sent.get(0).message.getSeqNo());
        stage(child, 1);
        f.runUntilIdle();
        Assert.assertEquals(ConfirmationSource.ACK, sent.join().getSource());
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, sent.join().getState());
        CompletableFuture<Void> prepared = writer.prepareCommit();
        f.runUntilIdle();
        prepared.join();
        // The pinned live YDB comparison decides the commit status; this tests handling of its ABORTED result.
        tx.status.complete(Status.of(StatusCode.ABORTED));
        f.runUntilIdle();
        Assert.assertEquals(WriteAck.State.WRITTEN_IN_TX, sent.join().getState());
        shutdown(writer, f, true);
    }

    private static MultiWriterTestSupport support() {
        return new MultiWriterTestSupport(MultiWriterTestSupport.settings().build());
    }

    private static TransactionalMultiWriterImpl initialized(MultiWriterTestSupport support, YdbTransaction tx) {
        TransactionalMultiWriterImpl writer = support.createTransactional(tx);
        CompletableFuture<InitResult> init = writer.init();
        support.runUntilIdle();
        Assert.assertTrue(init.isDone());
        init.join();
        return writer;
    }

    private static MultiMessage message(int value) {
        return MultiMessage.of(new byte[] { (byte) value }, "a");
    }

    private static void stage(ControlledWriter lower, long sequence) {
        lower.ack(new WriteAck(sequence, WriteAck.State.WRITTEN_IN_TX, null, null));
    }

    private static void shutdown(TransactionalMultiWriterImpl writer, MultiWriterTestSupport support, boolean failure) {
        CompletableFuture<Void> closed = writer.shutdown();
        support.runUntilIdle();
        Assert.assertTrue("Shutdown must complete", closed.isDone());
        Assert.assertEquals(failure, closed.isCompletedExceptionally());
        if (!failure) {
            closed.join();
        }
    }

    private static final class MutableTransaction implements YdbTransaction {
        private String id = "transaction-1";
        private String session = "session-1";
        private CompletableFuture<Status> status = new CompletableFuture<>();

        @Override
        public String getId() {
            return id;
        }

        @Override
        public TxMode getTxMode() {
            return TxMode.SERIALIZABLE_RW;
        }

        @Override
        public String getSessionId() {
            return session;
        }

        @Override
        public CompletableFuture<Status> getStatusFuture() {
            return status;
        }
    }
}
