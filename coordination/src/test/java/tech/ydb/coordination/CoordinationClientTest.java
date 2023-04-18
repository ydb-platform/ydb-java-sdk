package tech.ydb.coordination;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.rpc.CoordinationGrpc;
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

//    @Test
//    public void coordinationStartSessionAndAcquireTest() {
//        CoordinationSession session = client.createSession(path);
//        CompletableFuture<SessionResponse> startCompletableFuture = new CompletableFuture<>();
//
//        CompletableFuture<Status> status = session.start(startCompletableFuture::complete);
//
//        session.sendStartSession(
//                SessionRequest.SessionStart.newBuilder()
//                        .setPath(path)
//                        .setTimeoutMillis(2_000)
//                        .build()
//        );
//
//        startCompletableFuture.join();
//        Assert.assertTrue(startCompletableFuture.isDone());
//
//        session.stop();
//        Assert.assertTrue(status.join().isSuccess());
//    }

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
