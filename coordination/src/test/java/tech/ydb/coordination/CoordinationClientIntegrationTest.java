package tech.ydb.coordination;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.test.junit4.GrpcTransportRule;

public class CoordinationClientIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationClientIntegrationTest.class);

    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();

    public static final CoordinationClient CLIENT = CoordinationClient.newClient(YDB_TRANSPORT);

    @Rule
    public final Timeout testTimeoutRule = new Timeout(10, TimeUnit.SECONDS);

    @Test
    public void alterNodeTest() {
        String nodePath = CLIENT.getDatabase() + "/alter-node";

        NodeConfig config = NodeConfig.create()
                .withDurationsConfig(Duration.ofSeconds(5), Duration.ofSeconds(25))
                .withReadConsistencyMode(NodeConfig.ConsistencyMode.RELAXED)
                .withAttachConsistencyMode(NodeConfig.ConsistencyMode.STRICT)
                .withRateLimiterCountersMode(NodeConfig.RateLimiterCountersMode.DETAILED);

        CoordinationNodeSettings createSettings = CoordinationNodeSettings.newBuilder()
                .withNodeConfig(config)
                .build();

        CLIENT.createNode(nodePath, createSettings).join().expectSuccess("creating of new node failed");

        NodeConfig createdConfig = CLIENT.describeNode(nodePath).join().getValue();
        Assert.assertEquals(Duration.ofSeconds(5), createdConfig.getSelfCheckPeriod());
        Assert.assertEquals(Duration.ofSeconds(25), createdConfig.getSessionGracePeriod());
        Assert.assertEquals(NodeConfig.ConsistencyMode.RELAXED, createdConfig.getReadConsistencyMode());
        Assert.assertEquals(NodeConfig.ConsistencyMode.STRICT, createdConfig.getAttachConsistencyMode());
        Assert.assertEquals(NodeConfig.RateLimiterCountersMode.DETAILED, createdConfig.getRateLimiterCountersMode());

        NodeConfig alterConfig = NodeConfig.create()
                .withDurationsConfig(Duration.ofSeconds(10), Duration.ofSeconds(20))
                .withReadConsistencyMode(NodeConfig.ConsistencyMode.UNSET)
                .withAttachConsistencyMode(NodeConfig.ConsistencyMode.RELAXED)
                .withRateLimiterCountersMode(NodeConfig.RateLimiterCountersMode.AGGREGATED);

        CoordinationNodeSettings alterSettings = CoordinationNodeSettings.newBuilder()
                .withNodeConfig(alterConfig)
                .build();

        CLIENT.alterNode(nodePath, alterSettings).join().expectSuccess("alter of node failed");

        NodeConfig alteredConfig = CLIENT.describeNode(nodePath).join().getValue();
        Assert.assertEquals(Duration.ofSeconds(10), alteredConfig.getSelfCheckPeriod());
        Assert.assertEquals(Duration.ofSeconds(20), alteredConfig.getSessionGracePeriod());
        Assert.assertEquals(NodeConfig.ConsistencyMode.RELAXED, alteredConfig.getReadConsistencyMode());
        Assert.assertEquals(NodeConfig.ConsistencyMode.RELAXED, alteredConfig.getAttachConsistencyMode());
        Assert.assertEquals(NodeConfig.RateLimiterCountersMode.AGGREGATED, alteredConfig.getRateLimiterCountersMode());

        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }

    @Test
    public void doubleCreateTest() {
        String nodePath = "double-create-node";

        CLIENT.createNode(nodePath).join().expectSuccess("creating of new node failed");
        CLIENT.createNode(nodePath).join().expectSuccess("second creating of new node failed");

        NodeConfig config = CLIENT.describeNode(nodePath).join().getValue();
        NodeConfig changed = config.withDurationsConfig(
                config.getSelfCheckPeriod().plusSeconds(5),
                config.getSessionGracePeriod().plusSeconds(10)
        );
        CoordinationNodeSettings createSettings = CoordinationNodeSettings.newBuilder()
                .withNodeConfig(changed)
                .build();

        // We can send other config to create node, but it won't be applied
        CLIENT.createNode(nodePath, createSettings).join().expectSuccess("third creating of new node failed");
        NodeConfig updated = CLIENT.describeNode(nodePath).join().getValue();
        Assert.assertEquals(config.getSelfCheckPeriod(), updated.getSelfCheckPeriod());
        Assert.assertEquals(config.getSessionGracePeriod(), updated.getSessionGracePeriod());

        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }

    @Test
    public void doubleDropTest() {
        String nodePath = "double-drop-node";

        CLIENT.createNode(nodePath).join().expectSuccess("creating of new node failed");
        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");

        Status secondDrop = CLIENT.dropNode(nodePath).join();
        Assert.assertFalse(secondDrop.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, secondDrop.getCode());
    }

    @Test
    public void createSessionTest() {
        String nodePath = "test-sessions/create-test";

        logger.info("create node");
        CLIENT.createNode(nodePath).join().expectSuccess("creating of node failed");

        logger.info("create session");
        CoordinationSession session = CLIENT.createSession(nodePath);

        List<CoordinationSession.State> states = new ArrayList<>();
        session.addStateListener(state -> states.add(state));

        Assert.assertEquals(CoordinationSession.State.UNSTARTED, session.getState());
        Assert.assertNull(session.getId());
        Assert.assertTrue(states.isEmpty());

        logger.info("connect session");
        session.connect().join().expectSuccess("cannot connect session");

        Assert.assertEquals(2, states.size());
        Assert.assertEquals(CoordinationSession.State.CONNECTING, states.get(0));
        Assert.assertEquals(CoordinationSession.State.CONNECTED, states.get(1));
        Assert.assertEquals(CoordinationSession.State.CONNECTED, session.getState());
        Assert.assertNotNull(session.getId());

        logger.info("stop session");
        session.close();

        Assert.assertEquals(3, states.size());
        Assert.assertEquals(CoordinationSession.State.CLOSED, states.get(2));
        Assert.assertEquals(CoordinationSession.State.CLOSED, session.getState());

        logger.info("drop node");
        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }

    @Test
    public void createSemaphoreTest() {
        String nodePath = "test-sessions/create-semaphore-test";
        String semaphoreName = "semaphore1";
        byte[] semaphoreData = new byte[] { 0x00, 0x12 };

        logger.info("create node");
        CLIENT.createNode(nodePath).join().expectSuccess("creating of node failed");
        logger.info("create session");
        CoordinationSession session = CLIENT.createSession(nodePath);
        logger.info("connect session");
        session.connect().join().expectSuccess("cannot connect session");

        logger.info("create semaphore");
        session.createSemaphore(semaphoreName, 10, semaphoreData).join().expectSuccess("cannot create semaphore");

        logger.info("delete semaphore");
        session.deleteSemaphore(semaphoreName).join().expectSuccess("cannpt create semaphore");

        logger.info("stop session");
        session.close();
        logger.info("drop node");
        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }

    @Test
    public void acquireSemaphoreTest() {
        String nodePath = "test-sessions/acquire-semaphore-test";
        String semaphoreName = "semaphore2";
        Duration timeout = Duration.ofSeconds(5);

        logger.info("create node");
        CLIENT.createNode(nodePath).join().expectSuccess("creating of node failed");
        logger.info("create sessions");
        CoordinationSession session1 = CLIENT.createSession(nodePath);
        CoordinationSession session2 = CLIENT.createSession(nodePath);
        logger.info("connect sessions");
        session1.connect().join().expectSuccess("cannot connect session");
        session2.connect().join().expectSuccess("cannot connect session");
        logger.info("create semaphore");
        session1.createSemaphore(semaphoreName, 20).join().expectSuccess("cannot create semaphore");

        logger.info("take first lease");
        CompletableFuture<Result<SemaphoreLease>> lease2 = session2.acquireSemaphore(semaphoreName, 15, timeout);
        lease2.join().getStatus().expectSuccess("cannot acquire semaphore");

        logger.info("request second lease, waiting");
        CompletableFuture<Result<SemaphoreLease>> lease1 = session1.acquireSemaphore(semaphoreName, 15, timeout);
        Assert.assertFalse(lease1.isDone());

        logger.info("release first lease, complete second lease");
        lease2.join().getValue().release().join();
        lease1.join().getStatus().expectSuccess("cannot acquire semaphore");

        logger.info("release second lease");
        lease1.join().getValue().release().join();

        logger.info("delete semaphore");
        session2.deleteSemaphore(semaphoreName).join().expectSuccess("cannpt create semaphore");
        logger.info("stop sessions");
        session1.close();
        session2.close();
        logger.info("drop node");
        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }
}