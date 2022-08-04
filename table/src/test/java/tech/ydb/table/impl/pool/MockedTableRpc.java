package tech.ydb.table.impl.pool;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;

import org.junit.Assert;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedTableRpc extends TableRpcStub {
    private final Clock clock;
    private final Set<String> activeSessions = new HashSet<>();

    private final AtomicInteger sessionCounter = new AtomicInteger();
    
    private final Queue<CreateSession> createSessionQueue = new LinkedBlockingQueue<>();
    private final Queue<ExecuteDataQuery> executeDataQueryQueye = new LinkedBlockingQueue<>();
    private final Queue<KeepAlive> keepAliveQueue = new LinkedBlockingQueue<>();

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
        ExecuteDataQuery task = new ExecuteDataQuery(request, clock.instant()
                .plusMillis(settings.getDeadlineAfter()));
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
        String id = request.getSessionId();

        if (!activeSessions.contains(id)) {
            return CompletableFuture.completedFuture(Status.of(StatusCode.BAD_SESSION));
        }

        activeSessions.remove(id);
        return CompletableFuture.completedFuture(Status.SUCCESS);
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
            future.complete(Result.fail(StatusCode.OVERLOADED));
        }

        public void completeTransportUnavailable() {
            future.complete(Result.fail(StatusCode.TRANSPORT_UNAVAILABLE));
        }
    }

    public class ExecuteDataQuery {
        private final YdbTable.ExecuteDataQueryRequest request;
        private final Instant deadline;
        private final CompletableFuture<Result<YdbTable.ExecuteQueryResult>> future;

        public ExecuteDataQuery(YdbTable.ExecuteDataQueryRequest request, Instant deadline) {
            this.request = request;
            this.deadline = deadline;
            this.future = new CompletableFuture<>();
        }
        
        public void completeSuccess() {
            boolean ok = activeSessions.contains(request.getSessionId());
            
            if (!ok) {
                future.complete(Result.fail(StatusCode.BAD_SESSION));
                return;
            }

            future.complete(Result.success(YdbTable.ExecuteQueryResult.getDefaultInstance()));
        }

        public void completeOverloaded() {
            boolean ok = activeSessions.contains(request.getSessionId());
            
            if (!ok) {
                future.complete(Result.fail(StatusCode.BAD_SESSION));
                return;
            }

            future.complete(Result.fail(StatusCode.OVERLOADED));
        }

        public void completeTransportUnavailable() {
            boolean ok = activeSessions.contains(request.getSessionId());
            
            if (!ok) {
                future.complete(Result.fail(StatusCode.BAD_SESSION));
                return;
            }

            future.complete(Result.fail(StatusCode.TRANSPORT_UNAVAILABLE));
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
            boolean ok = activeSessions.contains(request.getSessionId());
            
            if (!ok) {
                future.complete(Result.fail(StatusCode.BAD_SESSION));
                return;
            }
            
            YdbTable.KeepAliveResult result = YdbTable.KeepAliveResult.newBuilder()
                    .setSessionStatus(YdbTable.KeepAliveResult.SessionStatus.SESSION_STATUS_READY)
                    .build();
            future.complete(Result.success(result));
        }

        public void completeBusy() {
            boolean ok = activeSessions.contains(request.getSessionId());
            
            if (!ok) {
                future.complete(Result.fail(StatusCode.BAD_SESSION));
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
            return this;
        }

        public Checker sessionRequests(int count) {
            Assert.assertEquals("MockTable check session requests", count, createSessionQueue.size());
            return this;
        }

        public Checker keepAlives(int count) {
            Assert.assertEquals("MockTable check keep alives", count, keepAliveQueue.size());
            return this;
        }

        public Checker executeDataRequests(int count) {
            Assert.assertEquals("MockTable check session requests", count, executeDataQueryQueye.size());
            return this;
        }
    }
}
