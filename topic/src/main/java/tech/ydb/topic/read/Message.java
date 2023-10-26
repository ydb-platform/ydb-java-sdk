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
     * @return Message byte data.
     * @throws DecompressionException in case of decompression error. Raw data can be retrieved this exception
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
     * @return Partition session of this message
     */
    PartitionSession getPartitionSession();

    /**
     * Commits this message
     * If there was an error while committing, there is no point of retrying committing the same message:
     * the whole PartitionSession should be shut down by that time. And if commit hadn't reached the server,
     * it will resend all these messages in next PartitionSession.
     *
     * @return CompletableFuture that will be completed when commit confirmation from server will be received
     */
    CompletableFuture<Void> commit();

}
