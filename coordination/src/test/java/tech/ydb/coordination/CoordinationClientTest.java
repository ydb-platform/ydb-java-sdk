package tech.ydb.coordination;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import tech.ydb.coordination.CoordinationSessionNew.CoordinationSemaphore;
import tech.ydb.coordination.CoordinationSessionNew.DescribeMode;
import tech.ydb.coordination.CoordinationSessionNew.WatchMode;
import tech.ydb.coordination.impl.CoordinationClientImpl;
import tech.ydb.coordination.rpc.CoordinationRpc;
import tech.ydb.coordination.rpc.grpc.GrpcCoordinationRpc;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreChanged;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.coordination.settings.SemaphoreDescription;
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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationClientTest {
    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();
    private static final Logger logger = LoggerFactory.getLogger(CoordinationClientTest.class);
    private final String path = YDB_TRANSPORT.getDatabase() + "/coordination-node";
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

    @Test(timeout = 60_000)
    public void coordinationSessionFullCycleTest() {
        final String semaphoreName = "test-semaphore";
        try (CoordinationSessionNew session = client.createSession(path, Duration.ofSeconds(3)).join()) {
            session.createSemaphore(semaphoreName, 100).get(3, TimeUnit.SECONDS);
            CoordinationSemaphore semaphore = session.acquireSemaphore(semaphoreName, 70, Duration.ofSeconds(3))
                    .join().getValue();
            final CompletableFuture<Boolean> dataChangedFuture = new CompletableFuture<>();
            final Consumer<DescribeSemaphoreChanged> updateWatcher =
                    changes -> dataChangedFuture.complete(changes.isDataChanged());

            final SemaphoreDescription description = session.describeSemaphore(semaphoreName,
                    DescribeMode.WITH_OWNERS_AND_WAITERS,
                    WatchMode.WATCH_DATA,
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
            Assert.assertTrue(semaphore.release().join().getValue());

            session.removeWatcher(semaphoreName);
        } catch (Exception e) {
            Assert.fail("There have to be no exceptions. [exception]: " + e);
            throw new RuntimeException(e);
        }
    }

    @Test(timeout = 60_000)
    public void ephemeralSemaphoreBaseTest() {
        final String semaphoreName = "ephemeral-semaphore-base-test";
        /* 18446744073709551615 = 2^64 - 1 (or just -1 in Java) */
        final long count = -1;
        try (CoordinationSessionNew session = client.createSession(path, Duration.ofSeconds(3)).join()) {
            session.acquireSemaphore(semaphoreName,
                            count, true, Duration.ofSeconds(3))
                    .join()
                    .getValue();
            final SemaphoreDescription description = session.describeSemaphore(semaphoreName, DescribeMode.DATA_ONLY)
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

    @Test
    public void retryCoordinationSessionTest() {
        final CoordinationRpc rpc = new CoordinationProxyRpc(GrpcCoordinationRpc.useTransport(YDB_TRANSPORT));
        final String semaphoreName = "retry-test";
        CoordinationClient mockClient = new CoordinationClientImpl(rpc);

        try (CoordinationSessionNew session = mockClient.createSession(path, Duration.ofSeconds(100)).join();) {
            session.createSemaphore(semaphoreName, 101).join();
            CoordinationSemaphore semaphore = session.acquireSemaphore(semaphoreName, 90, Duration.ofSeconds(20))
                    .join().getValue();
            Assert.assertEquals(session.updateSemaphore(semaphoreName,
                    "data".getBytes(StandardCharsets.UTF_8)).join(), Status.SUCCESS);

            List<CoordinationSessionNew> sessions = ThreadLocalRandom
                    .current()
                    .ints(10)
                    .mapToObj(n -> mockClient.createSession(path, Duration.ofSeconds(100)))
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            ProxyStream.IS_STOPPED.set(true);
//            ------------------------
            List<CompletableFuture<Result<CoordinationSemaphore>>> acquireFutures = new ArrayList<>();
            sessions.forEach(otherSession -> {
                final CompletableFuture<Result<CoordinationSemaphore>> acquireFuture = new CompletableFuture<>();
                acquireFutures.add(acquireFuture);
                otherSession.createSemaphore(semaphoreName, 1)
                        .whenComplete((result, thSem) -> {
                            Assert.assertNull(thSem);
                            otherSession.acquireSemaphore(semaphoreName, 1, Duration.ofSeconds(100))
                                    .whenComplete((acquired, th) ->
                                            acquireFuture.complete(acquired));
                        });
            });

            session.updateSemaphore(semaphoreName, "changed data".getBytes(StandardCharsets.UTF_8));
//            ------------------------
            ProxyStream.IS_STOPPED.set(false);

            for (CompletableFuture<Result<CoordinationSemaphore>> future : acquireFutures) {
                Assert.assertEquals(Status.SUCCESS, future.get(100, TimeUnit.SECONDS).getStatus());
            }
            final SemaphoreDescription desc = session.describeSemaphore(semaphoreName, DescribeMode.DATA_ONLY)
                    .get(100, TimeUnit.SECONDS)
                    .getValue();
            Assert.assertEquals(90 + 10, desc.getCount());
            Assert.assertArrayEquals("changed data".getBytes(StandardCharsets.UTF_8), desc.getData());

            for (CoordinationSessionNew coordinationSessionNew : sessions) {
                coordinationSessionNew.close();
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

    private static final class CoordinationProxyRpc implements CoordinationRpc {
        private final CoordinationRpc rpc;

        private CoordinationProxyRpc(CoordinationRpc rpc) {
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
    }

    private static final class ProxyStream implements GrpcReadWriteStream<SessionResponse, SessionRequest> {
        private static final AtomicBoolean IS_STOPPED = new AtomicBoolean(false);
        private final GrpcReadWriteStream<SessionResponse, SessionRequest> workStream;
        private Observer<SessionResponse> observer;

        private ProxyStream(GrpcReadWriteStream<SessionResponse, SessionRequest> workStream) {
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
}
