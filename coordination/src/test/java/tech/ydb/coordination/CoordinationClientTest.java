package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.rpc.CoordinationGrpc;
import tech.ydb.coordination.session.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;

public class CoordinationClientTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final String path = ydbTransport.getDatabase() + "/coordination-node";
    private final CoordinationClient client = new CoordinationClientImpl(
            CoordinationGrpc.useTransport(ydbTransport)
    );

    @Before
    public void createNode() {
        CompletableFuture<Status> result = client.createNode(
                path,
                CoordinationNodeSettings.newBuilder()
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test
    public void alterNodeTest() {
        CompletableFuture<Status> result = client.alterNode(
                path,
                CoordinationNodeSettings.newBuilder()
                        .setReadConsistencyMode(
                                CoordinationNodeSettings.ConsistencyMode.CONSISTENCY_MODE_STRICT
                        )
                        .setSelfCheckPeriodMillis(2_000)
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test
    public void coordinationSessionFullCycleTest() {
        CoordinationSession session = client.createSession(path);

        CompletableFuture<SessionResponse> startCompletableFuture = new CompletableFuture<>();
        CompletableFuture<SessionResponse> acquireCompletableFuture = new CompletableFuture<>();
        CompletableFuture<SessionResponse> createCompletableFuture = new CompletableFuture<>();

        CompletableFuture<Status> status = session.start(
                value -> {
                    switch (value.getResponseCase()) {
                        case SESSION_STARTED:
                            startCompletableFuture.complete(value);
                            break;
                        case ACQUIRE_SEMAPHORE_RESULT:
                            acquireCompletableFuture.complete(value);
                            break;
                        case CREATE_SEMAPHORE_RESULT:
                            createCompletableFuture.complete(value);
                            break;
                    }
                }
        );

        session.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setPath(path)
                        .setTimeoutMillis(10_000)
                        .build()
        );

        startCompletableFuture.join();
        Assert.assertTrue(startCompletableFuture.join().hasSessionStarted());

        final String semaphoreName = "test-semaphore";

        session.sendCreateSemaphore(
            SessionRequest.CreateSemaphore.newBuilder()
                    .setLimit(100)
                    .setName(semaphoreName)
                    .build()
        );

        Assert.assertEquals(StatusCodesProtos.StatusIds.StatusCode.SUCCESS,
                createCompletableFuture.join().getCreateSemaphoreResult().getStatus());

        session.sendAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(1)
                        .build()
        );

        Assert.assertTrue(acquireCompletableFuture.join().getAcquireSemaphoreResult().getAcquired());

        session.stop();
        Assert.assertTrue(status.join().isSuccess());
    }

    @After
    public void deleteNode() {
        CompletableFuture<Status> result = client.dropNode(
                path,
                DropCoordinationNodeSettings.newBuilder()
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }
}
