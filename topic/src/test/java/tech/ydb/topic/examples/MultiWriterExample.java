package tech.ydb.topic.examples;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.description.Consumer;
import tech.ydb.topic.settings.CreateTopicSettings;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.PartitioningSettings;
import tech.ydb.topic.write.AsyncMultiWriter;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;

/** Run against a local, unauthenticated YDB: MultiWriterExample localhost:2136 /local. */
public final class MultiWriterExample {
    private MultiWriterExample() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: MultiWriterExample <host:port> <database>");
        }
        String name = "multiwriter-example-" + UUID.randomUUID();
        String path = args[1] + (args[1].endsWith("/") ? "" : "/") + name;
        try (GrpcTransport transport = GrpcTransport.forEndpoint(args[0], args[1]).build();
                TopicClient client = TopicClient.newClient(transport).build()) {
            client.createTopic(path, CreateTopicSettings.newBuilder()
                    .addConsumer(Consumer.newBuilder().setName("example-consumer").build())
                    .setPartitioningSettings(PartitioningSettings.newBuilder()
                            .setMinActivePartitions(2).setMaxActivePartitions(2).build())
                    .build()).join().expectSuccess("Cannot create the example topic");
            // Both cleanup actions belong to this example. A cleanup failure is suppressed on an earlier error.
            try (AutoCloseable topicCleanup = () -> client.dropTopic(path).join().expectSuccess()) {
                AsyncMultiWriter writer = client.createAsyncMultiWriter(MultiWriterSettings.newBuilder()
                        .setTopicPath(path).setProducerPrefix(name).build());
                try (AutoCloseable writerCleanup = () -> writer.shutdown().join()) {
                    writer.init().join();
                    // Pipeline messages: each key keeps its own admission order without waiting for every ACK.
                    CompletableFuture<MultiWriteResult> first = writer.send(message("account-1", "event-1"));
                    CompletableFuture<MultiWriteResult> second = writer.send(message("account-2", "event-2"));
                    CompletableFuture<MultiWriteResult> third = writer.send(message("account-1", "event-3"));
                    writer.flush().join();
                    print(first.join());
                    print(second.join());
                    print(third.join());
                }
            }
        }
    }

    private static MultiMessage message(String key, String payload) {
        return MultiMessage.of(payload.getBytes(StandardCharsets.UTF_8), key);
    }

    private static void print(MultiWriteResult result) {
        System.out.println("seqNo=" + result.getSeqNo() + ", partition=" + result.getPartitionId()
                + ", offset=" + result.getOffset() + ", confirmation=" + result.getSource());
    }
}
