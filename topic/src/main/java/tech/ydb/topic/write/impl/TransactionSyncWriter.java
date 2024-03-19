package tech.ydb.topic.write.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.SyncWriter;

/**
 * @author Nikolay Perfilov
 */
public class TransactionSyncWriter implements SyncWriter {
    private final SyncWriterImpl originalWriter;
    private final BaseTransaction transaction;
    TransactionSyncWriter(SyncWriterImpl originalWriter, BaseTransaction transaction) {
        this.originalWriter = originalWriter;
        this.transaction = transaction;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public InitResult initAndWait() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public void send(Message message) {
        originalWriter.sendImpl(message, transaction, false).join();
    }

    @Override
    public void send(Message message, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        originalWriter.sendImpl(message, transaction, false).get(timeout, unit);
    }

    @Override
    public SyncWriter getTransactionWriter(BaseTransaction transaction) {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public void shutdown(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
            TimeoutException {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }



}
