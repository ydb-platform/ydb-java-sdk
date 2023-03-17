package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface AsyncReader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    CompletableFuture<Void> init();

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();
}
