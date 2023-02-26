package tech.ydb.topic.read;

import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;

/**
 * @author Nikolay Perfilov
 */
public class Message {

    public ByteString getData() {
        // Temp -----
        return ByteString.copyFromUtf8("Message data");
        // ----------
    }

    // Non-blocking
    public CompletableFuture<Void> commit() {
        return new CompletableFuture<>();
    }

}
