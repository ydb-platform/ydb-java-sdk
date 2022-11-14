package tech.ydb.table.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.TableClient;
import tech.ydb.table.YdbTable;
import tech.ydb.table.impl.pool.MockedTableRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PooledTableClientTest {
    private void testAssert(String exceptionMsg, ThrowingRunnable runnable) {
        IllegalArgumentException ex = Assert.assertThrows(
                "IllegalArgumentException must be throwed",
                IllegalArgumentException.class,
                runnable
        );
        Assert.assertEquals("Validate exception msg", exceptionMsg, ex.getMessage());
    }

    @Test
    public void testSessionPoolAsserts() {
        testAssert("table rpc is null", () -> {
            PooledTableClient.newClient(null).build();
        });

        testAssert("sessionPoolMaxSize(0) is negative or zero", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionPoolSize(0, 0)
                    .build();
        });

        testAssert("sessionPoolMinSize(-1) is negative", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionPoolSize(-1, 1)
                    .build();
        });

        testAssert("sessionPoolMinSize(3) is greater than sessionPoolMaxSize(2)", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionPoolSize(3, 2)
                    .build();
        });

        testAssert("sessionKeepAliveTime(-5s) is negative", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionKeepAliveTime(Duration.ofSeconds(5).negated())
                    .build();
        });

        testAssert("sessionKeepAliveTime(0.999s) is less than minimal duration 1s", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionKeepAliveTime(Duration.ofMillis(999))
                    .build();
        });

        testAssert("sessionKeepAliveTime(31m) is greater than maximal duration 30m", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionKeepAliveTime(Duration.ofMinutes(31))
                    .build();
        });

        testAssert("sessionMaxIdleTime(-1s) is negative", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionMaxIdleTime(Duration.ofSeconds(1).negated())
                    .build();
        });

        testAssert("sessionMaxIdleTime(0.999s) is less than minimal duration 1s", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionMaxIdleTime(Duration.ofMillis(999))
                    .build();
        });

        testAssert("sessionMaxIdleTime(31m) is greater than maximal duration 30m", () -> {
            PooledTableClient.newClient(new DumpTableRpc())
                    .sessionMaxIdleTime(Duration.ofMinutes(31))
                    .build();
        });
    }

    @Test
    public void createSessionAndReleaseInARow() {
        TableClient client = PooledTableClient.newClient(new DumpTableRpc())
                .keepQueryText(true)
                .build();

        Session session1 = nextSession(client);
        check(client).acquired(1).idle(0);

        Session session2 = nextSession(client);
        check(client).acquired(2).idle(0);

        session1.close();
        check(client).acquired(1).idle(1);

        session2.close();
        check(client).acquired(0).idle(2);

        // LIFO
        Assert.assertSame(session2, nextSession(client));
        check(client).acquired(1).idle(1);

        Assert.assertSame(session1, nextSession(client));
        check(client).acquired(2).idle(0);

        session2.close();
        check(client).acquired(1).idle(1);

        session1.close();
        check(client).acquired(0).idle(2);

        Assert.assertSame(session1, nextSession(client));
        check(client).acquired(1).idle(1);

        Assert.assertSame(session2, nextSession(client));
        check(client).acquired(2).idle(0);

        session1.close();
        session2.close();

        client.close();
        check(client).acquired(0).idle(0);
    }


    @Test
    public void getOrCreateSessionAndReleaseRepeat() {
        TableClient client = PooledTableClient.newClient(new DumpTableRpc())
                .sessionPoolSize(0, 5)
                .build();

        for (int i = 0; i < client.sessionPoolStats().getMaxSize() * 2; i++) {
            Session session = client.createSession(Duration.ZERO).join().getValue();
            check(client).acquired(1).idle(0);
            session.close();
            check(client).acquired(0).idle(1);
        }

        client.close();
        check(client).acquired(0).idle(0);
    }

    @Test
    public void getOrCreateSessionAndReleaseMultipleTimes() {
        TableClient client = PooledTableClient.newClient(new DumpTableRpc())
                .sessionPoolSize(0, 5)
                .sessionMaxIdleTime(Duration.ofSeconds(2))
                .sessionKeepAliveTime(Duration.ofSeconds(5))
                .build();

        Session session = nextSession(client);
        check(client).acquired(1).idle(0);
        for (int i = 0; i < client.sessionPoolStats().getMaxSize() * 2; i++) {
            session.close();
            check(client).acquired(0).idle(1);
        }

        Assert.assertSame(session, nextSession(client));
        check(client).acquired(1).idle(0);

        client.close();
        check(client).acquired(1).idle(0);
        session.close();
    }

    @Test
    public void unavailableSessions() {
        TableClient client = PooledTableClient.newClient(new UnavailableTableRpc()).build();


        CompletableFuture<Result<Session>> f1 = client.createSession(Duration.ofMillis(50));
        Result<Session> r1 = f1.join();
        Assert.assertFalse(r1.isSuccess());
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, r1.getStatus().getCode());

        client.close();
    }

    @Test
    public void createSessionTimeout() throws InterruptedException {
        MockedTableRpc rpc = new MockedTableRpc(Clock.systemUTC());
        TableClient client = PooledTableClient.newClient(rpc).build();

        CompletableFuture<Result<Session>> f1 = client.createSession(Duration.ofMillis(50));
        Thread.sleep(100);

        Result<Session> r1 = f1.join();
        Assert.assertFalse(r1.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_DEADLINE_EXCEEDED, r1.getStatus().getCode());

        rpc.check().sessionRequests(1);
        rpc.nextCreateSession().completeSuccess();

        client.close();
        rpc.completeSessionDeleteRequests();
    }

    @Test
    public void createSessionException() throws InterruptedException {
        MockedTableRpc rpc = new MockedTableRpc(Clock.systemUTC());
        TableClient client = PooledTableClient.newClient(rpc).build();

        CompletableFuture<Result<Session>> f1 = client.createSession(Duration.ofMillis(50));
        rpc.check().sessionRequests(1);
        rpc.nextCreateSession().completeRuntimeException();

        Result<Session> r1 = f1.join();
        Assert.assertFalse(r1.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, r1.getStatus().getCode());

        client.close();
        rpc.completeSessionDeleteRequests();
    }

    private Session nextSession(TableClient client) {
        return client.createSession(Duration.ZERO).join().getValue();
    }

    private class TableClientChecker {
        private final SessionPoolStats stats;

        public TableClientChecker(TableClient client) {
            this.stats = client.sessionPoolStats();
        }

        public TableClientChecker size(int minSize, int maxSize) {
            Assert.assertEquals("Check pool min size", minSize, stats.getMinSize());
            Assert.assertEquals("Check pool max size", maxSize, stats.getMaxSize());
            return this;
        }

        public TableClientChecker idle(int size) {
            Assert.assertEquals("Check pool idle size", size, stats.getIdleCount());
            return this;
        }

        public TableClientChecker acquired(int size) {
            Assert.assertEquals("Check pool acquired size", size, stats.getAcquiredCount());
            return this;
        }

        public TableClientChecker pending(int size) {
            Assert.assertEquals("Check pool pending size", size, stats.getPendingAcquireCount());
            return this;
        }
    }

    private TableClientChecker check(TableClient pool) {
        return new TableClientChecker(pool);
    }

    private class DumpTableRpc extends MockedTableRpc {
        public DumpTableRpc() {
            super(Clock.systemUTC());
        }

        @Override
        public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
            YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
            CompletableFuture<Result<YdbTable.CreateSessionResult>> future = super.createSession(request, settings);
            nextCreateSession().completeSuccess();
            return future;
        }
    }

    private class UnavailableTableRpc extends MockedTableRpc {
        public UnavailableTableRpc() {
            super(Clock.systemUTC());
        }

        @Override
        public CompletableFuture<Result<YdbTable.CreateSessionResult>> createSession(
            YdbTable.CreateSessionRequest request, GrpcRequestSettings settings) {
            CompletableFuture<Result<YdbTable.CreateSessionResult>> future = super.createSession(request, settings);
            nextCreateSession().completeTransportUnavailable();
            return future;
        }
    }
}
