package tech.ydb.table.impl.pool;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class MockedTableRpc extends TableRpcStub {
    private final Clock clock;
    private final Set<String> activeSessions = new HashSet<>();

    private final AtomicInteger sessionCounter = new AtomicInteger();
    private final AtomicInteger operationCounter = new AtomicInteger();
    
    private final Queue<CreateSession> createSessionQueue = new LinkedBlockingQueue<>();
    private final Queue<KeepAlive> keepAliveQueue = new LinkedBlockingQueue<>();

    public MockedTableRpc(Clock clock) {
        this.clock = clock;
    }
    
    public CreateSession pollCreateSession() {
        Assert.assertFalse("Mocked server has session create request", createSessionQueue.isEmpty());
        return createSessionQueue.poll();
    }

    public KeepAlive pollKeepAlive() {
        Assert.assertFalse("Mocked server has keep alive request", keepAliveQueue.isEmpty());
        return keepAliveQueue.poll();
    }
    
    @Override
    public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
        YdbTable.CreateSessionRequest request, long deadlineAfter) {
        CreateSession task = new CreateSession(request, clock.instant().plusMillis(deadlineAfter));
        createSessionQueue.offer(task);
        return task.future;
    }

    @Override
    public CompletableFuture<Result<YdbTable.KeepAliveResponse>> keepAlive(
            YdbTable.KeepAliveRequest request, long deadlineAfter) {
        KeepAlive task = new KeepAlive(request, clock.instant().plusMillis(deadlineAfter));
        keepAliveQueue.offer(task);
        return task.future;
    }

    @Override
    public CompletableFuture<Result<YdbTable.DeleteSessionResponse>> deleteSession(
            YdbTable.DeleteSessionRequest request, long deadlineAfter) {
        String id = request.getSessionId();
        YdbTable.DeleteSessionResponse response = YdbTable.DeleteSessionResponse
                .newBuilder()
                .setOperation(successOperation())
                .build();

        if (!activeSessions.contains(id)) {
            return CompletableFuture.completedFuture(Result.fail(StatusCode.BAD_SESSION));
        }

        activeSessions.remove(id);
        return CompletableFuture.completedFuture(Result.success(response));
    }

    private <M extends Message> OperationProtos.Operation resultOperation(M message) {
        return OperationProtos.Operation.newBuilder()
            .setId(generateNextOperation())
            .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
            .setReady(true)
            .setResult(Any.pack(message))
            .build();
    }

    private OperationProtos.Operation successOperation() {
        return OperationProtos.Operation.newBuilder()
            .setId(generateNextOperation())
            .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
            .setReady(true)
            .build();
    }
    
    private String generateNextSession() {
        return "session_" + sessionCounter.incrementAndGet();
    }

    private String generateNextOperation() {
        return "operation_" + operationCounter.incrementAndGet();
    }
    
    public class CreateSession {
        private final YdbTable.CreateSessionRequest request;
        private final Instant deadline;
        private final CompletableFuture<Result<YdbTable.CreateSessionResponse>> future;

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
            YdbTable.CreateSessionResponse response = YdbTable.CreateSessionResponse.newBuilder()
                .setOperation(resultOperation(result))
                .build();

            activeSessions.add(sessionID);
            future.complete(Result.success(response));
        }

        public void completeOverloaded() {
            future.complete(Result.fail(StatusCode.OVERLOADED));
        }
    }

    public class KeepAlive {
        private final YdbTable.KeepAliveRequest request;
        private final Instant deadline;
        private final CompletableFuture<Result<YdbTable.KeepAliveResponse>> future;

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
            YdbTable.KeepAliveResponse response = YdbTable.KeepAliveResponse.newBuilder()
                .setOperation(resultOperation(result))
                .build();
            future.complete(Result.success(response));
        }
    }
}
