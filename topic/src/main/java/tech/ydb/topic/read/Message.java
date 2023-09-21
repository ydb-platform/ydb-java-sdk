package tech.ydb.topic.read;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import io.grpc.ExperimentalApi;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface Message {
    /**
     * @return Message byte data.
     * @throws IOException in case of decompression error. Raw data can be retrieved via getRawData() method
     */
    byte[] getData() throws IOException;

    /**
     * @return Message raw byte data if it was not compressed. Return null if the message was actually decompressed
     * Data may not be compressed in 2 cases:
     * 1) It shouldn't be compressed due to codec settings (RAW codec)
     * 2) There was an exception caught during decompression. Use getException method to get that exception
     */
    @Nullable
    byte[] getRawData();

    /**
     * @return IOException if it was thrown during message decompression
     * Data may not be compressed in 2 cases:
     * 1) It shouldn't be compressed due to codec settings (RAW codec)
     * 2) There was an exception caught during decompression. Use getException method to get that exception
     */
    @Nullable
    IOException getException();

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
     * Commit this message
     * @return CompletableFuture that will be completed when commit confirmation from server will be received
     */
    CompletableFuture<Void> commit();

}
