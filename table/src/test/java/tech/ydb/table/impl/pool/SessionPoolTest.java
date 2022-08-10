package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.SessionPoolStats;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.transaction.TxControl;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class SessionPoolTest extends FutureHelper {
    private final Logger logger = LoggerFactory.getLogger(SessionPoolTest.class);

    private final static Duration TIMEOUT = Duration.ofMillis(50);

    private final MockedClock clock = MockedClock.create(ZoneId.of("UTC"));
    private final MockedScheduler scheduler = new MockedScheduler(clock);
    private final MockedTableRpc tableRpc = new MockedTableRpc(clock);
    
    @Before
    public void setup() {
        clock.reset(Instant.parse("2022-07-01T00:00:00.000Z"));
    }
    
    @After
    public void close() {
        scheduler.shutdown();

        scheduler.check()
                .isClosed()
                .hasNoTasks();
        tableRpc.check()
                .hasNoSessions()
                .hasNoPendingRequests();
    }

    @Test
    public void baseTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        
        check(pool).idle(0).acquired(0).pending(0).size(0, 2);
        
        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f2 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f3 = pendingFuture(() -> pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(0).pending(3);
        
        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        
        check(pool).idle(0).acquired(2).pending(1);

        Session s1 = futureIsReady(f1);
        Session s2 = futureIsReady(f2);
        futureIsPending(f3);
        
        s2.close();
        check(pool).idle(0).acquired(2).pending(0);

        Session s3 = futureIsReady(f3);
        Assert.assertEquals("Third future get the same session", s2, s3);
        
        s1.close();
        s3.close();

        check(pool).idle(2).acquired(0).pending(0);
        pool.close();
    }
    
    @Test
    public void createSessionWithErrorTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f2 = pendingFuture(() -> pool.acquire(TIMEOUT));

        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeOverloaded();

        tableRpc.check().sessionRequests(0);
        check(pool).idle(0).acquired(1).pending(0);

        Session s1 = futureIsReady(f1);
        futureIsExceptionally(f2, "Cannot get value, code: OVERLOADED");
        
        s1.close();
        pool.close();
    }
    
    @Test
    public void createSessionTimeoutTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        scheduler.runTasksTo(clock.instant().plus(TIMEOUT));

        futureIsExceptionally(f1, "deadline was expired");
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(1).acquired(0).pending(0);
        
        Session s1 = readyFuture(() -> pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(1).pending(0);

        s1.close();
        pool.close();
    }

    @Test
    public void sessionUsageErrorTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();
        
        Session s1 = futureIsReady(f1);
        
        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();
        resultIsWrong(r1, StatusCode.TRANSPORT_UNAVAILABLE);
        
        s1.close();

        check(pool).idle(0).acquired(0).pending(0);
        pool.close();
    }

    @Test
    public void sessionUsageNonErrorTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();
        
        Session s1 = futureIsReady(f1);
        
        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeOverloaded();
        resultIsWrong(r1, StatusCode.OVERLOADED);
        
        s1.close();

        check(pool).idle(1).acquired(0).pending(0);
        pool.close();
    }

    @Test
    public void removeIdleSessionsTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT
                        .withSize(2, 5)
                        .withKeepAliveTimeMillis(5000)
                        .withMaxIdleTimeMillis(1000));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        Instant now = clock.instant();

        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f2 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f3 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f4 = pendingFuture(() -> pool.acquire(TIMEOUT));
        
        check(pool).idle(0).acquired(0).pending(4);
        tableRpc.check().sessionRequests(4);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        
        Session s1 = futureIsReady(f1);
        Session s2 = futureIsReady(f2);
        Session s3 = futureIsReady(f3);
        Session s4 = futureIsReady(f4);
        
        CompletableFuture<Result<DataQueryResult>> r3 = s3.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r4 = s4.executeDataQuery("SELECT 1;", TxControl.onlineRo());

        tableRpc.check().executeDataRequests(4);

        clock.goToFuture(now.plusMillis(200));
        tableRpc.nextExecuteDataQuery().completeSuccess();
        resultIsGood(r3);
        s3.close();

        clock.goToFuture(now.plusMillis(400));
        tableRpc.nextExecuteDataQuery().completeSuccess();
        resultIsGood(r1);
        s1.close();

        clock.goToFuture(now.plusMillis(600));
        tableRpc.nextExecuteDataQuery().completeSuccess();
        resultIsGood(r2);
        s2.close();

        clock.goToFuture(now.plusMillis(800));
        tableRpc.nextExecuteDataQuery().completeSuccess();
        resultIsGood(r4);
        s4.close();
        
        tableRpc.check().executeDataRequests(0);
        check(pool).idle(4).acquired(0).pending(0);
        
        scheduler.runTasksTo(now.plusMillis(1000));
        check(pool).idle(4).acquired(0).pending(0);

        scheduler.runTasksTo(now.plusMillis(1500));
        check(pool).idle(3).acquired(0).pending(0);
        
        scheduler.runTasksTo(now.plusMillis(2000));
        check(pool).idle(2).acquired(0).pending(0);
        
        Session s5 = readyFuture(() -> pool.acquire(Duration.ZERO));
        Session s6 = readyFuture(() -> pool.acquire(Duration.ZERO));
        check(pool).idle(0).acquired(2).pending(0);
        
        Assert.assertEquals("Check acquire order", s5, s4);
        Assert.assertEquals("Check acquire order", s6, s2);
        
        s5.close();
        s6.close();

        pool.close();
    }

    @Test
    public void keepAliveSessionsTest() {
        SessionPool pool = new SessionPool(scheduler, clock, tableRpc, true, 
                SessionPoolOptions.DEFAULT
                        .withSize(2, 3)
                        .withKeepAliveTimeMillis(1000)
                        .withMaxIdleTimeMillis(2500));
        
        check(pool).idle(0).acquired(0).pending(0);
        
        Instant now = clock.instant();

        CompletableFuture<Session> f1 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f2 = pendingFuture(() -> pool.acquire(TIMEOUT));
        CompletableFuture<Session> f3 = pendingFuture(() -> pool.acquire(TIMEOUT));
        
        check(pool).idle(0).acquired(0).pending(3);
        tableRpc.check().sessionRequests(3);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(3).pending(0);
        
        futureIsReady(f1).close();
        futureIsReady(f2).close();
        futureIsReady(f3).close();

        check(pool).idle(3).acquired(0).pending(0);

        scheduler.runTasksTo(now.plusMillis(900));
        tableRpc.check().hasNoKeepAlive();

        scheduler.runTasksTo(now.plusMillis(1100), () -> {
            tableRpc.check().keepAlives(2); // max keep alive count
            tableRpc.nextKeepAlive().completeReady();
            tableRpc.nextKeepAlive().completeReady();
        });

        scheduler.runTasksTo(now.plusMillis(1300), () -> {
            tableRpc.check().keepAlives(1); 
            tableRpc.nextKeepAlive().completeReady();
        });

        scheduler.runTasksTo(now.plusMillis(2000));
        tableRpc.check().hasNoKeepAlive();

        scheduler.runTasksTo(now.plusMillis(2100), () -> {
            tableRpc.check().keepAlives(2); // max keep alive count
            tableRpc.nextKeepAlive().completeReady();
            tableRpc.nextKeepAlive().completeReady();
        });
        scheduler.runTasksTo(now.plusMillis(2300), () -> {
            tableRpc.check().keepAlives(1); 
            tableRpc.nextKeepAlive().completeReady();
        });
        check(pool).idle(3).acquired(0).pending(0);

        scheduler.runTasksTo(now.plusMillis(2500));
        tableRpc.check().hasNoKeepAlive();
        check(pool).idle(2).acquired(0).pending(0);

        scheduler.runTasksTo(now.plusMillis(2900));
        scheduler.runTasksTo(now.plusMillis(3100), () -> {
            tableRpc.check().keepAlives(1);
            tableRpc.nextKeepAlive().completeBusy();
        });
        scheduler.runTasksTo(now.plusMillis(3300), () -> {
            tableRpc.check().keepAlives(1);
            tableRpc.nextKeepAlive().completeReady();
        });

        check(pool).idle(1).acquired(0).pending(0);

        pool.close();
    }

    private class PoolChecker {
        private final SessionPoolStats stats;

        public PoolChecker(SessionPool pool) {
            this.stats = pool.stats();
        }
        
        public PoolChecker size(int minSize, int maxSize) {
            logger.trace("pool stats {}", stats.toString());

            Assert.assertEquals("Check pool min size", minSize, stats.getMinSize());
            Assert.assertEquals("Check pool max size", maxSize, stats.getMaxSize());
            return this;
        }
        
        public PoolChecker idle(int size) {
            Assert.assertEquals("Check pool idle size", size, stats.getIdleCount());
            return this;
        }

        public PoolChecker acquired(int size) {
            Assert.assertEquals("Check pool acquired size", size, stats.getAcquiredCount());
            return this;
        }
        
        public PoolChecker pending(int size) {
            Assert.assertEquals("Check pool pending size", size, stats.getPendingAcquireCount());
            return this;
        }
    }
    
    private PoolChecker check(SessionPool pool) {
        return new PoolChecker(pool);
    }

    private DataQueryResult resultIsGood(CompletableFuture<Result<DataQueryResult>> future) {
        Result<DataQueryResult> result = futureIsReady(future);
        Assert.assertTrue("Check data query result", result.isSuccess());
        Assert.assertEquals("Check  data query result status", StatusCode.SUCCESS, result.getStatus().getCode());
        return result.getValue();
    }
    
    private void resultIsWrong(CompletableFuture<Result<DataQueryResult>> future, StatusCode code) {
        Result<DataQueryResult> result = futureIsReady(future);
        Assert.assertFalse("Check data query result", result.isSuccess());
        Assert.assertEquals("Check  data query result status", code, result.getStatus().getCode());
    }
}
