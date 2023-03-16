package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface AsyncWriter {

    /**
     * Initialize internal threads in the background. Non-blocking
     * @return {@link CompletableFuture} with {@link InitResult} containing initialization data like lastSeqNo
     */
    CompletableFuture<InitResult> init();

    /**
     * Send message. Non-blocking
     * @param message message data to write
     * @return {@link CompletableFuture} with {@link WriteAck} for write acknowledgement
     */
    CompletableFuture<WriteAck> send(Message message) throws QueueOverflowException;

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();
}
