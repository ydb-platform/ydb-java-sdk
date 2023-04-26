package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.coordination.observer.CoordinationSessionObserver;
import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.session.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationClientTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final String path = ydbTransport.getDatabase() + "/coordination-node";
    private final CoordinationClient client = CoordinationClient.newClient(
            GrpcCoordinationRpc.useTransport(ydbTransport)
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
        CoordinationSession session = client.createSession();

        CompletableFuture<Boolean> startCompletableFuture = new CompletableFuture<>();
        CompletableFuture<Boolean> acquireCompletableFuture = new CompletableFuture<>();
        CompletableFuture<Status> createCompletableFuture = new CompletableFuture<>();

        CompletableFuture<Status> status = session.start(
                new CoordinationSessionObserver() {
                    @Override
                    public void onSessionStarted() {
                        startCompletableFuture.complete(true);
                    }

                    @Override
                    public void onAcquireSemaphoreResult(boolean acquired, Status status) {
                        if (status.isSuccess()) {
                            acquireCompletableFuture.complete(acquired);
                        }
                    }

                    @Override
                    public void onCreateSemaphoreResult(Status status) {
                        createCompletableFuture.complete(status);
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
        Assert.assertTrue(startCompletableFuture.join());

        final String semaphoreName = "test-semaphore";

        session.sendCreateSemaphore(
            SessionRequest.CreateSemaphore.newBuilder()
                    .setLimit(100)
                    .setName(semaphoreName)
                    .build()
        );

        Assert.assertTrue(createCompletableFuture.join().isSuccess());

        session.sendAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(1)
                        .build()
        );

        Assert.assertTrue(acquireCompletableFuture.join());

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
