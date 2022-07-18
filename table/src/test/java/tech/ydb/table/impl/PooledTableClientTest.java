package tech.ydb.table.impl;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.Test;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.SessionSupplier;
import tech.ydb.table.TableClient;
import tech.ydb.table.TableRpcStub;
import tech.ydb.table.YdbTable;
import tech.ydb.table.impl.pool.MockedTableRpc;
import tech.ydb.table.rpc.TableRpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PooledTableClientTest {
    @Test(expected = IllegalArgumentException.class)
    public void testTableRpcAssert() {
        PooledTableClient.newClient(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSessionSizeAssert() {
        PooledTableClient.newClient(new DumpTableRpc())
                .sessionPoolSize(0, 0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKeepAliveTimeoutAssert() {
        PooledTableClient.newClient(new DumpTableRpc())
                .sessionKeepAliveTime(Duration.ofMillis(10))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxIdleTimeoutAssert() {
        PooledTableClient.newClient(new DumpTableRpc())
                .sessionMaxIdleTime(Duration.ofMillis(10))
                .build();
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
            Session session = client.createSession(Duration.ZERO).join()
                    .expect("cannot create session after " + i + " iterations");
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
        Assert.assertEquals(StatusCode.TRANSPORT_UNAVAILABLE, r1.getCode());
        
        client.close();
    }

    private Session nextSession(TableClient client) {
        return client.createSession(Duration.ZERO).join().expect("cannot create session");
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
        public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
            YdbTable.CreateSessionRequest request, long deadlineAfter) {
            CompletableFuture<Result<YdbTable.CreateSessionResponse>> future = super.createSession(request, deadlineAfter);
            nextCreateSession().completeSuccess();
            return future;
        }
    }

    private class UnavailableTableRpc extends MockedTableRpc {
        public UnavailableTableRpc() {
            super(Clock.systemUTC());
        }
        
        @Override
        public CompletableFuture<Result<YdbTable.CreateSessionResponse>> createSession(
            YdbTable.CreateSessionRequest request, long deadlineAfter) {
            CompletableFuture<Result<YdbTable.CreateSessionResponse>> future = super.createSession(request, deadlineAfter);
            nextCreateSession().completeTransportUnavailable();
            return future;
        }
    }
}
