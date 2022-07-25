package tech.ydb.table.impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import tech.ydb.OperationProtos;
import tech.ydb.StatusCodesProtos;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.rpc.TableRpc;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class TableClientImplTest {
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
                YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {

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
                    YdbTable.DeleteSessionRequest request, GrpcRequestSettings settings) {
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

        try (TableClient client = TableClient.newClient(fakeRpc).build()) {
            // Test TableClient interface - without session pool
            Session session1 = client.createSession().join().expect("cannot create session");
            // Server has 1 session
            Assert.assertEquals(1, sessionIDs.size());
            // Session1 not used by pool - can't release
            Assert.assertFalse(session1.release());

            // Test SessionSupplier interface - use session pool
            Session session2 = client.getOrCreateSession(Duration.ZERO).join().expect("cannot create session");
            // Server has 2 session
            Assert.assertEquals(2, sessionIDs.size());
            // Session in pool - successful release
            Assert.assertTrue(session2.release());
            // But server still has 2 session
            Assert.assertEquals(2, sessionIDs.size());

            // Session1 not used by pool - manual close
            Assert.assertTrue(session1.close().get().isSuccess());
            // After closing server still has 1 session
            Assert.assertEquals(1, sessionIDs.size());
            // Repeat close return error
            Assert.assertFalse(session1.close().get().isSuccess());
        }

        // After closing TableClient SessionPool released all sessions
        Assert.assertEquals(0, sessionIDs.size());
    }

    @Test
    public void unavailableSessions() {
        TableRpc fakeRpc = new TableRpcStub() {
            @Override
            public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
                YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
                return CompletableFuture.completedFuture(Result.fail(StatusCode.TRANSPORT_UNAVAILABLE));
            }
        };

        try (TableClient client = TableClient.newClient(fakeRpc).build()) {
            // Test TableClient interface
            Result<Session> sessionResult = client.createSession().join();
            Assert.assertFalse(sessionResult.isSuccess());
            Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getCode());

            // Test SessionSupplier interface
            sessionResult = client.getOrCreateSession(Duration.ZERO).join();
            Assert.assertFalse(sessionResult.isSuccess());
            Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getCode());
        }
    }
}
