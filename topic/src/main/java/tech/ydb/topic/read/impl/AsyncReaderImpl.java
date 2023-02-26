package tech.ydb.topic.read.impl;

import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.read.AsyncReader;

/**
 * @author Nikolay Perfilov
 */
public class AsyncReaderImpl implements AsyncReader {

    @Override
    public void init() { }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }
}
