package tech.ydb.topic.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.v1.TopicServiceGrpc;
import tech.ydb.topic.TopicClient;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BaseMockedTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseMockedTest.class);

    private interface WriteStream extends
            GrpcReadWriteStream<YdbTopic.StreamWriteMessage.FromServer, YdbTopic.StreamWriteMessage.FromClient> {
    }

    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    private final ScheduledFuture<?> emptyFuture = Mockito.mock(ScheduledFuture.class);
    private final WriteStream writeStream = Mockito.mock(WriteStream.class);
    private final SchedulerAssert schedulerHelper = new SchedulerAssert();

    protected final TopicClient client = TopicClient.newClient(transport)
            .setCompressionExecutor(Runnable::run) // Disable compression in separate executors
            .build();

    private volatile MockedWriteStream streamMock = null;

    @Before
    public void beforeEach() {
        streamMock = null;

        Mockito.when(transport.getScheduler()).thenReturn(scheduler);
        Mockito.when(transport.readWriteStreamCall(Mockito.eq(TopicServiceGrpc.getStreamWriteMethod()), Mockito.any()))
                .thenReturn(writeStream);

        // Every writeStream.start updates mockedWriteStream
        Mockito.when(writeStream.start(Mockito.any())).thenAnswer(defaultStreamMockAnswer());

        // Every writeStream.senbNext add message from client to mockedWriteStream.sent list
        Mockito.doAnswer((Answer<Void>) (InvocationOnMock iom) -> {
            streamMock.sent.add(iom.getArgument(0, YdbTopic.StreamWriteMessage.FromClient.class));
            return null;
        }).when(writeStream).sendNext(Mockito.any());

        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
                .thenAnswer((InvocationOnMock iom) -> {
                    logger.debug("mock scheduled task");
                    schedulerHelper.tasks.add(iom.getArgument(0, Runnable.class));
                    return emptyFuture;
                });
    }

    protected MockedWriteStream currentStream() {
        return streamMock;
    }

    protected SchedulerAssert getScheduler() {
        return schedulerHelper;
    }

    protected OngoingStubbing<CompletableFuture<Status>> mockStreams() {
        return Mockito.when(writeStream.start(Mockito.any()));
    }

    protected Answer<CompletableFuture<Status>> defaultStreamMockAnswer() {
        return (InvocationOnMock iom) -> {
            streamMock = new MockedWriteStream(iom.getArgument(0));
            return streamMock.streamFuture;
        };
    }

    protected Answer<CompletableFuture<Status>> errorStreamMockAnswer(StatusCode code) {
        return (iom) -> {
            streamMock = null;
            return CompletableFuture.completedFuture(Status.of(code));
        };
    }

    protected static class SchedulerAssert {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        public SchedulerAssert hasNoTasks() {
            Assert.assertTrue(tasks.isEmpty());
            return this;
        }

        public SchedulerAssert hasTasks(int count) {
            Assert.assertEquals(count, tasks.size());
            return this;
        }

        public SchedulerAssert executeNextTasks(int count) {
            Assert.assertTrue(count <= tasks.size());

            CompletableFuture.runAsync(() -> {
                logger.debug("execute {} scheduled tasks", count);
                for (int idx = 0; idx < count; idx++) {
                    tasks.poll().run();
                }
            }).join();
            return this;
        }
    }

    protected static class MockedWriteStream {
        private final GrpcReadWriteStream.Observer<YdbTopic.StreamWriteMessage.FromServer> observer;
        private final CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        private final List<YdbTopic.StreamWriteMessage.FromClient> sent = new ArrayList<>();
        private volatile int sentIdx = 0;

        public MockedWriteStream(GrpcReadStream.Observer<YdbTopic.StreamWriteMessage.FromServer> observer) {
            this.observer = observer;
        }

        public void complete(Status status) {
            streamFuture.complete(status);
        }

        public void complete(Throwable th) {
            streamFuture.completeExceptionally(th);
        }

        public void hasNoNewMessages() {
            Assert.assertTrue(sentIdx >= sent.size());
        }

        public Checker nextMsg() {
            Assert.assertTrue(sentIdx < sent.size());
            return new Checker(sent.get(sentIdx++));
        }

        public void responseErrorBadRequest() {
            YdbTopic.StreamWriteMessage.FromServer msg = YdbTopic.StreamWriteMessage.FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST)
                    .build();
            observer.onNext(msg);
        }

        public void responseErrorSchemeError() {
            YdbTopic.StreamWriteMessage.FromServer msg = YdbTopic.StreamWriteMessage.FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SCHEME_ERROR)
                    .build();
            observer.onNext(msg);
        }

        public void responseInit(long lastSeqNo) {
            responseInit(lastSeqNo, 123, "mocked", new int[] { 0, 1, 2});
        }

        public void responseInit(long lastSeqNo, long partitionId, String sessionId, int[] codecs) {
            YdbTopic.StreamWriteMessage.FromServer msg = YdbTopic.StreamWriteMessage.FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                            .setLastSeqNo(lastSeqNo)
                            .setPartitionId(partitionId)
                            .setSessionId(sessionId)
                            .setSupportedCodecs(YdbTopic.SupportedCodecs.newBuilder()
                                    .addAllCodecs(IntStream.of(codecs).boxed().collect(Collectors.toList())))
                    ).build();
            observer.onNext(msg);
        }

        public void responseWriteWritten(long firstSeqNo, int messagesCount) {
            List<YdbTopic.StreamWriteMessage.WriteResponse.WriteAck> acks = LongStream
                    .range(firstSeqNo, firstSeqNo + messagesCount)
                    .mapToObj(seqNo -> YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder()
                            .setSeqNo(seqNo)
                            .setWritten(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Written.newBuilder())
                            .build())
                    .collect(Collectors.toList());

            YdbTopic.StreamWriteMessage.FromServer msg = YdbTopic.StreamWriteMessage.FromServer.newBuilder()
                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder().addAllAcks(acks))
                    .build();
            observer.onNext(msg);
        }

        protected class Checker {
            private final YdbTopic.StreamWriteMessage.FromClient msg;

            public Checker(YdbTopic.StreamWriteMessage.FromClient msg) {
                this.msg = msg;
            }

            public Checker isInit() {
                Assert.assertTrue("next msg must be init request", msg.hasInitRequest());
                return this;
            }

            public Checker hasInitPath(String path) {
                Assert.assertEquals("invalid init request path", path, msg.getInitRequest().getPath());
                return this;
            }

            public Checker isWrite() {
                Assert.assertTrue("next msg must be write request", msg.hasWriteRequest());
                return this;
            }

            public Checker hasWrite(int codec, long... seqnums) {
                Assert.assertEquals("invalid write codec", codec, msg.getWriteRequest().getCodec());
                Assert.assertEquals("invalid messages count", seqnums.length, msg.getWriteRequest().getMessagesCount());
                for (int idx = 0; idx < seqnums.length; idx++) {
                    Assert.assertEquals("invalid msg seqNo " + idx, seqnums[idx],
                            msg.getWriteRequest().getMessages(idx).getSeqNo());
                }
                return this;
            }
        }
    }
}
