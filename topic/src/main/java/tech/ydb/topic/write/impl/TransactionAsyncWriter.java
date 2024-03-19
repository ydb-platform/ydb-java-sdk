package tech.ydb.topic.write.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * @author Nikolay Perfilov
 */
public class TransactionAsyncWriter implements AsyncWriter {
    private final AsyncWriterImpl originalWriter;
    private final BaseTransaction transaction;
    TransactionAsyncWriter(AsyncWriterImpl originalWriter, BaseTransaction transaction) {
        this.originalWriter = originalWriter;
        this.transaction = transaction;
    }

    @Override
    public CompletableFuture<InitResult> init() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException {
        try {
            return originalWriter.sendImpl(message, transaction, true).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof QueueOverflowException) {
                throw (QueueOverflowException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public AsyncWriter getTransactionWriter(BaseTransaction transaction) {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        throw new UnsupportedOperationException("Can't use this method in virtual transaction writer");
    }
}
