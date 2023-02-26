package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

/**
 * @author Nikolay Perfilov
 */
public interface AsyncReader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    void init();

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();
}
