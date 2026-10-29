package tech.ydb.topic.write;

import java.util.concurrent.CompletableFuture;

/**
 * Writes to several topic partitions using one sequence-number domain and producer prefix.
 * Only one live writer may own a given prefix for a topic. Sequential sends for the same key
 * preserve admission order within this writer; independent transactions have no shared commit order.
 */
public interface AsyncMultiWriter {
    /**
     * Loads topology and producer history. The returned sequence number is the bootstrap maximum.
     *
     * @return initialization result
     */
    CompletableFuture<InitResult> init();

    /**
     * Admits an immutable message snapshot after successful init and returns its logical acknowledgement.
     * Validation, lifecycle and capacity rejections occur synchronously before admission.
     * An automatic sequence number identifies this call, even if key and payload match an earlier call.
     * SDK retries preserve that identity. A successful result in transactional mode confirms staging only.
     * Cancelling the returned future does not retract a message already admitted or prove its absence.
     *
     * @param message immutable input
     * @return acknowledgement, which may survive replacement of a partition writer
     * @throws QueueOverflowException when the message was not admitted because the shared buffer is full
     */
    CompletableFuture<MultiWriteResult> send(MultiMessage message) throws QueueOverflowException;

    /**
     * Waits for the outcomes of messages admitted before this call. Later sends do not extend this barrier.
     * Failures before the barrier are propagated; cancelling this wait does not cancel admitted messages.
     * Transactional acknowledgements are staging results, not commit results.
     *
     * @return independent barrier future
     */
    CompletableFuture<Void> flush();

    /**
     * Stops admission, drains accepted messages within the configured deadline and closes owned writers.
     * A timeout cannot prove that the server did not write a message or that all lower resources stopped.
     * This method does not close the shared TopicClient, transport, or an external transaction/session.
     *
     * @return idempotent shutdown result
     */
    CompletableFuture<Void> shutdown();
}
