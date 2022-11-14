package tech.ydb.table.impl.pool;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.Metadata;
import org.junit.Assert;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedTableRpc extends TableRpcStub {
    private final static Status BAD_SESSION = Status.of(StatusCode.BAD_SESSION, null);
    private final static Status OVERLOADED = Status.of(StatusCode.OVERLOADED, null);
    private final static Status TRANSPORT_UNAVAILABLE = Status.of(StatusCode.TRANSPORT_UNAVAILABLE, null);

    private final Clock clock;
    private final Set<String> activeSessions = new HashSet<>();

    private final AtomicInteger sessionCounter = new AtomicInteger();

    private final Queue<CreateSession> createSessionQueue = new LinkedBlockingQueue<>();
    private final Queue<ExecuteDataQuery> executeDataQueryQueye = new LinkedBlockingQueue<>();
    private final Queue<KeepAlive> keepAliveQueue = new LinkedBlockingQueue<>();
    private final Queue<DeleteSession> deleteSessionQueue = new LinkedBlockingQueue<>();

    public MockedTableRpc(Clock clock) {
        this.clock = clock;
    }

    public Checker check() {
        return new Checker();
    }

    public CreateSession nextCreateSession() {
        Assert.assertFalse("Mocked server has session create request", createSessionQueue.isEmpty());
        return createSessionQueue.poll();
    }

    public ExecuteDataQuery nextExecuteDataQuery() {
        Assert.assertFalse("Mocked server has execute data query request", executeDataQueryQueye.isEmpty());
        return executeDataQueryQueye.poll();
    }

    public KeepAlive nextKeepAlive() {
        Assert.assertFalse("Mocked server has keep alive request", keepAliveQueue.isEmpty());
        return keepAliveQueue.poll();
    }

    public DeleteSession nextDeleteSession() {
        Assert.assertFalse("Mocked server has delete session request", deleteSessionQueue.isEmpty());
        return deleteSessionQueue.poll();
    }

    public void completeSessionDeleteRequests() {
        DeleteSession next = deleteSessionQueue.poll();
        while (next != null) {
            next.completeSuccess();
            next = deleteSessionQueue.poll();
        }
    }

    @Override
    public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
        YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
        CreateSession task = new CreateSession(request, clock.instant()
                .plusMillis(settings.getDeadlineAfter()));
        createSessionQueue.offer(task);
        return task.future;
    }

    @Override
    public CompletableFuture<Result<YdbTable.ExecuteQueryResult>> executeDataQuery(
        YdbTable.ExecuteDataQueryRequest request, GrpcRequestSettings settings) {
        ExecuteDataQuery task = new ExecuteDataQuery(request, settings);
        executeDataQueryQueye.offer(task);
        return task.future;
    }

    @Override
    public CompletableFuture<Result<YdbTable.KeepAliveResult>> keepAlive(
            YdbTable.KeepAliveRequest request, GrpcRequestSettings settings) {
        KeepAlive task = new KeepAlive(request, clock.instant()
                .plusMillis(settings.getDeadlineAfter()));
        keepAliveQueue.offer(task);
        return task.future;
    }

    @Override
    public CompletableFuture<Status> deleteSession(
            YdbTable.DeleteSessionRequest request, GrpcRequestSettings settings) {
        DeleteSession task = new DeleteSession(request, clock.instant().plusMillis(settings.getDeadlineAfter()));
        deleteSessionQueue.offer(task);
        return task.future;
    }

    private String generateNextSession() {
        return "session_" + sessionCounter.incrementAndGet();
    }

    public class CreateSession {
        private final YdbTable.CreateSessionRequest request;
        private final Instant deadline;
        private final CompletableFuture<Result<YdbTable.CreateSessionResult>> future;

        public CreateSession(YdbTable.CreateSessionRequest request, Instant deadline) {
            this.request = request;
            this.deadline = deadline;
            this.future = new CompletableFuture<>();
        }

        public void completeSuccess() {
            String sessionID = generateNextSession();
            YdbTable.CreateSessionResult result = YdbTable.CreateSessionResult.newBuilder()
                .setSessionId(sessionID)
                .build();

            activeSessions.add(sessionID);
            future.complete(Result.success(result));
        }

        public void completeOverloaded() {
            future.complete(Result.fail(OVERLOADED));
        }

        public void completeTransportUnavailable() {
            future.complete(Result.fail(TRANSPORT_UNAVAILABLE));
        }

        public void completeRuntimeException() {
            future.completeExceptionally(new RuntimeException("Can't create session"));
        }
    }

    public class DeleteSession {
        private final YdbTable.DeleteSessionRequest request;
        private final Instant deadline;
        private final CompletableFuture<Status> future;

        public DeleteSession(YdbTable.DeleteSessionRequest request, Instant deadline) {
            this.request = request;
            this.deadline = deadline;
            this.future = new CompletableFuture<>();
        }

        public void completeSuccess() {
            if (!activeSessions.remove(request.getSessionId())) {
                future.complete(BAD_SESSION);
                return;
            }
            future.complete(Status.SUCCESS);
        }

        public void completeTransportUnavailable() {
            if (!activeSessions.remove(request.getSessionId())) {
                future.complete(BAD_SESSION);
                return;
            }
            future.complete(TRANSPORT_UNAVAILABLE);
        }

        public void completeRuntimeException() {
            if (!activeSessions.remove(request.getSessionId())) {
                future.complete(BAD_SESSION);
                return;
            }
            future.completeExceptionally(new RuntimeException("Can't delete session"));
        }
    }

    public class ExecuteDataQuery {
        private final YdbTable.ExecuteDataQueryRequest request;
        private final GrpcRequestSettings settings;
        private final CompletableFuture<Result<YdbTable.ExecuteQueryResult>> future;

        public ExecuteDataQuery(YdbTable.ExecuteDataQueryRequest request, GrpcRequestSettings settings) {
            this.request = request;
            this.settings = settings;
            this.future = new CompletableFuture<>();
        }

        private void completeSuccess(boolean shutdownHook) {
            if (!activeSessions.contains(request.getSessionId())) {
                future.complete(Result.fail(BAD_SESSION));
                return;
            }

            if (settings.getTrailersHandler() != null) {
                Metadata headers = new Metadata();
                if (shutdownHook) {
                    headers.put(YdbHeaders.YDB_SERVER_HINTS, "session-close");
                }
                settings.getTrailersHandler().accept(headers);
            }

            future.complete(Result.success(YdbTable.ExecuteQueryResult.getDefaultInstance()));
        }

        public void completeSuccess() {
            completeSuccess(false);
        }

        public void completeSuccessWithShutdownHook() {
            completeSuccess(true);
        }

        public void completeOverloaded() {
            if (!activeSessions.contains(request.getSessionId())) {
                future.complete(Result.fail(BAD_SESSION));
                return;
            }

            future.complete(Result.fail(OVERLOADED));
        }

        public void completeTransportUnavailable() {
            if (!activeSessions.contains(request.getSessionId())) {
                future.complete(Result.fail(BAD_SESSION));
                return;
            }

            future.complete(Result.fail(TRANSPORT_UNAVAILABLE));
        }
    }

    public class KeepAlive {
        private final YdbTable.KeepAliveRequest request;
        private final Instant deadline;
        private final CompletableFuture<Result<YdbTable.KeepAliveResult>> future;

        public KeepAlive(YdbTable.KeepAliveRequest request, Instant deadline) {
            this.request = request;
            this.deadline = deadline;
            this.future = new CompletableFuture<>();
        }

        public void completeReady() {
            if (!activeSessions.contains(request.getSessionId())) {
                future.complete(Result.fail(BAD_SESSION));
                return;
            }

            YdbTable.KeepAliveResult result = YdbTable.KeepAliveResult.newBuilder()
                    .setSessionStatus(YdbTable.KeepAliveResult.SessionStatus.SESSION_STATUS_READY)
                    .build();
            future.complete(Result.success(result));
        }

        public void completeBusy() {
            if (!activeSessions.contains(request.getSessionId())) {
                future.complete(Result.fail(BAD_SESSION));
                return;
            }

            YdbTable.KeepAliveResult result = YdbTable.KeepAliveResult.newBuilder()
                    .setSessionStatus(YdbTable.KeepAliveResult.SessionStatus.SESSION_STATUS_BUSY)
                    .build();
            future.complete(Result.success(result));
        }
    }


    public class Checker {
        public Checker hasNoSessions() {
            Assert.assertTrue("MockTable has no sessions", activeSessions.isEmpty());
            return this;
        }

        public Checker hasNoKeepAlive() {
            Assert.assertTrue("MockTable has no keep alives", keepAliveQueue.isEmpty());
            return this;
        }

        public Checker hasNoPendingRequests() {
            Assert.assertTrue("MockTable has no session requests", createSessionQueue.isEmpty());
            Assert.assertTrue("MockTable has no keepalive requests", keepAliveQueue.isEmpty());
            Assert.assertTrue("MockTable has no exec data query requests", executeDataQueryQueye.isEmpty());
            Assert.assertTrue("MockTable has no session delete responses", deleteSessionQueue.isEmpty());
            return this;
        }

        public Checker sessionRequests(int count) {
            Assert.assertEquals("MockTable check session requests", count, createSessionQueue.size());
            return this;
        }

        public Checker deleteSessionRequests(int count) {
            Assert.assertEquals("MockTable check delete session requests", count, deleteSessionQueue.size());
            return this;
        }

        public Checker keepAlives(int count) {
            Assert.assertEquals("MockTable check keep alives", count, keepAliveQueue.size());
            return this;
        }

        public Checker executeDataRequests(int count) {
            Assert.assertEquals("MockTable check execute data requests", count, executeDataQueryQueye.size());
            return this;
        }
    }
}
