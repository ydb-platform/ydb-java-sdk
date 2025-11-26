package tech.ydb.table.impl;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.table.YdbTable;
import tech.ydb.table.Session;
import tech.ydb.table.SessionSupplier;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.rpc.TableRpc;


/**
 * @author Sergey Polovko
 */
public class SimpleTableClientTest {

    @Test
    public void createSessionAndRelease() {
        Set<String> sessionIDs = new HashSet<>();

        TableRpc fakeRpc = new TableRpcStub(null) {
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
        client.createSession(Duration.ZERO).join().getValue();
        // Server has 1 session
        Assert.assertEquals(1, sessionIDs.size());

        // Create second session
        try (Session session2 = client.createSession(Duration.ZERO).join().getValue()) {
            // Server has 2 session
            Assert.assertEquals(2, sessionIDs.size());
            session2.getId();
        }
        // But server still has 1 session
        Assert.assertEquals(1, sessionIDs.size());
    }

    @Test
    public void unavailableSessions() {
        TableRpc fakeRpc = new TableRpcStub(null) {
            @Override
            public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
                YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
                return CompletableFuture.completedFuture(Result.fail(Status.of(StatusCode.TRANSPORT_UNAVAILABLE)));
            }
        };

        SimpleTableClient client = SimpleTableClient.newClient(fakeRpc)
                .keepQueryText(true)
                .build();

        Assert.assertNull(client.getScheduler());

        // Test TableClient interface
        Result<Session> sessionResult = client.createSession(Duration.ZERO).join();
        Assert.assertFalse(sessionResult.isSuccess());
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getStatus().getCode());

        // Test SessionSupplier interface
        sessionResult = client.createSession(Duration.ZERO).join();
        Assert.assertFalse(sessionResult.isSuccess());
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, sessionResult.getStatus().getCode());
    }
}
