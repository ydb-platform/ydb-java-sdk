package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.impl.CoordinationGrpc;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.scenario.service_discovery.Subscriber;
import tech.ydb.coordination.scenario.service_discovery.Worker;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.proto.coordination.AlterNodeRequest;
import tech.ydb.proto.coordination.CreateNodeRequest;
import tech.ydb.proto.coordination.DescribeNodeRequest;
import tech.ydb.proto.coordination.DropNodeRequest;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;
import tech.ydb.proto.coordination.SessionResponse.Failure;
import tech.ydb.test.junit4.GrpcTransportRule;

public class CoordinationClientTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private final String path = YDB_TRANSPORT.getDatabase() + "/coordination-node";
    private final Duration timeout = Duration.ofSeconds(60);
    private final CoordinationClient client = CoordinationClient.newClient(YDB_TRANSPORT);

    @Before
    public void createNode() {
        CompletableFuture<Status> result = client.createNode(
                path,
                CoordinationNodeSettings.newBuilder()
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test(timeout = 20_000)
    public void alterNodeTest() {
        CompletableFuture<Status> result = client.alterNode(
                path,
                CoordinationNodeSettings.newBuilder()
                        .setReadConsistencyMode(CoordinationNodeSettings.ConsistencyMode.CONSISTENCY_MODE_STRICT)
                        .setSelfCheckPeriodMillis(2_000)
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test(timeout = 20_000)
    public void coordinationSessionFullCycleTest() {
        final String semaphoreName = "test-semaphore";
        try (CoordinationSession session = client.createSession(path).join()) {
            session.createSemaphore(semaphoreName, 100).get(3, TimeUnit.SECONDS);
            SemaphoreLease semaphore = session.acquireSemaphore(semaphoreName, 70, Duration.ofSeconds(3))
                    .join();
            final CompletableFuture<Boolean> dataChangedFuture = new CompletableFuture<>();
            final Consumer<SemaphoreChangedEvent> updateWatcher =
                    changes -> dataChangedFuture.complete(changes.isDataChanged());

            final SemaphoreDescription description = session.describeSemaphore(semaphoreName,
                    DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                    WatchSemaphoreMode.WATCH_DATA,
                    updateWatcher
            ).join().getValue();

            Assert.assertEquals(semaphoreName, description.getName());
            Assert.assertEquals(70, description.getCount());
            Assert.assertEquals(100, description.getLimit());
            Assert.assertEquals(Collections.emptyList(), description.getWaitersList());

            Assert.assertFalse(dataChangedFuture.isDone());
            final byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
            session.updateSemaphore(semaphoreName, data).join();
            Assert.assertTrue(dataChangedFuture.get(1, TimeUnit.MINUTES));
            Assert.assertTrue(semaphore.release().join());
        } catch (Exception e) {
            Assert.fail("There have to be no exceptions. [exception]: " + e);
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 20_000)
    public void ephemeralSemaphoreBaseTest() {
        final String semaphoreName = "ephemeral-semaphore-base-test";
        /* 18446744073709551615 = 2^64 - 1 (or just -1 in Java) */
        final long count = -1;
        try (CoordinationSession session = client.createSession(path).join()) {
            session.acquireSemaphore(semaphoreName,
                            count, true, Duration.ofSeconds(3))
                    .join();
            final SemaphoreDescription description = session.describeSemaphore(semaphoreName, DescribeSemaphoreMode.DATA_ONLY)
                    .join()
                    .getValue();
            Assert.assertEquals(-1, description.getLimit());
            Assert.assertEquals(-1, description.getCount());
            Assert.assertEquals(semaphoreName, description.getName());
            Assert.assertEquals(Collections.emptyList(), description.getWaitersList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 60_000)
    public void retryCoordinationSessionTest() {
        final CoordinationRpc rpc = new CoordinationProxyRpc(CoordinationGrpc.useTransport(YDB_TRANSPORT));
        final String semaphoreName = "retry-test";
        final int sessionNum = 10;
        CoordinationClient mockClient = new CoordinationClientImpl(rpc);

        try (CoordinationSession session = mockClient.createSession(path).join()) {
            session.createSemaphore(semaphoreName, 90 + sessionNum + 1).join();
            SemaphoreLease semaphore = session.acquireSemaphore(semaphoreName, 90, timeout).join();
            Assert.assertEquals(session.updateSemaphore(semaphoreName,
                    "data".getBytes(StandardCharsets.UTF_8)).join(), Status.SUCCESS);

            List<CoordinationSession> sessions = ThreadLocalRandom
                    .current()
                    .ints(sessionNum)
                    .mapToObj(n -> mockClient.createSession(path))
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            ProxyStream.IS_STOPPED.set(true);
//            ------------------------
            List<CompletableFuture<SemaphoreLease>> acquireFutures = new ArrayList<>();
            sessions.forEach(otherSession -> {
                final CompletableFuture<SemaphoreLease> acquireFuture = new CompletableFuture<>();
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

            for (CompletableFuture<SemaphoreLease> future : acquireFutures) {
                Assert.assertTrue(future.get(timeout.toMillis(), TimeUnit.MILLISECONDS).isValid());
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

    @Test(timeout = 20_000)
    public void leaderElectionTest() throws InterruptedException {
        final Duration duration = Duration.ofSeconds(60);
        final String semaphoreName = "leader-election-semaphore";
        final int sessionCount = 10;
        final CountDownLatch latch1 = new CountDownLatch(sessionCount);
        List<CoordinationSession> sessions = Stream.generate(() -> client.createSession(path).join())
                .limit(sessionCount)
                .collect(Collectors.toList());

        CompletableFuture<SemaphoreLease> semaphore = new CompletableFuture<>();
        CompletableFuture<CoordinationSession> leader = new CompletableFuture<>();

        sessions.forEach(session ->
                session.createSemaphore(semaphoreName, 1)
                        .whenComplete((status, createSemaphoreTh) -> {
                                    latch1.countDown();
                                    Assert.assertNull(createSemaphoreTh);
                                    Assert.assertTrue(status == Status.SUCCESS &&
                                            status.getCode() == StatusCode.ALREADY_EXISTS);
                                }
                        )
        );

        latch1.await();
        final CountDownLatch latch2 = new CountDownLatch(sessionCount);

        sessions.forEach(session -> session.acquireSemaphore(semaphoreName, 1, false, Duration.ZERO,
                        String.valueOf(session.getId()).getBytes())
                .whenComplete((lease, acquireSemaphoreTh) -> {
                            Assert.assertNull(acquireSemaphoreTh);
                            if (lease.isValid()) {
                                semaphore.complete(lease);
                                leader.complete(session);
                            }
                            latch2.countDown();
                        }
                )
        );

        latch2.await();
        final CoordinationSession leaderSession = leader.join();
        final CountDownLatch latch3 = new CountDownLatch(sessionCount);

        sessions.forEach(session -> session.describeSemaphore(semaphoreName, DescribeSemaphoreMode.WITH_OWNERS)
                .whenComplete((result, th) -> {
                    Assert.assertTrue(result.isSuccess());
                    Assert.assertNull(th);
                    Assert.assertArrayEquals(String.valueOf(leaderSession.getId()).getBytes(),
                            result.getValue().getOwnersList().get(0).getData());
                    latch3.countDown();
                }));

        latch3.await();
    }

    @Test(timeout = 20_000)
    public void serviceDiscoveryTest() {
        try (CoordinationSession checkSession = client.createSession(path).join()) {
            final CoordinationSession session1 = client.createSession(path).join();
            final Worker worker1 = Worker.newWorker(session1, "endpoint-1", timeout).join();

            final SemaphoreDescription oneWorkerDescription = checkSession
                    .describeSemaphore(Worker.SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS)
                    .join()
                    .getValue();

            Assert.assertEquals("endpoint-1", new String(oneWorkerDescription.getOwnersList().get(0).getData()));
            Assert.assertEquals(1, oneWorkerDescription.getOwnersList().size());

            final CoordinationSession session2 = client.createSession(path).join();
            final Worker worker2 = Worker.newWorker(session2, "endpoint-2", timeout).join();
            /* The First knows about The Second */
            final Subscriber subscriber1 = Subscriber.newSubscriber(session1).join();
            SemaphoreDescription subscriberOneDescription = subscriber1.getDescription().join();
            Assert.assertTrue(subscriberOneDescription
                    .getOwnersList()
                    .stream()
                    .anyMatch(semaphoreSession -> "endpoint-2".equals(new String(semaphoreSession.getData())))
            );
            Assert.assertEquals(2, subscriberOneDescription.getOwnersList().size());
            /* The Second knows about The First */
            final Subscriber subscriber2 = Subscriber.newSubscriber(session2).join();
            subscriberOneDescription = subscriber2.getDescription().join();
            Assert.assertTrue(subscriberOneDescription
                    .getOwnersList()
                    .stream()
                    .anyMatch(semaphoreSession -> "endpoint-1".equals(new String(semaphoreSession.getData())))
            );
            Assert.assertEquals(2, subscriberOneDescription.getOwnersList().size());

            /* Remove The First worker */
            final Boolean stoppedWorker1 = worker1.stop().join();
            Assert.assertEquals(true, stoppedWorker1);
            final SemaphoreDescription removeDescription = subscriber2.getDescription().join();
            Assert.assertEquals("endpoint-2", new String(removeDescription.getOwnersList().get(0).getData()));
            Assert.assertEquals(1, removeDescription.getOwnersList().size());
            Assert.assertEquals(removeDescription,
                    checkSession.describeSemaphore(Worker.SEMAPHORE_NAME, DescribeSemaphoreMode.WITH_OWNERS).join().getValue());

            Assert.assertEquals(true, worker2.stop().join());
        } catch (Exception e) {
            Assert.fail("There shouldn't be an exception.");
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

class ServiceDiscovery {
    private final CoordinationSession session;
    private final String endpoint;

    ServiceDiscovery(CoordinationSession session, String endpoint) {
        this.session = session;
        this.endpoint = endpoint;
    }

    public CoordinationSession getSession() {
        return session;
    }

    public String getEndpoint() {
        return endpoint;
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
}
