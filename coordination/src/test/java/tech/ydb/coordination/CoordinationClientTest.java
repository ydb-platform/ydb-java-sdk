package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.impl.CoordinationGrpc;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.coordination.settings.NodeConsistenteMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
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
                        .withReadConsistencyMode(NodeConsistenteMode.STRICT)
                        .withSelfCheckPeriod(Duration.ofSeconds(2))
                        .build()
        );

        Assert.assertTrue(result.join().isSuccess());
    }

    @Test(timeout = 20_000)
    public void coordinationSessionFullCycleTest() {
        final String semaphoreName = "test-semaphore";
        try (CoordinationSession session = client.createSession(path).join()) {
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
            semaphore.close();
        } catch (Exception e) {
            Assert.fail("There have to be no exceptions. [exception]: " + e);
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 20_000)
    public void ephemeralSemaphoreBaseTest() {
        final String semaphoreName = "coordination-client-ephemeral-semaphore-base-test";
        try (CoordinationSession session = client.createSession(path).join()) {
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

        try (CoordinationSession session = mockClient.createSession(path).join()) {
            session.createSemaphore(semaphoreName, 90 + sessionNum + 1).join();
            SemaphoreLease lease = session.acquireSemaphore(semaphoreName, 90, timeout).join().getValue();

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
}
