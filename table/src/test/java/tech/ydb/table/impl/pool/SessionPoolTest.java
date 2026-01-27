package tech.ydb.table.impl.pool;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
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
    private final MockedTableRpc tableRpc = new MockedTableRpc(clock, scheduler);

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
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));

        check(pool).idle(0).acquired(0).pending(0).size(0, 2).totalClient(0, 0).totalServer(0, 0, 0, 0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f3 = pendingFuture(pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(0).pending(3).totalClient(0, 0).totalServer(2, 0, 0, 0);

        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(2).pending(1).totalClient(2, 0).totalServer(2, 2, 0, 0);

        Session s1 = futureIsReady(f1).getValue();
        Session s2 = futureIsReady(f2).getValue();
        futureIsPending(f3);

        s2.close();
        check(pool).idle(0).acquired(2).pending(0).totalClient(3, 1).totalServer(2, 2, 0, 0);

        Session s3 = futureIsReady(f3).getValue();
        Assert.assertEquals("Third future get the same session", s2, s3);

        s1.close();
        s3.close();

        check(pool).idle(2).acquired(0).pending(0).totalClient(3, 3).totalServer(2, 2, 0, 0);
        pool.close();

        check(pool).idle(0).acquired(0).pending(0).totalClient(3, 3).totalServer(2, 2, 0, 2);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void increaseLimitsTest() {
        int wsize = WaitingQueue.WAITINGS_LIMIT_FACTOR;
        SessionPool pool = new SessionPool(clock, tableRpc, true, SessionPoolOptions.DEFAULT.withSize(0, 1));

        check(pool).idle(0).acquired(0).pending(0).size(0, 1).totalClient(0, 0).totalServer(0, 0, 0, 0);
        Deque<CompletableFuture<Result<Session>>> futures = new ArrayDeque<>();

        // one request we can make directly
        futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));
        tableRpc.check().sessionRequests(1);
        check(pool).idle(0).acquired(0).pending(1).size(0, 1).totalClient(0, 0).totalServer(1, 0, 0, 0);

        // and WAITINGS_LIMIT_FACTOR extra requests will be in waitings queue
        for (int i = 0; i < wsize; i += 1) {
            futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));
        }
        tableRpc.check().sessionRequests(1);
        check(pool).idle(0).acquired(0).pending(1 + wsize).size(0, 1).totalClient(0, 0).totalServer(1, 0, 0, 0);

        // next request returns error
        resultIsWrong(pool.acquire(TIMEOUT), StatusCode.CLIENT_RESOURCE_EXHAUSTED);

        // increase pool limit
        pool.updateMaxSize(2);
        tableRpc.check().sessionRequests(2);
        check(pool).idle(0).acquired(0).pending(1 + wsize).size(0, 2).totalClient(0, 0).totalServer(2, 0, 0, 0);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(1).pending(wsize).size(0, 2).totalClient(1, 0).totalServer(2, 1, 0, 0);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(2).pending(-1 + wsize).size(0, 2).totalClient(2, 0).totalServer(2, 2, 0, 0);

        while (!futures.isEmpty()) {
            futureIsReady(futures.removeFirst()).getValue().close();
        }

        check(pool).idle(2).acquired(0).pending(0).size(0, 2).totalClient(1 + wsize, 1 + wsize).totalServer(2, 2, 0, 0);
        pool.close();
        check(pool).idle(0).acquired(0).pending(0).size(0, 2).totalClient(1 + wsize, 1 + wsize).totalServer(2, 2, 0, 2);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void decreaseLimitsTest() {
        int wsize = WaitingQueue.WAITINGS_LIMIT_FACTOR;
        SessionPool pool = new SessionPool(clock, tableRpc, true, SessionPoolOptions.DEFAULT.withSize(0, 2));

        check(pool).idle(0).acquired(0).pending(0).size(0, 2).totalClient(0, 0).totalServer(0, 0, 0, 0);
        Deque<CompletableFuture<Result<Session>>> futures = new ArrayDeque<>();

        // two request we can make directly
        futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));
        futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));
        tableRpc.check().sessionRequests(2);
        check(pool).idle(0).acquired(0).pending(2).size(0, 2).totalClient(0, 0).totalServer(2, 0, 0, 0);

        // and WAITINGS_LIMIT_FACTOR extra requests will be in waitings queue
        for (int i = 0; i < 2 * wsize; i += 1) {
            futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));
        }
        tableRpc.check().sessionRequests(2);
        check(pool).idle(0).acquired(0).pending(2 + 2 * wsize).size(0, 2).totalClient(0, 0).totalServer(2, 0, 0, 0);

        // next request returns error
        resultIsWrong(pool.acquire(TIMEOUT), StatusCode.CLIENT_RESOURCE_EXHAUSTED);

        // decrease pool limit
        pool.updateMaxSize(1);

        tableRpc.check().sessionRequests(2);
        check(pool).idle(0).acquired(0).pending(2 + 2 * wsize).size(0, 1).totalClient(0, 0).totalServer(2, 0, 0, 0);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(1).pending(1 + 2 * wsize).size(0, 1).totalClient(1, 0).totalServer(2, 1, 0, 0);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(2).pending(2 * wsize).size(0, 1).totalClient(2, 0).totalServer(2, 2, 0, 0);

        // new requests will be rejected
        resultIsWrong(pool.acquire(TIMEOUT), StatusCode.CLIENT_RESOURCE_EXHAUSTED);
        for (int i = 0; i < wsize; i += 1) {
            resultIsWrong(pool.acquire(TIMEOUT), StatusCode.CLIENT_RESOURCE_EXHAUSTED);

            check(pool).idle(0).acquired(2).pending(2 * wsize - i).size(0, 1)
                    .totalClient(2 + i, i).totalServer(2, 2, 0, 0);
            futureIsReady(futures.removeFirst()).getValue().close();
        }

        check(pool).idle(0).acquired(2).pending(wsize).size(0, 1).totalClient(2 + wsize, wsize).totalServer(2, 2, 0, 0);
        resultIsWrong(pool.acquire(TIMEOUT), StatusCode.CLIENT_RESOURCE_EXHAUSTED);
        futureIsReady(futures.removeFirst()).getValue().close();

        check(pool).idle(0).acquired(2).pending(wsize - 1).size(0, 1)
                .totalClient(3 + wsize, 1 + wsize).totalServer(2, 2, 0, 0);
        futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));

        for (int i = 0; i < wsize; i += 1) {
            check(pool).idle(0).acquired(2).pending(wsize - i).size(0, 1)
                    .totalClient(3 + wsize + i, wsize + 1 + i).totalServer(2, 2, 0, 0);
            futureIsReady(futures.removeFirst()).getValue().close();
        }

        check(pool).idle(0).acquired(2).pending(0).size(0, 1)
                .totalClient(3 + 2 * wsize, 1 + 2 * wsize).totalServer(2, 2, 0, 0);
        futureIsReady(futures.removeFirst()).getValue().close();

        check(pool).idle(0).acquired(1).pending(0).size(0, 1)
                .totalClient(3 + 2 * wsize, 2 + 2 * wsize).totalServer(2, 2, 0, 1);
        futures.addLast(pendingFuture(pool.acquire(TIMEOUT)));

        check(pool).idle(0).acquired(1).pending(1).size(0, 1)
                .totalClient(3 + 2 * wsize, 2 + 2 * wsize).totalServer(2, 2, 0, 1);

        futureIsReady(futures.removeFirst()).getValue().close();
        futureIsReady(futures.removeFirst()).getValue().close();

        check(pool).idle(1).acquired(0).pending(0).size(0, 1)
                .totalClient(4 + 2 * wsize, 4 + 2 * wsize).totalServer(2, 2, 0, 1);

        pool.close();

        check(pool).idle(0).acquired(0).pending(0).size(0, 1)
                .totalClient(4 + 2 * wsize, 4 + 2 * wsize).totalServer(2, 2, 0, 2);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void createSessionWithErrorTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 3));

        check(pool).idle(0).acquired(0).pending(0).size(0, 3).totalClient(0, 0).totalServer(0, 0, 0, 0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f3 = pendingFuture(pool.acquire(TIMEOUT));

        tableRpc.check().sessionRequests(3);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeOverloaded();
        tableRpc.nextCreateSession().completeRuntimeException();

        tableRpc.check().sessionRequests(0);
        check(pool).idle(0).acquired(1).pending(0).size(0, 3).totalClient(1, 0).totalServer(3, 1, 1, 0);

        Session s1 = futureIsReady(f1).getValue();
        resultIsWrong(f2, StatusCode.OVERLOADED);
        resultIsWrong(f3, StatusCode.CLIENT_INTERNAL_ERROR);

        s1.close();
        check(pool).idle(1).acquired(0).pending(0).size(0, 3).totalClient(1, 1).totalServer(3, 1, 1, 0);
        pool.close();
        check(pool).idle(0).acquired(0).pending(0).size(0, 3).totalClient(1, 1).totalServer(3, 1, 1, 1);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void createSessionShutdownHintTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 3));

        check(pool).idle(0).acquired(0).pending(0).size(0, 3).totalClient(0, 0).totalServer(0, 0, 0, 0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f3 = pendingFuture(pool.acquire(TIMEOUT));

        tableRpc.check().sessionRequests(3);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        Session s1 = futureIsReady(f1).getValue();
        Session s2 = futureIsReady(f2).getValue();
        Session s3 = futureIsReady(f3).getValue();

        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r3 = s3.executeDataQuery("SELECT 1;", TxControl.onlineRo());

        tableRpc.check().executeDataRequests(3);
        tableRpc.nextExecuteDataQuery().completeSuccessWithShutdownHook();
        tableRpc.nextExecuteDataQuery().completeSuccessWithShutdownHook();
        tableRpc.nextExecuteDataQuery().completeSuccessWithShutdownHook();

        check(pool).idle(0).acquired(3).pending(0).size(0, 3).totalClient(3, 0).totalServer(3, 3, 0, 0);

        futureIsReady(r1);
        futureIsReady(r2);
        futureIsReady(r3);

        s3.close(); // will be shutdown

        check(pool).idle(0).acquired(2).pending(0).size(0, 3).totalClient(3, 1).totalServer(3, 3, 0, 1);
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();

        r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());

        tableRpc.check().executeDataRequests(2);
        tableRpc.nextExecuteDataQuery().completeSuccessWithShutdownHook();
        tableRpc.nextExecuteDataQuery().completeSuccess();

        futureIsReady(r1);
        futureIsReady(r2);

        s1.close(); // will be shutdown
        s2.close(); // will be shutdown

        check(pool).idle(0).acquired(0).pending(0).size(0, 3).totalClient(3, 3).totalServer(3, 3, 0, 3);
        tableRpc.check().deleteSessionRequests(2);
        tableRpc.nextDeleteSession().completeSuccess();
        tableRpc.nextDeleteSession().completeSuccess();

        pool.close();
    }

    @Test
    public void sessionUseAfterClosingTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));

        check(pool).idle(0).acquired(0).pending(0).size(0, 2).totalClient(0, 0).totalServer(0, 0, 0, 0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));

        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        Session s1 = futureIsReady(f1).getValue();
        Session s2 = futureIsReady(f2).getValue();

        check(pool).idle(0).acquired(2).pending(0).size(0, 2).totalClient(2, 0).totalServer(2, 2, 0, 0);

        s1.close();
        s2.close();

        check(pool).idle(2).acquired(0).pending(0).size(0, 2).totalClient(2, 2).totalServer(2, 2, 0, 0);

        // try to use session after closing - must be fixed in further versions
        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        CompletableFuture<Result<DataQueryResult>> r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());

        tableRpc.check().executeDataRequests(2);
        tableRpc.nextExecuteDataQuery().completeSuccess();
        tableRpc.nextExecuteDataQuery().completeSuccess();

        futureIsReady(r1);
        futureIsReady(r2);

        check(pool).idle(2).acquired(0).pending(0).size(0, 2).totalClient(2, 2).totalServer(2, 2, 0, 0);

        s1 = futureIsReady(pool.acquire(TIMEOUT)).getValue();
        s2 = futureIsReady(pool.acquire(TIMEOUT)).getValue();
        check(pool).idle(0).acquired(2).pending(0).size(0, 2).totalClient(4, 2).totalServer(2, 2, 0, 0);

        s1.close();
        s2.close();
        check(pool).idle(2).acquired(0).pending(0).size(0, 2).totalClient(4, 4).totalServer(2, 2, 0, 0);

        // broke session inside the pool
        r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());

        tableRpc.check().executeDataRequests(2);
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();

        futureIsReady(r1);
        futureIsReady(r2);

        check(pool).idle(2).acquired(0).pending(0).size(0, 2).totalClient(4, 4).totalServer(2, 2, 0, 0);

        // next session acquire removed broken sessions
        f1 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1).size(0, 2).totalClient(4, 4).totalServer(3, 2, 0, 2);
        tableRpc.check().sessionRequests(1);
        tableRpc.check().deleteSessionRequests(2);
        tableRpc.nextDeleteSession().completeSuccess();
        tableRpc.nextDeleteSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(1).pending(0).size(0, 2).totalClient(5, 4).totalServer(3, 3, 0, 2);
        futureIsReady(f1).getValue().close();

        check(pool).idle(1).acquired(0).pending(0).size(0, 2).totalClient(5, 5).totalServer(3, 3, 0, 2);
        pool.close();
        check(pool).idle(0).acquired(0).pending(0).size(0, 2).totalClient(5, 5).totalServer(3, 3, 0, 3);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void createSessionTimeoutTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));

        check(pool).idle(0).acquired(0).pending(0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        scheduler.runTasksTo(clock.instant().plus(TIMEOUT));

        resultIsWrong(f1, StatusCode.CLIENT_DEADLINE_EXPIRED);

        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(1).acquired(0).pending(0);

        Session s1 = readyFuture(() -> pool.acquire(TIMEOUT)).getValue();
        check(pool).idle(0).acquired(1).pending(0);

        s1.close();
        pool.close();
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void canceledSessionsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));

        check(pool).idle(0).acquired(0).pending(0);

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        f1.cancel(true);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeTransportUnavailable();

        check(pool).idle(0).acquired(0).pending(0);

        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        f2.cancel(true);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(1).acquired(0).pending(0);
        pool.close();
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void sessionDataQueryErrorsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));
        check(pool).idle(0).acquired(0).pending(0);

        // Test TRANSPORT_UNAVAILABLE on executeDataQuery - session will be removed from pool
        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();

        Session s1 = futureIsReady(f1).getValue();

        CompletableFuture<Result<DataQueryResult>> r1 = s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();
        resultIsWrong(r1, StatusCode.TRANSPORT_UNAVAILABLE);

        s1.close();
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();

        // Test OVERLOADED on executeDataQuery - session will be returned to pool
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();

        Session s2 = futureIsReady(f2).getValue();

        CompletableFuture<Result<DataQueryResult>> r2 = s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeOverloaded();
        resultIsWrong(r2, StatusCode.OVERLOADED);

        s2.close();
        tableRpc.check().deleteSessionRequests(0);

        check(pool).idle(1).acquired(0).pending(0);
        pool.close();
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void sessionDeleteErrorsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT.withSize(0, 2));

        // Create session1 request
        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(1).pending(0);

        // Get error of the data query request
        Session s1 = futureIsReady(f1).getValue();
        s1.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();

        // Close session 1 with transport unavailable
        s1.close();
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeTransportUnavailable();
        check(pool).idle(0).acquired(0).pending(0);

        // Create session2 request
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        check(pool).idle(0).acquired(0).pending(1);

        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();
        check(pool).idle(0).acquired(1).pending(0);

        // Get error of the data query request
        Session s2 = futureIsReady(f2).getValue();
        s2.executeDataQuery("SELECT 1;", TxControl.onlineRo());
        tableRpc.check().executeDataRequests(1);
        tableRpc.nextExecuteDataQuery().completeTransportUnavailable();

        // Close session 2 with runtime exception
        s2.close();
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeRuntimeException();
        check(pool).idle(0).acquired(0).pending(0);

        pool.close();
        tableRpc.check()
                .hasNoSessions()
                .hasNoPendingRequests();
    }

    @Test
    public void removeIdleSessionsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT
                        .withSize(2, 5)
                        .withKeepAliveTimeMillis(5000)
                        .withMaxIdleTimeMillis(1000));

        check(pool).idle(0).acquired(0).pending(0);

        Instant now = clock.instant();

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f3 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f4 = pendingFuture(pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(0).pending(4);
        tableRpc.check().sessionRequests(4);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        Session s1 = futureIsReady(f1).getValue();
        Session s2 = futureIsReady(f2).getValue();
        Session s3 = futureIsReady(f3).getValue();
        Session s4 = futureIsReady(f4).getValue();

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
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();

        scheduler.runTasksTo(now.plusMillis(2000));
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();
        check(pool).idle(2).acquired(0).pending(0);

        Session s5 = readyFuture(() -> pool.acquire(Duration.ZERO)).getValue();
        Session s6 = readyFuture(() -> pool.acquire(Duration.ZERO)).getValue();
        check(pool).idle(0).acquired(2).pending(0);

        Assert.assertEquals("Check acquire order", s5, s4);
        Assert.assertEquals("Check acquire order", s6, s2);

        s5.close();
        s6.close();

        pool.close();

        tableRpc.check().deleteSessionRequests(2);
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void keepAliveSessionsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT
                        .withSize(2, 3)
                        .withKeepAliveTimeMillis(1000)
                        .withMaxIdleTimeMillis(2500));

        check(pool).idle(0).acquired(0).pending(0);

        Instant now = clock.instant();

        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f3 = pendingFuture(pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(0).pending(3);
        tableRpc.check().sessionRequests(3);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(3).pending(0);

        futureIsReady(f1).getValue().close();
        futureIsReady(f2).getValue().close();
        futureIsReady(f3).getValue().close();

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
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();
        check(pool).idle(2).acquired(0).pending(0);

        scheduler.runTasksTo(now.plusMillis(2900));
        scheduler.runTasksTo(now.plusMillis(3100), () -> {
            tableRpc.check().keepAlives(1);
            tableRpc.nextKeepAlive().completeBusy();
        });
        tableRpc.check().deleteSessionRequests(0);

        scheduler.runTasksTo(now.plusMillis(3300), () -> {
            tableRpc.check().keepAlives(1);
            tableRpc.nextKeepAlive().completeReady();
        });
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();

        check(pool).idle(1).acquired(0).pending(0);

        pool.close();
        tableRpc.completeSessionDeleteRequests();
    }

    @Test
    public void wrongKeepAliveSessionsTest() {
        SessionPool pool = new SessionPool(clock, tableRpc, true,
                SessionPoolOptions.DEFAULT
                        .withSize(2, 3)
                        .withKeepAliveTimeMillis(1000)
                        .withMaxIdleTimeMillis(2500));

        check(pool).idle(0).acquired(0).pending(0);

        Instant now = clock.instant();

        // Fill the pool with 2 session
        CompletableFuture<Result<Session>> f1 = pendingFuture(pool.acquire(TIMEOUT));
        CompletableFuture<Result<Session>> f2 = pendingFuture(pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(0).pending(2);
        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(2).pending(0);

        futureIsReady(f1).getValue().close();
        futureIsReady(f2).getValue().close();

        check(pool).idle(2).acquired(0).pending(0);

        // Go to future to run keep alive request
        scheduler.runTasksTo(now.plusMillis(900), () -> {
            tableRpc.check().keepAlives(0);
        });
        scheduler.runTasksTo(now.plusMillis(1100), () -> {
            tableRpc.check().keepAlives(2); // max keep alive count
            tableRpc.nextKeepAlive().completeReady();
            tableRpc.nextKeepAlive().completeBusy();
        });

        check(pool).idle(2).acquired(0).pending(0);

        // Now we get the broken session firstly, close it and retry get the good session
        tableRpc.check().deleteSessionRequests(0);
        Session s1 = futureIsReady(pool.acquire(TIMEOUT)).getValue();
        tableRpc.check().deleteSessionRequests(1);
        tableRpc.nextDeleteSession().completeSuccess();

        // Because the broken session was closed, second request is pending
        f2 = pendingFuture(pool.acquire(TIMEOUT));

        check(pool).idle(0).acquired(1).pending(1);
        tableRpc.check().sessionRequests(1);
        tableRpc.nextCreateSession().completeSuccess();

        check(pool).idle(0).acquired(2).pending(0);
        s1.close();
        futureIsReady(f2).getValue().close();

        check(pool).idle(2).acquired(0).pending(0);
        // Go to future to run keep alive request
        scheduler.runTasksTo(now.plusMillis(1900), () -> {
            tableRpc.check().keepAlives(0);
        });
        scheduler.runTasksTo(now.plusMillis(2100), () -> {
            tableRpc.check().keepAlives(2); // max keep alive count
            tableRpc.nextKeepAlive().completeBusy();
            tableRpc.nextKeepAlive().completeBusy();
        });

        // When all session are broken - delete them and create new create request
        tableRpc.check().deleteSessionRequests(0);
        check(pool).idle(2).acquired(0).pending(0);

        f1 = pendingFuture(pool.acquire(TIMEOUT));
        tableRpc.check().deleteSessionRequests(2);
        check(pool).idle(0).acquired(0).pending(1);

        f2 = pendingFuture(pool.acquire(TIMEOUT));
        tableRpc.check().deleteSessionRequests(2);
        check(pool).idle(0).acquired(0).pending(2);

        tableRpc.check().sessionRequests(2);
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.nextCreateSession().completeSuccess();
        tableRpc.completeSessionDeleteRequests();

        check(pool).idle(0).acquired(2).pending(0);
        futureIsReady(f1).getValue().close();
        futureIsReady(f2).getValue().close();

        check(pool).idle(2).acquired(0).pending(0);

        // Go to future to run keep alive request
        scheduler.runTasksTo(now.plusMillis(2900), () -> {
            tableRpc.check().keepAlives(0);
        });
        scheduler.runTasksTo(now.plusMillis(3100), () -> {
            tableRpc.check().keepAlives(2); // max keep alive count
            tableRpc.nextKeepAlive().completeBusy();
            tableRpc.nextKeepAlive().completeBusy();
        });

        check(pool).idle(2).acquired(0).pending(0);
        tableRpc.check().deleteSessionRequests(0);

        // Go to future to run keep alive request
        scheduler.runTasksTo(now.plusMillis(3300), () -> {
            tableRpc.check().keepAlives(0);
        });
        tableRpc.check().deleteSessionRequests(2);
        tableRpc.completeSessionDeleteRequests();

        check(pool).idle(0).acquired(0).pending(0);
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

        public PoolChecker totalClient(long acquired, long released) {
            Assert.assertEquals("Check pool total acquired count", acquired, stats.getAcquiredTotal());
            Assert.assertEquals("Check pool total released count", released, stats.getReleasedTotal());
            return this;
        }

        public PoolChecker totalServer(long requested, long created, long failed, long deleted) {
            Assert.assertEquals("Check pool total requested count", requested, stats.getRequestedTotal());
            Assert.assertEquals("Check pool total created count", created, stats.getCreatedTotal());
            Assert.assertEquals("Check pool total failed count", failed, stats.getFailedTotal());
            Assert.assertEquals("Check pool total deleted count", deleted, stats.getDeletedTotal());
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

    private<T> void resultIsWrong(CompletableFuture<Result<T>> future, StatusCode code) {
        Result<T> result = futureIsReady(future);
        Assert.assertFalse("Check data query result", result.isSuccess());
        Assert.assertEquals("Check  data query result status", code, result.getStatus().getCode());
    }
}
