package tech.ydb.coordination;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.test.junit4.GrpcTransportRule;

public class CoordinationClientIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CoordinationClientIntegrationTest.class);

    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();

    public static final CoordinationClient CLIENT = CoordinationClient.newClient(YDB_TRANSPORT);

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
        session.createSemaphore(semaphoreName, 10, semaphoreData).join().expectSuccess("cannpt create semaphore");

        logger.info("delete semaphore");
        session.deleteSemaphore(semaphoreName).join().expectSuccess("cannpt create semaphore");

        logger.info("stop session");
        session.close();

        logger.info("drop node");
        CLIENT.dropNode(nodePath).join().expectSuccess("removing of node failed");
    }
/*
    @Test(timeout = 20_000)
    public void coordinationSessionFullCycleTest() {
        final String semaphoreName = "test-semaphore";
        try (CoordinationSession session = client.createSession(path)) {
            session.connect().join().expectSuccess();
            session.createSemaphore(semaphoreName, 100).get(20, TimeUnit.SECONDS);
            SemaphoreLease semaphore = session.acquireSemaphore(semaphoreName, 70, timeout)
                    .join().getValue();

            SemaphoreWatcher watch = session.describeAndWatchSemaphore(semaphoreName,
                    DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                    WatchSemaphoreMode.WATCH_DATA
            ).join().getValue();

            Assert.assertEquals(semaphoreName, watch.getDescription().getName());
            Assert.assertEquals(70, watch.getDescription().getCount());
            Assert.assertEquals(100, watch.getDescription().getLimit());
            Assert.assertTrue(watch.getDescription().getWaitersList().isEmpty());

            Assert.assertFalse(watch.getChangedFuture().isDone());
            final byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
            session.updateSemaphore(semaphoreName, data).join();
            Assert.assertTrue(watch.getChangedFuture().get(1, TimeUnit.MINUTES).isDataChanged());
            semaphore.release().join();
        } catch (Exception e) {
            Assert.fail("There have to be no exceptions. [exception]: " + e);
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 20_000)
    public void ephemeralSemaphoreBaseTest() {
        final String semaphoreName = "coordination-client-ephemeral-semaphore-base-test";
        try (CoordinationSession session = client.createSession(path)) {
            session.connect().join().expectSuccess();
            session.acquireEphemeralSemaphore(semaphoreName, true, timeout)
                    .join().getValue();
            final SemaphoreDescription description =
                    session.describeSemaphore(semaphoreName, DescribeSemaphoreMode.DATA_ONLY)
                            .join()
                            .getValue();
            Assert.assertEquals(-1L, description.getLimit());
            Assert.assertEquals(-1L, description.getCount());
            Assert.assertEquals(semaphoreName, description.getName());
            Assert.assertTrue(description.getWaitersList().isEmpty());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 100_000)
    public void retryCoordinationSessionTest() {
        final CoordinationRpc rpc = new CoordinationProxyRpc(CoordinationGrpc.useTransport(YDB_TRANSPORT));
        final String semaphoreName = "retry-test";
        final int sessionNum = 10;
        CoordinationClient mockClient = new CoordinationClientImpl(rpc);

        try (CoordinationSession session = mockClient.createSession(path)) {
            session.connect().join().expectSuccess();
            session.createSemaphore(semaphoreName, 90 + sessionNum + 1).join();
            SemaphoreLease lease = session.acquireSemaphore(semaphoreName, 90, timeout).join().getValue();

            Assert.assertEquals(session.updateSemaphore(semaphoreName,
                    "data".getBytes(StandardCharsets.UTF_8)).join(), Status.SUCCESS);

            List<CoordinationSession> sessions = ThreadLocalRandom
                    .current()
                    .ints(sessionNum)
                    .mapToObj(n -> mockClient.createSession(path))
                    .collect(Collectors.toList());

            sessions.forEach(s -> s.connect().join().expectSuccess());

            ProxyStream.IS_STOPPED.set(true);
//            ------------------------
            List<CompletableFuture<Result<SemaphoreLease>>> acquireFutures = new ArrayList<>();
            sessions.forEach(otherSession -> {
                final CompletableFuture<Result<SemaphoreLease>> acquireFuture = new CompletableFuture<>();
                acquireFutures.add(acquireFuture);
                otherSession.createSemaphore(semaphoreName, 1)
                        .whenComplete((result, thSem) -> {
                            Assert.assertNull(thSem);
                            otherSession.acquireSemaphore(semaphoreName, 1, timeout)
                                    .whenComplete((acquired, th) ->
                                            acquireFuture.complete(acquired));
                        });
            });

            session.updateSemaphore(semaphoreName, "changed data".getBytes(StandardCharsets.UTF_8));
//            ------------------------
            ProxyStream.IS_STOPPED.set(false);

            for (CompletableFuture<Result<SemaphoreLease>> future : acquireFutures) {
                Assert.assertTrue(future.get(timeout.toMillis(), TimeUnit.MILLISECONDS).isSuccess());
            }
            final SemaphoreDescription desc = session.describeSemaphore(semaphoreName, DescribeSemaphoreMode.DATA_ONLY)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .getValue();
            Assert.assertEquals(90 + sessionNum, desc.getCount());
            Assert.assertArrayEquals("changed data".getBytes(StandardCharsets.UTF_8), desc.getData());

            for (CoordinationSession coordinationSession : sessions) {
                coordinationSession.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

final class CoordinationProxyRpc implements CoordinationRpc {
    private final CoordinationRpc rpc;

    CoordinationProxyRpc(CoordinationRpc rpc) {
        this.rpc = rpc;
    }

    @Override
    public GrpcReadWriteStream<SessionResponse, SessionRequest> session() {
        return new ProxyStream(rpc.session());
    }

    @Override
    public CompletableFuture<Status> createNode(CreateNodeRequest request, GrpcRequestSettings settings) {
        return rpc.createNode(request, settings);
    }

    @Override
    public CompletableFuture<Status> alterNode(AlterNodeRequest request, GrpcRequestSettings settings) {
        return rpc.alterNode(request, settings);
    }

    @Override
    public CompletableFuture<Status> dropNode(DropNodeRequest request, GrpcRequestSettings settings) {
        return rpc.dropNode(request, settings);
    }

    @Override
    public CompletableFuture<Status> describeNode(DescribeNodeRequest request, GrpcRequestSettings settings) {
        return rpc.describeNode(request, settings);
    }

    @Override
    public String getDatabase() {
        return rpc.getDatabase();
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return rpc.getScheduler();
    }
}

final class ProxyStream implements GrpcReadWriteStream<SessionResponse, SessionRequest> {
    static final AtomicBoolean IS_STOPPED = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(ProxyStream.class);
    private final GrpcReadWriteStream<SessionResponse, SessionRequest> workStream;
    private Observer<SessionResponse> observer;

    ProxyStream(GrpcReadWriteStream<SessionResponse, SessionRequest> workStream) {
        logger.trace("Create MockedStream: " + workStream);
        this.workStream = workStream;
    }

    @Override
    public CompletableFuture<Status> start(Observer<SessionResponse> observer) {
        // TODO: test with stream ruin in start method
        this.observer = observer;
        final CompletableFuture<Status> c = workStream.start(observer);
        logger.trace("Start in MockedStream: return " + IS_STOPPED);
        return IS_STOPPED.get() ? new CompletableFuture<>() : c;
    }

    @Override
    public void cancel() {
        workStream.cancel();
    }

    @Override
    public String authToken() {
        return workStream.authToken();
    }

    @Override
    public void sendNext(SessionRequest message) {
        if (!IS_STOPPED.get()) {
            workStream.sendNext(message);
        } else {
            observer.onNext(SessionResponse.newBuilder().setFailure(Failure.newBuilder().build()).build());
        }
    }

    @Override
    public void close() {
        workStream.close();
    }
*/
}
