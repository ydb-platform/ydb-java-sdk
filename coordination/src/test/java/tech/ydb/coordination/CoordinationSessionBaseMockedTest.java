package tech.ydb.coordination;

import java.time.Duration;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class CoordinationSessionBaseMockedTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationSessionBaseMockedTest.class);

    private final CoordinationClient client = Mockito.mock(CoordinationClient.class);
    private final CoordinationSession coordinationSession = Mockito.mock(CoordinationSession.class);
    private final SessionMock sessionMock = new SessionMock();
    private final SessionStateAssert sessionStateAssert = new SessionStateAssert();

    @Before
    public void beforeEach() {
        when(client.createSession(any())).thenReturn(coordinationSession);

        when(coordinationSession.connect())
                .thenReturn(
                        CompletableFuture.completedFuture(
                                Status.of(StatusCode.TRANSPORT_UNAVAILABLE)
                        )
                );

        doAnswer((InvocationOnMock iom) -> {
            Consumer<CoordinationSession.State> consumer = iom.getArgument(0, Consumer.class);
            logger.debug("Add session mock listener={}", consumer);
            sessionMock.addListener(consumer);
            return null;
        }).when(coordinationSession).addStateListener(any());

        doAnswer((InvocationOnMock iom) -> {
            Consumer<CoordinationSession.State> consumer = iom.getArgument(0, Consumer.class);
            logger.debug("Remove session mock listener={}", consumer);
            sessionMock.removeListener(consumer);
            return null;
        }).when(coordinationSession).removeStateListener(any());

        doAnswer((InvocationOnMock iom) -> {
            logger.debug("Get mock state={}", sessionMock.state);
            return sessionMock.state;
        }).when(coordinationSession).getState();
    }

    public CoordinationSession getCoordinationSession() {
        return coordinationSession;
    }

    public SessionStateAssert getSessionStateAssert() {
        return sessionStateAssert;
    }

    protected Answer<CompletableFuture<Status>> successConnect() {
        return (InvocationOnMock iom) -> {
            logger.debug("Successful session connect");
            return CompletableFuture.completedFuture(
                    Status.SUCCESS
            );
        };
    }

    protected Answer<CompletableFuture<Status>> failedConnect(StatusCode code) {
        return (InvocationOnMock iom) -> {
            logger.debug("Failed session connect, code={}", code);
            return CompletableFuture.completedFuture(
                    Status.of(code)
            );
        };
    }

    protected Answer<CompletableFuture<Result<SemaphoreLease>>> successAcquire(SemaphoreLease lease) {
        return (InvocationOnMock iom) -> {
            logger.debug("Success semaphore acquire {}", lease.getSemaphoreName());
            return CompletableFuture.completedFuture(
                    Result.success(lease)
            );
        };
    }

    protected Answer<CompletableFuture<Result<SemaphoreLease>>> statusAcquire(StatusCode statusCode) {
        return (InvocationOnMock iom) -> {
            logger.debug("Response semaphore acquire with code: {}", statusCode);
            return CompletableFuture.completedFuture(
                    Result.fail(Status.of(statusCode))
            );
        };
    }

    protected Answer<CompletableFuture<Result<SemaphoreLease>>> timeoutAcquire() {
        return (InvocationOnMock iom) -> {
            logger.debug("Timeout semaphore acquire");
            return CompletableFuture.completedFuture(
                    Result.fail(Status.of(StatusCode.TIMEOUT))
            );
        };
    }

    protected Answer<CompletableFuture<Result<SemaphoreLease>>> timeoutAcquire(Duration blockDuration) {
        return (InvocationOnMock iom) -> {
            logger.debug("Block acquire duration={}", blockDuration);
            Thread.sleep(blockDuration.toMillis());
            return CompletableFuture.completedFuture(
                    Result.fail(Status.of(StatusCode.TIMEOUT))
            );
        };
    }

    protected Answer<CompletableFuture<Result<SemaphoreLease>>> lostAcquire() {
        return (InvocationOnMock iom) -> {
            logger.debug("Lost session during");
            sessionMock.lost();
            return CompletableFuture.completedFuture(
                    Result.fail(Status.of(StatusCode.TIMEOUT))
            );
        };
    }

    protected LeaseMock lease(String semaphoreName) {
        return new LeaseMock(
                sessionMock,
                semaphoreName,
                coordinationSession
        );
    }

    protected CoordinationClient getClient() {
        return client;
    }

    protected SessionMock getSessionMock() {
        return sessionMock;
    }

    protected class SessionStateAssert implements Consumer<CoordinationSession.State> {
        private final Queue<CoordinationSession.State> queue = new ConcurrentLinkedQueue<>();

        public SessionStateAssert next(CoordinationSession.State state) {
            queue.add(state);
            return this;
        }

        @Override
        public void accept(CoordinationSession.State state) {
            logger.debug("Next state: {}", state);
            Assert.assertFalse(queue.isEmpty());
            CoordinationSession.State lastState = queue.poll();
            Assert.assertEquals(state, lastState);
        }

        public void finished() {
            Assert.assertTrue(queue.isEmpty());
        }
    }

    protected class LeaseMock implements SemaphoreLease {
        private final SessionMock sessionMock;
        private final String name;
        private final CoordinationSession session;
        private boolean released = false;
        private CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

        public LeaseMock(SessionMock sessionMock, String name, CoordinationSession session) {
            this.sessionMock = sessionMock;
            this.name = name;
            this.session = session;
        }

        @Override
        public CoordinationSession getSession() {
            return session;
        }

        @Override
        public String getSemaphoreName() {
            return name;
        }

        @Override
        public CompletableFuture<Void> release() {
            released = true;
            return result;
        }

        public LeaseMock failed(Exception exception) {
            result = new CompletableFuture<>();
            result.completeExceptionally(exception);
            return this;
        }

        public void assertReleased() {
            Assert.assertTrue(released);
        }
    }

    protected class SessionMock {
        private final Set<Consumer<CoordinationSession.State>> listeners = new HashSet<>();

        private CoordinationSession.State state = CoordinationSession.State.INITIAL;

        public SessionMock() {
        }

        private void addListener(Consumer<CoordinationSession.State> consumer) {
            listeners.add(consumer);
        }

        private void removeListener(Consumer<CoordinationSession.State> consumer) {
            listeners.remove(consumer);
        }

        public OngoingStubbing<CompletableFuture<Status>> connect() {
            return when(coordinationSession.connect());
        }

        public OngoingStubbing<CompletableFuture<Result<SemaphoreLease>>> acquireEphemeralSemaphore() {
            return when(coordinationSession.acquireEphemeralSemaphore(anyString(), anyBoolean(), any(), any()));
        }

        public void connecting() {
            changeState(CoordinationSession.State.CONNECTING);
        }

        public void connected() {
            changeState(CoordinationSession.State.CONNECTED);
        }

        public void lost() {
            changeState(CoordinationSession.State.LOST);
        }

        public void closed() {
            changeState(CoordinationSession.State.CLOSED);
        }

        private void changeState(CoordinationSession.State newState) {
            state = newState;
            listeners.forEach(it -> it.accept(newState));
        }
    }

}
