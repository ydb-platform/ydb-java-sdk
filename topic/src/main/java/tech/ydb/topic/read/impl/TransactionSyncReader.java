package tech.ydb.topic.read.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.topic.read.Message;
import tech.ydb.topic.read.SyncReader;

/**
 * @author Nikolay Perfilov
 */
public class TransactionSyncReader implements SyncReader {
    private final SyncReaderImpl originalWriter;
    private final BaseTransaction transaction;
    TransactionSyncReader(SyncReaderImpl originalWriter, BaseTransaction transaction) {
        this.originalWriter = originalWriter;
        this.transaction = transaction;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public void initAndWait() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    @Nullable
    public Message receive(long timeout, TimeUnit unit) throws InterruptedException {
        return originalWriter.receiveInternal(timeout, unit, transaction);
    }

    @Override
    public Message receive() throws InterruptedException {
        return originalWriter.receiveInternal(transaction);
    }

    @Override
    public SyncReader getTransactionReader(BaseTransaction transaction) {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }
}
