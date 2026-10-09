package tech.ydb.topic.write.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.metrics.LongCounter;
import tech.ydb.core.metrics.LongMeasurement;
import tech.ydb.core.metrics.Meter;
import tech.ydb.core.metrics.MetricRegistration;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.TopicClient;
import tech.ydb.topic.TopicRpc;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.impl.TopicClientImpl;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

public class WriterMetricsTest {

    @Test
    public void writerCountersCountAcceptedMessagesAndAcknowledgements() throws QueueOverflowException {
        Map<String, Long> counters = new HashMap<>();
        Meter meter = new Meter() {
            @Override
            public LongCounter createCounter(String name, String unit, String description) {
                counters.put(name, 0L);
                return (value, attrs) -> counters.put(name, counters.get(name) + value);
            }
        };
        StreamMock stream = new StreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                writer.send(Message.of(new byte[]{1, 2}));
                writer.send(Message.of(new byte[]{3, 4, 5}));
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(Long.valueOf(2), counters.get("ydb.topic.writer.sending.messages"));
                Assert.assertEquals(Long.valueOf(5), counters.get("ydb.topic.writer.sending.bytes"));
                Assert.assertEquals(Long.valueOf(2), counters.get("ydb.topic.writer.written.messages"));
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
    }

    @Test
    public void writerBufferGaugesTrackBudgetAndLimit() throws QueueOverflowException {
        Map<String, LongSupplier> gauges = new HashMap<>();
        Meter meter = new Meter() {
            @Override
            public MetricRegistration registerLongGauge(String name, String unit, String description,
                    Consumer<LongMeasurement> callback) {
                gauges.put(name, () -> {
                    long[] value = new long[1];
                    callback.accept((observed, attrs) -> value[0] = observed);
                    return value[0];
                });
                return () -> gauges.remove(name);
            }
        };
        String used = "ydb.topic.writer.buffer.used.bytes";
        String limit = "ydb.topic.writer.buffer.limit.bytes";
        StreamMock stream = new StreamMock();
        WriterSettings settings = WriterSettings.newBuilder()
                .setTopicPath("/test/topic").setCodec(Codec.RAW).setMaxSendBufferMemorySize(100)
                .withMeter(meter, "writer").build();
        try (TopicClient client = TopicClientImpl.newClient(mockRpc(stream))
                .setCompressionExecutor(Runnable::run).build()) {
            AsyncWriter writer = client.createAsyncWriter(settings);
            try {
                writer.init();
                stream.sendInitResponse(0);
                Assert.assertEquals(0, gauges.get(used).getAsLong());
                Assert.assertEquals(100, gauges.get(limit).getAsLong());
                writer.send(Message.of(new byte[]{1, 2}));
                writer.send(Message.of(new byte[]{3, 4, 5}));
                Assert.assertEquals(5, gauges.get(used).getAsLong());
                Assert.assertEquals(100, gauges.get(limit).getAsLong());
                stream.sendAckResponse(2, 10);
                Assert.assertEquals(0, gauges.get(used).getAsLong());
            } finally {
                writer.shutdown();
                stream.close(Status.SUCCESS);
            }
        }
        Assert.assertTrue(gauges.isEmpty());
    }

    private static TopicRpc mockRpc(StreamMock stream) {
        TopicRpc rpc = Mockito.mock(TopicRpc.class);
        Mockito.when(rpc.getScheduler()).thenReturn(Mockito.mock(ScheduledExecutorService.class));
        Mockito.when(rpc.writeSession(Mockito.any(String.class))).thenReturn(stream);
        return rpc;
    }

    private static class StreamMock implements GrpcReadWriteStream<FromServer, FromClient> {
        private final CompletableFuture<Status> future = new CompletableFuture<>();
        private GrpcReadStream.Observer<FromServer> observer = null;

        void sendInitResponse(long lastSeqNo) {
            observer.onNext(FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                            .setLastSeqNo(lastSeqNo)
                            .setSessionId("test-session")
                            .build())
                    .build());
        }

        void sendAckResponse(long seqNo, long offset) {
            observer.onNext(FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder()
                            .addAcks(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder()
                                    .setSeqNo(seqNo)
                                    .setWritten(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Written.newBuilder()
                                            .setOffset(offset)
                                            .build())
                                    .build())
                            .build())
                    .build()
            );
        }


        void close(Status status) {
            future.complete(status);
        }

        @Override
        public String authToken() {
            return "token";
        }

        @Override
        public void sendNext(FromClient message) {
        }

        @Override
        public void close() {
        }

        @Override
        public CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> observer) {
            this.observer = observer;
            return future;
        }

        @Override
        public void cancel() {
        }
    }
}
