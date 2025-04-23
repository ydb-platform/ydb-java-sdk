package tech.ydb.coordination;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import java.util.concurrent.*;

public class CoordinationSessionBaseMockedTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionBaseMockedTest.class);

    private final ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
    private final GrpcTransport transport = Mockito.mock(GrpcTransport.class);
    private final ScheduledFuture<?> emptyFuture = Mockito.mock(ScheduledFuture.class);
    private final SchedulerAssert schedulerHelper = new SchedulerAssert();

    protected final CoordinationClient client = CoordinationClient.newClient(transport);

    @Before
    public void beforeEach() {
        Mockito.when(transport.getScheduler()).thenReturn(scheduler);

        Mockito.when(scheduler.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
                .thenAnswer((InvocationOnMock iom) -> {
                    logger.debug("mock scheduled task");
                    schedulerHelper.tasks.add(iom.getArgument(0, Runnable.class));
                    return emptyFuture;
                });
    }

    protected SchedulerAssert getScheduler() {
        return schedulerHelper;
    }

    protected StreamMock mockStream() {
        StreamMock streamMock = new StreamMock();

        GrpcReadWriteStream<SessionResponse, SessionRequest> readWriteStream = Mockito.mock(GrpcReadWriteStream.class);

        Mockito.when(readWriteStream.start(Mockito.any())).thenAnswer(
                (InvocationOnMock iom) -> {
                    streamMock.setObserver(iom.getArgument(0));
                    return streamMock.streamFuture;
                }
        ).thenThrow(new RuntimeException("Unexpected second start call"));

        Mockito.doAnswer((Answer<Void>) (InvocationOnMock iom) -> {
            streamMock.sent.add(iom.getArgument(0, SessionRequest.class));
            return null;
        }).when(readWriteStream).sendNext(Mockito.any());

        Mockito.when(transport.readWriteStreamCall(Mockito.eq(CoordinationServiceGrpc.getSessionMethod()), Mockito.any()))
                .thenReturn(readWriteStream);
        return streamMock;
    }

    protected static class SchedulerAssert implements Executor {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

        @Override
        public void execute(@NotNull Runnable command) {
            logger.debug("scheduling command: " + command);
            tasks.add(command);
        }

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

    protected class StreamMock {
        private final CompletableFuture<Status> streamFuture;
        private final List<SessionRequest> sent = new ArrayList<>();
        private volatile int sentIdx = 0;

        private volatile GrpcReadWriteStream.Observer<SessionResponse> observer = null;

        public StreamMock() {
            streamFuture = new CompletableFuture<>();
        }

        public void setObserver(GrpcReadWriteStream.Observer<SessionResponse> observer) {
            this.observer = observer;
        }

        public void complete(StatusCode statusCode) {
            streamFuture.complete(Status.of(statusCode));
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
            response(msg);
        }

        public void responseSessionStarted(long sessionId) {
            SessionResponse msg = SessionResponse.newBuilder()
                    .setSessionStarted(
                            SessionResponse.SessionStarted.newBuilder()
                                    .setSessionId(sessionId)
                                    .build()
                    )
                    .build();
            response(msg);
        }

        public void responseAcquiredSuccessfully(long requestId) {
            SessionResponse msg = SessionResponse.newBuilder()
                    .setAcquireSemaphoreResult(
                            SessionResponse.AcquireSemaphoreResult.newBuilder()
                                    .setReqId(requestId)
                                    .setAcquired(true)
                                    .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                    )
                    .build();
            response(msg);
        }

        private void response(SessionResponse msg) {
            Assert.assertNotNull(observer);
            observer.onNext(msg);
        }

    }

    protected class Checker {
        private final SessionRequest msg;

        public Checker(SessionRequest msg) {
            this.msg = msg;
        }

        public SessionRequest get() {
            return msg;
        }

        public Checker isAcquireSemaphore() {
            Assert.assertTrue("next msg must be acquire semaphore", msg.hasAcquireSemaphore());
            return this;
        }

        public Checker isEphemeralSemaphore() {
            Assert.assertTrue("next msg must be acquire ephemeral semaphore", msg.getAcquireSemaphore().getEphemeral());
            return this;
        }

        public Checker hasSemaphoreName(String semaphoreName) {
            Assert.assertEquals("invalid semaphore name", semaphoreName, msg.getAcquireSemaphore().getName());
            return this;
        }

        public Checker isSessionStart() {
            Assert.assertTrue("next msg must be session start", msg.hasSessionStart());
            return this;
        }

        public Checker hasPath(String coordinationNodePath) {
            Assert.assertEquals("invalid coordination node path", coordinationNodePath, msg.getSessionStart().getPath());
            return this;
        }
    }
}
