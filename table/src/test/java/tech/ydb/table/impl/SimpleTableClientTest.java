package tech.ydb.table.impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.table.Session;
import tech.ydb.table.SessionSupplier;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.rpc.TableRpc;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sergey Polovko
 */
public class SimpleTableClientTest {
    @Test
    public void createSessionAndRelease() throws InterruptedException, ExecutionException {
        Set<String> sessionIDs = new HashSet<>();

        TableRpc fakeRpc = new TableRpcStub() {
            private int counter = 0;

            @Override
            public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
                YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {

                counter += 1;
                String id = "session " + counter;
                YdbTable.CreateSessionResult result = YdbTable.CreateSessionResult.newBuilder()
                    .setSessionId(id)
                    .build();

                sessionIDs.add(id);
                return CompletableFuture.completedFuture(Result.success(result));
            }

            @Override
            public CompletableFuture<Status> deleteSession(
                    YdbTable.DeleteSessionRequest request, GrpcRequestSettings settings) {
                String id = request.getSessionId();

                if (sessionIDs.contains(id)) {
                    sessionIDs.remove(id);
                    return CompletableFuture.completedFuture(Status.SUCCESS);
                } else {
                    return CompletableFuture.completedFuture(Status.of(StatusCode.BAD_SESSION));
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
            public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
                YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
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
