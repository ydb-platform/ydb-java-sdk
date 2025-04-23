package tech.ydb.coordination;

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
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;
import tech.ydb.proto.coordination.v1.CoordinationServiceGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class CoordinationSessionBaseMockedTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionBaseMockedTest.class);

    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final ScheduledFuture<?> emptyFuture = Mockito.mock(ScheduledFuture.class);
    private final GrpcReadWriteStream<SessionResponse, SessionRequest> writeStream =
            Mockito.mock(GrpcReadWriteStream.class);
    private final SchedulerAssert schedulerHelper = new SchedulerAssert();

    protected final CoordinationClient client = CoordinationClient.newClient(transport);

    private volatile MockedWriteStream streamMock = null;

    @Before
    public void beforeEach() {
        Mockito.when(transport.getScheduler()).thenReturn(scheduler);
        Mockito.when(transport.readWriteStreamCall(Mockito.eq(CoordinationServiceGrpc.getSessionMethod()), Mockito.any()))
                .thenReturn(writeStream); // create mocked stream

        // Every writeStream.start updates mockedWriteStream
        Mockito.when(writeStream.start(Mockito.any()))
                .thenAnswer(defaultStreamMockAnswer());

        // Every writeStream.sendNext add message from client to mockedWriteStream.sent list
        Mockito.doAnswer((Answer<Void>) (InvocationOnMock iom) -> {
            streamMock.sent.add(iom.getArgument(0, SessionRequest.class));
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

    protected Answer<CompletableFuture<Status>> errorStreamMockAnswer(StatusCode code) {
        return (iom) -> {
            streamMock = null;
            return CompletableFuture.completedFuture(Status.of(code));
        };
    }

    protected Answer<CompletableFuture<Status>> defaultStreamMockAnswer() {
        return (InvocationOnMock iom) -> {
            streamMock = new MockedWriteStream(iom.getArgument(0));
            return streamMock.streamFuture;
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

    protected class MockedWriteStream {
        private final GrpcReadWriteStream.Observer<SessionResponse> observer;
        private final CompletableFuture<Status> streamFuture = new CompletableFuture<>();
        private final List<SessionRequest> sent = new ArrayList<>();
        private volatile int sentIdx = 0;

        public MockedWriteStream(GrpcReadWriteStream.Observer<SessionResponse> observer) {
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

        public void responseSemaphoreAlreadyExists() {
            SessionResponse msg = SessionResponse.newBuilder()
                    .setAcquireSemaphoreResult(
                            SessionResponse.AcquireSemaphoreResult.newBuilder().setStatus(
                                    StatusCodesProtos.StatusIds.StatusCode.ALREADY_EXISTS
                            )
                    )
                    .build();
            observer.onNext(msg);
        }

    }

    protected class Checker {
        private final SessionRequest msg;

        public Checker(SessionRequest msg) {
            this.msg = msg;
        }

        public Checker isAcquireSemaphore() {
            Assert.assertTrue("next msg must be acquire semaphore", msg.hasAcquireSemaphore());
            return this;
        }
    }
}
