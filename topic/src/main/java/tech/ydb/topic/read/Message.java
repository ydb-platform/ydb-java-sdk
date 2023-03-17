package tech.ydb.topic.read;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface Message {
    /**
     * @return Message byte data
     */
    byte[] getData();

    /**
     * @return Message offset
     */
    long getOffset();

    /**
     * @return Message seqNo
     */
    long getSeqNo();

    /**
     * @return Message creation time
     */
    Instant getCreatedAt();

    /**
     * @return Message group id
     */
    String getMessageGroupId();

    /**
     * @return producer id
     */
    String getProducerId();

    /**
     * @return write session metadata
     */
    Map<String, String> getWriteSessionMeta();

    /**
     * @return Time the message was written at
     */
    Instant getWrittenAt();

    /**
     * Commit this message
     * @return CompletableFuture that will be completed when commit confirmation from server will be received
     */
    CompletableFuture<Void> commit();

}
