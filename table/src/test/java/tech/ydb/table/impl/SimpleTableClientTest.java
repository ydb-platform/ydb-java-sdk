package tech.ydb.table.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import java.time.Duration;
import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.rpc.TableRpc;
import org.junit.Assert;
import org.junit.Test;
import tech.ydb.table.SessionSupplier;


/**
 * @author Sergey Polovko
 */
public class SimpleTableClientTest {
    public static <M extends Message> OperationProtos.Operation resultOperation(M message) {
        return OperationProtos.Operation.newBuilder()
            .setId("fake_id")
            .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
            .setReady(true)
            .setResult(Any.pack(message))
            .build();
    }

    public static OperationProtos.Operation successOperation() {
        return OperationProtos.Operation.newBuilder()
            .setId("fake_id")
            .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
            .setReady(true)
            .build();
    }

    @Test
    public void createSessionAndRelease() throws InterruptedException, ExecutionException {
        Set<String> sessionIDs = new HashSet<>();

        TableRpc fakeRpc = new TableRpcStub() {
            private int counter = 0;

            @Override
            public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
                YdbTable.CreateSessionRequest request, long deadlineAfter) {

                counter += 1;
                String id = "session " + counter;
                YdbTable.CreateSessionResult result = YdbTable.CreateSessionResult.newBuilder()
                    .setSessionId(id)
                    .build();
                YdbTable.CreateSessionResponse response = YdbTable.CreateSessionResponse.newBuilder()
                    .setOperation(resultOperation(result))
                    .build();

                sessionIDs.add(id);
                return CompletableFuture.completedFuture(Result.success(response));
            }

            @Override
            public CompletableFuture<Result<YdbTable.DeleteSessionResponse>> deleteSession(
                    YdbTable.DeleteSessionRequest request, long deadlineAfter) {
                String id = request.getSessionId();
                YdbTable.DeleteSessionResponse response = YdbTable.DeleteSessionResponse
                        .newBuilder()
                        .setOperation(successOperation())
                        .build();

                if (sessionIDs.contains(id)) {
                    sessionIDs.remove(id);
                    return CompletableFuture.completedFuture(Result.success(response));
                } else {
                    return CompletableFuture.completedFuture(Result.fail(StatusCode.BAD_SESSION));
                }
            }
        };

        SessionSupplier client = SimpleTableClient.newClient(fakeRpc).build();
        // Test TableClient interface
        client.createSession(Duration.ZERO).join().expect("cannot create session");
        // Server has 1 session
        Assert.assertEquals(1, sessionIDs.size());

        // Create second session
        try (Session session2 = client.createSession(Duration.ZERO).join().expect("cannot create session")) {
            // Server has 2 session
            Assert.assertEquals(2, sessionIDs.size());
            session2.getId();
        }
        // But server still has 1 session
        Assert.assertEquals(1, sessionIDs.size());
    }

    @Test
    public void unavailableSessions() {
        TableRpc fakeRpc = new TableRpcStub() {
            @Override
            public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
                YdbTable.CreateSessionRequest request, long deadlineAfter) {
                return CompletableFuture.completedFuture(Result.fail(StatusCode.TRANSPORT_UNAVAILABLE));
            }
        };

        SessionSupplier client = SimpleTableClient.newClient(fakeRpc)
                .keepQueryText(true)
                .build();

        // Test TableClient interface
        Result<Session> sessionResult = client.createSession(Duration.ZERO).join();
        Assert.assertFalse(sessionResult.isSuccess());
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getCode());

        // Test SessionSupplier interface
        sessionResult = client.createSession(Duration.ZERO).join();
        Assert.assertFalse(sessionResult.isSuccess());
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getCode());
    }
}
