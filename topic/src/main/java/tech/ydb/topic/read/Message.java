package tech.ydb.topic.read;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Nikolay Perfilov
 */
public interface Message {

    byte[] getData();
    long getOffset();
    long getSeqNo();
    Instant getCreatedAt();
    String getMessageGroupId();
    String getProducerId();
    Map<String, String> getWriteSessionMeta();
    Instant getWrittenAt();

    // Non-blocking
    CompletableFuture<Void> commit();

}
