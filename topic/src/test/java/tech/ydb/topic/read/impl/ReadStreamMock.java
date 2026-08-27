package tech.ydb.topic.read.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import org.junit.Assert;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
public class ReadStreamMock implements GrpcReadWriteStream<FromServer, FromClient> {
    private static final CodecRegistry REGISTRY = new CodecRegistry();

    private final CompletableFuture<Status> future = new CompletableFuture<>();
    private final Deque<FromClient> messages = new ArrayDeque<>();
    private final AtomicInteger partCounter = new AtomicInteger();
    private Observer<FromServer> observer = null;
    private boolean isClosed = false;
    private boolean isCanceled = false;

    @Override
    public String authToken() {
        return "token";
    }

    @Override
    public void sendNext(FromClient message) {
        messages.add(message);
    }

    @Override
    public void close() {
        this.isClosed = true;
    }

    @Override
    public CompletableFuture<Status> start(Observer<FromServer> observer) {
        this.observer = observer;
        return future;
    }

    @Override
    public void cancel() {
        this.isCanceled = true;
    }

    public void closeStream(Status status) {
        future.complete(status);
    }

    public void responseInit(String sessionId) {
        FromServer msg = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(YdbTopic.StreamReadMessage.InitResponse.newBuilder()
                        .setSessionId(sessionId)
                        .build())
                .build();
        observer.onNext(msg);
    }

    public void responseStartPartition(String topicPath, long partitionID) {
        FromServer msg = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setStartPartitionSessionRequest(YdbTopic.StreamReadMessage.StartPartitionSessionRequest.newBuilder()
                        .setPartitionSession(YdbTopic.StreamReadMessage.PartitionSession.newBuilder()
                                .setPath(topicPath)
                                .setPartitionId(partitionID)
                                .setPartitionSessionId(partCounter.incrementAndGet())
                                .build())
                        .build())
                .build();
        observer.onNext(msg);
    }

    public void responseStopPartition(long psid, boolean graceful) {
        FromServer msg = FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setStopPartitionSessionRequest(YdbTopic.StreamReadMessage.StopPartitionSessionRequest.newBuilder()
                        .setPartitionSessionId(psid)
                        .setGraceful(graceful)
                        .build())
                .build();
        observer.onNext(msg);
    }

    public DataResponse responseData(long bytesSize) {
        return new DataResponse(bytesSize);
    }

    public void assertSentMessagesCount(int expectedCount) {
        Assert.assertEquals("Read stream sent messages count", expectedCount, messages.size());
    }

    public void assertIsClosed() {
        Assert.assertTrue("Read stream is closed", isClosed);
    }

    public void assertIsCancelled() {
        Assert.assertTrue("Read stream is cancelled", isCanceled);
    }

    public MessageAssert assertLastMessage() {
        return new MessageAssert(messages.getLast());
    }

    public class DataResponse {
        private final YdbTopic.StreamReadMessage.ReadResponse.Builder data;

        public DataResponse(long bytesSize) {
            this.data = YdbTopic.StreamReadMessage.ReadResponse.newBuilder();
            data.setBytesSize(bytesSize);
        }

        public void send() {
            FromServer msg = FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setReadResponse(data.build())
                    .build();
            observer.onNext(msg);
        }

        public Partition partition(long psid, long offset) {
            return new Partition(psid, offset);
        }

        public class Partition {
            private final YdbTopic.StreamReadMessage.ReadResponse.PartitionData.Builder part;
            private final AtomicLong offset;

            public Partition(long psid, long firstOffset) {
                this.part = YdbTopic.StreamReadMessage.ReadResponse.PartitionData.newBuilder();
                this.part.setPartitionSessionId(psid);
                this.offset = new AtomicLong(firstOffset);
            }

            public Partition batch(int codec, byte[]... messages) {
                YdbTopic.StreamReadMessage.ReadResponse.Batch.Builder batch = YdbTopic.StreamReadMessage.ReadResponse
                        .Batch.newBuilder().setCodec(codec);
                for (byte[] msg: messages) {
                    batch.addMessageData(YdbTopic.StreamReadMessage.ReadResponse.MessageData.newBuilder()
                            .setUncompressedSize(msg.length)
                            .setData(encode(codec, msg))
                            .setOffset(offset.incrementAndGet())
                            .build());
                }
                part.addBatches(batch.build());
                return this;
            }

            public DataResponse and() {
                data.addPartitionData(part.build());
                return DataResponse.this;
            }
        }
    }

    public static class MessageAssert {
        private final FromClient msg;

        public MessageAssert(FromClient msg) {
            this.msg = msg;
        }

        public MessageAssert isInitRequest(String consumerName, String... topicPaths) {
            Assert.assertTrue("Msg is not init request", msg.hasInitRequest());
            Assert.assertFalse("Auto partition is disabled", msg.getInitRequest().getAutoPartitioningSupport());
            Assert.assertEquals("Wrong consumer in init request", consumerName, msg.getInitRequest().getConsumer());

            Set<String> topics = msg.getInitRequest().getTopicsReadSettingsList().stream()
                    .map(YdbTopic.StreamReadMessage.InitRequest.TopicReadSettings::getPath)
                    .collect(Collectors.toSet());
            for (String topic: topicPaths) {
                Assert.assertTrue("Topic " + topic + " is not sent in init request", topics.contains(topic));
            }
            return this;
        }

        public MessageAssert isReadRequest(long bytesSize) {
            Assert.assertTrue("Msg is not read request", msg.hasReadRequest());
            Assert.assertEquals("Read request has incorrect size", bytesSize, msg.getReadRequest().getBytesSize());
            return this;
        }

        public MessageAssert isStartPartition(long psid) {
            Assert.assertTrue("Msg is not start partition response", msg.hasStartPartitionSessionResponse());
            YdbTopic.StreamReadMessage.StartPartitionSessionResponse resp = msg.getStartPartitionSessionResponse();
            Assert.assertEquals("Start partition has incorrect id", psid, resp.getPartitionSessionId());
            return this;
        }

        public MessageAssert isStopPartition(long psid) {
            Assert.assertTrue("Msg is not stop partition response", msg.hasStopPartitionSessionResponse());
            YdbTopic.StreamReadMessage.StopPartitionSessionResponse resp = msg.getStopPartitionSessionResponse();
            Assert.assertEquals("Stop partition has incorrect id", psid, resp.getPartitionSessionId());
            return this;
        }
    }

    private static ByteString encode(int code, byte[] data) {
        Codec codec = REGISTRY.getCodec(code);
        Assert.assertNotNull("Invalid codec code", codec);

        try (ByteString.Output encoded = ByteString.newOutput()) {
            try (OutputStream os = codec.encode(encoded)) {
                os.write(data, 0, data.length);
            }
            return encoded.toByteString();
        } catch (IOException ex) {
            throw new AssertionError("cannot encode message", ex);
        }
    }
}
