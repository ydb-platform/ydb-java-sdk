package tech.ydb.table;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.table.impl.PooledTableClient;
import tech.ydb.table.impl.pool.FutureHelper;
import tech.ydb.table.impl.pool.MockedTableRpc;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.transaction.TxControl;


/**
 * @author Sergey Polovko
 */
public class SessionRetryContextTest extends FutureHelper  {
    private static final Status NOT_FOUND = Status.of(StatusCode.NOT_FOUND);
    private static final Status SCHEME_ERROR = Status.of(StatusCode.SCHEME_ERROR);
    private static final Status SESSION_BUSY = Status.of(StatusCode.SESSION_BUSY);
    private static final Status TRANSPORT_UNAVAILABLE = Status.of(StatusCode.TRANSPORT_UNAVAILABLE);
    private static final Status CANCELLED = Status.of(StatusCode.CANCELLED);
    private static final Status OVERLOADED = Status.of(StatusCode.OVERLOADED);
    private static final Status CLIENT_RESOURCE_EXHAUSTED = Status.of(StatusCode.CLIENT_RESOURCE_EXHAUSTED);

    private static final Duration TEN_MILLIS = Duration.ofMillis(10);
    private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);
    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static <T> CompletableFuture<T> completedFuture(T value) {
        return CompletableFuture.completedFuture(value);
    }

    @AfterClass
    public static void cleanUp() {
        scheduler.shutdown();
    }

    @Test
    public void successSession_successResult() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .build();

        AtomicInteger cnt = new AtomicInteger();
        Result<String> result = ctx.supplyResult(session -> {
            cnt.incrementAndGet();
            return completedFuture(Result.success("done"));
        }).join();

        Assert.assertEquals(1, cnt.get());
        Assert.assertEquals(Result.success("done"), result);
    }

    @Test
    public void successSession_failedResult() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .backoffSlot(TEN_MILLIS)
            .build();

        // not retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Result<Object> result = ctx.supplyResult(session -> {
                cnt.incrementAndGet();
                return completedFuture(Result.fail(CANCELLED));
            }).join();

            Assert.assertEquals(1, cnt.get());
            Assert.assertEquals(Result.fail(CANCELLED), result);
        }

        // retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Result<Object> result = ctx.supplyResult(session -> {
                cnt.incrementAndGet();
                return completedFuture(Result.fail(OVERLOADED));
            }).join();

            Assert.assertEquals(3, cnt.get());
            Assert.assertEquals(Result.fail(OVERLOADED), result);
        }
    }

    @Test
    public void successSession_exceptionResult() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .backoffSlot(TEN_MILLIS)
            .retryNotFound(true)
            .build();

        // not retryable exception
        {
            AtomicInteger cnt = new AtomicInteger();
            try {
                ctx.supplyResult(session -> {
                    cnt.incrementAndGet();
                    throw new RuntimeException("some error message");
                }).join();
                Assert.fail("expected exception not thrown");
            } catch (Throwable t) {
                Throwable cause = FutureTools.unwrapCompletionException(t);
                Assert.assertTrue(cause instanceof RuntimeException);
                Assert.assertEquals(1, cnt.get());
                Assert.assertEquals("some error message", cause.getMessage());
            }
        }

        // retryable exception
        {
            AtomicInteger cnt = new AtomicInteger();
            try {
                ctx.supplyResult(session -> {
                    cnt.incrementAndGet();
                    Result.fail(NOT_FOUND).getValue();
                    return null;
                }).join();
                Assert.fail("expected exception not thrown");
            } catch (Throwable t) {
                Throwable cause = FutureTools.unwrapCompletionException(t);
                Assert.assertTrue(cause instanceof UnexpectedResultException);
                Assert.assertEquals(3, cnt.get());
                Assert.assertEquals("Cannot get value, code: NOT_FOUND", cause.getMessage());
            }
        }
    }

    @Test
    public void successSession_successStatus() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .build();

        AtomicInteger cnt = new AtomicInteger();
        Status status = ctx.supplyStatus(session -> {
            cnt.incrementAndGet();
            return completedFuture(Status.SUCCESS);
        }).join();

        Assert.assertEquals(1, cnt.get());
        Assert.assertEquals(Status.SUCCESS, status);
    }

    @Test
    public void successSession_failedStatus() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .backoffSlot(TEN_MILLIS)
            .build();

        // not retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(SCHEME_ERROR);
            }).join();

            Assert.assertEquals(1, cnt.get());
            Assert.assertEquals(SCHEME_ERROR, status);
        }

        // retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(OVERLOADED);
            }).join();

            Assert.assertEquals(3, cnt.get());
            Assert.assertEquals(OVERLOADED, status);
        }
    }

    @Test
    public void failedSession_retryable() {
        // one session creation fail, but retryable. Slow backoff
        List<StatusCode> slowBackoffCodes = Arrays.asList(
                StatusCode.OVERLOADED,
                StatusCode.CLIENT_RESOURCE_EXHAUSTED
        );
        for(StatusCode statusCode : slowBackoffCodes) {
            FailSupplier sessionSupplier = new FailSupplier(1, statusCode);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
                .maxRetries(2)
                .backoffSlot(TEN_MILLIS)
                .fastBackoffSlot(TEN_SECONDS)
                .build();

            AtomicInteger cnt = new AtomicInteger();
            Instant startTime = Instant.now();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();

            Duration timePassed = Duration.between(startTime, Instant.now());
            Assert.assertTrue(String.format("Code: %s - Wrong timeout between retries", statusCode.toString()),
                    timePassed.compareTo(FIVE_SECONDS) < 0);

            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 2, sessionSupplier.getRequestsCount());
            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 1, cnt.get());
            Assert.assertEquals(Status.SUCCESS, status);
        }

        // one session creation fail, but retryable. Fast backoff
        List<StatusCode> fastBackoffCodes = Arrays.asList(
                StatusCode.ABORTED,
                StatusCode.SESSION_BUSY,
                StatusCode.UNAVAILABLE
        );
        for(StatusCode statusCode : fastBackoffCodes) {
            FailSupplier sessionSupplier = new FailSupplier(1, statusCode);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
                    .maxRetries(2)
                    .backoffSlot(TEN_SECONDS)
                    .fastBackoffSlot(TEN_MILLIS)
                    .build();

            AtomicInteger cnt = new AtomicInteger();
            Instant startTime = Instant.now();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();

            Duration timePassed = Duration.between(startTime, Instant.now());
            Assert.assertTrue(String.format("Code: %s - Wrong timeout between retries", statusCode.toString()),
                    timePassed.compareTo(FIVE_SECONDS) < 0);

            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 2, sessionSupplier.getRequestsCount());
            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 1, cnt.get());
            Assert.assertEquals(Status.SUCCESS, status);
        }

        // one session creation fail, but retryable. Idempotent operations. Fast backoff
        List<StatusCode> fastBackoffIdempotentCodes = Arrays.asList(
                StatusCode.CLIENT_CANCELLED,
                StatusCode.CLIENT_INTERNAL_ERROR,
                StatusCode.UNDETERMINED,
                StatusCode.TRANSPORT_UNAVAILABLE
        );
        // Idempotent = false (default)
        for(StatusCode statusCode : fastBackoffIdempotentCodes) {
            FailSupplier sessionSupplier = new FailSupplier(1, statusCode);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier).build();
            Status status = ctx.supplyStatus(session -> completedFuture(Status.SUCCESS)).join();
            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 1, sessionSupplier.getRequestsCount());
            Assert.assertEquals(statusCode, status.getCode());
        }
        // Idempotent = true
        for(StatusCode statusCode : fastBackoffIdempotentCodes) {
            FailSupplier sessionSupplier = new FailSupplier(1, statusCode);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
                    .maxRetries(2)
                    .backoffSlot(TEN_SECONDS)
                    .fastBackoffSlot(TEN_MILLIS)
                    .idempotent(true)
                    .build();

            AtomicInteger cnt = new AtomicInteger();
            Instant startTime = Instant.now();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();

            Duration timePassed = Duration.between(startTime, Instant.now());
            Assert.assertTrue(String.format("Code: %s - Wrong timeout between retries", statusCode.toString()),
                    timePassed.compareTo(FIVE_SECONDS) < 0);

            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 2, sessionSupplier.getRequestsCount());
            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 1, cnt.get());
            Assert.assertEquals(Status.SUCCESS, status);
        }

        // one session creation fail, but retryable. Instant retry
        List<StatusCode> instantRetryCodes = Arrays.asList(
                StatusCode.BAD_SESSION
        );
        for(StatusCode statusCode : instantRetryCodes) {
            FailSupplier sessionSupplier = new FailSupplier(1, statusCode);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
                    .maxRetries(1)
                    .backoffSlot(TEN_SECONDS)
                    .fastBackoffSlot(TEN_SECONDS)
                    .build();

            AtomicInteger cnt = new AtomicInteger();
            Instant startTime = Instant.now();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();

            Duration timePassed = Duration.between(startTime, Instant.now());
            Assert.assertTrue(String.format("Code: %s - Wrong timeout between retries", statusCode.toString()),
                    timePassed.compareTo(FIVE_SECONDS) < 0);

            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 2, sessionSupplier.getRequestsCount());
            Assert.assertEquals(String.format("Code: %s", statusCode.toString()), 1, cnt.get());
            Assert.assertEquals(Status.SUCCESS, status);
        }

        // too many session creation fails
        {
            FailSupplier sessionSupplier = new FailSupplier(10, StatusCode.CLIENT_RESOURCE_EXHAUSTED);
            SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
                .maxRetries(2)
                .backoffSlot(TEN_MILLIS)
                .build();

            AtomicInteger cnt = new AtomicInteger();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();

            Assert.assertEquals(3, sessionSupplier.getRequestsCount());
            Assert.assertEquals(0, cnt.get());
            Assert.assertEquals(CLIENT_RESOURCE_EXHAUSTED, status);
        }
    }

    @Test
    public void failedSession_nonRetryable() {
        FailSupplier sessionSupplier = new FailSupplier(10, StatusCode.TRANSPORT_UNAVAILABLE);
        SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
            .maxRetries(2)
            .build();

        AtomicInteger cnt = new AtomicInteger();
        Status status = ctx.supplyStatus(session -> {
            cnt.incrementAndGet();
            return completedFuture(Status.SUCCESS);
        }).join();

        Assert.assertEquals(1, sessionSupplier.getRequestsCount());
        Assert.assertEquals(0, cnt.get());
        Assert.assertEquals(TRANSPORT_UNAVAILABLE, status);
    }

    @Test
    public void exceptionSession() {
        ExceptionSupplier sessionSupplier = new ExceptionSupplier();
        SessionRetryContext ctx = SessionRetryContext.create(sessionSupplier)
            .maxRetries(2)
            .build();

        AtomicInteger cnt = new AtomicInteger();
        try {
            ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.SUCCESS);
            }).join();
            Assert.fail("expected exception not thrown");
        } catch (Throwable t) {
            Throwable cause = FutureTools.unwrapCompletionException(t);
            Assert.assertTrue(cause instanceof RuntimeException);
            Assert.assertEquals("something goes wrong here", cause.getMessage());
        }

        Assert.assertEquals(1, sessionSupplier.getRetriesCount());
        Assert.assertEquals(0, cnt.get());
    }

    @Test(timeout = 5_000)
    public void sessionBusy_retryable() {
        SessionRetryContext ctx = SessionRetryContext.create(new SuccessSupplier())
            .maxRetries(2)
            .backoffSlot(Duration.ofHours(10L))
            .build();

        AtomicInteger cnt = new AtomicInteger();
        Status status = ctx.supplyStatus(session -> {
            if (cnt.incrementAndGet() == 1) {
                return completedFuture(SESSION_BUSY);
            }
            return completedFuture(Status.SUCCESS);
        }).join();

        Assert.assertEquals(Status.SUCCESS, status);
        Assert.assertEquals(2, cnt.get());
    }

    @Test
    public void baseUsageTest() throws InterruptedException {
        MockedTableRpc rpc = new MockedTableRpc(Clock.systemUTC(), scheduler);
        TableClient client = PooledTableClient.newClient(rpc).sessionPoolSize(0, 2).build();

        SessionRetryContext ctx = SessionRetryContext.create(client)
                .maxRetries(2)
                .backoffSlot(TEN_MILLIS)
                .build();

        CompletableFuture<Result<DataQueryResult>> f1 = futureIsPending(ctx.supplyResult(
                session -> session.executeDataQuery("SELECT 1;", TxControl.snapshotRo())
        ));
        CompletableFuture<Result<DataQueryResult>> f2 = futureIsPending(ctx.supplyResult(
                session -> session.executeDataQuery("SELECT 2;", TxControl.snapshotRo())
        ));

        rpc.check().sessionRequests(2).executeDataRequests(0).deleteSessionRequests(0);
        rpc.nextCreateSession().completeSuccess();
        rpc.nextCreateSession().completeSuccess();

        rpc.check().sessionRequests(0).executeDataRequests(2).deleteSessionRequests(0);
        rpc.nextExecuteDataQuery().completeOverloaded();
        rpc.nextExecuteDataQuery().completeOverloaded();

        Thread.sleep(200);

        rpc.check().sessionRequests(0).executeDataRequests(2).deleteSessionRequests(0);
        rpc.nextExecuteDataQuery().completeSuccess();
        rpc.nextExecuteDataQuery().completeSuccess();

        futureIsReady(f1);
        futureIsReady(f2);

        client.close();
        rpc.check().deleteSessionRequests(2);
        rpc.completeSessionDeleteRequests();
    }

    @Test
    public void customExecutorUsageTest() throws InterruptedException {
        ExecutorService custom = Executors.newSingleThreadExecutor();
        MockedTableRpc rpc = new MockedTableRpc(Clock.systemUTC(), scheduler);
        TableClient client = PooledTableClient.newClient(rpc).sessionPoolSize(0, 2).build();

        SessionRetryContext ctx = SessionRetryContext.create(client)
                .maxRetries(2)
                .executor(custom)
                .backoffSlot(TEN_MILLIS)
                .build();

        CompletableFuture<Result<DataQueryResult>> f1 = futureIsPending(ctx.supplyResult(
                session -> session.executeDataQuery("SELECT 1;", TxControl.snapshotRo())
        ));
        CompletableFuture<Result<DataQueryResult>> f2 = futureIsPending(ctx.supplyResult(
                session -> session.executeDataQuery("SELECT 2;", TxControl.snapshotRo())
        ));

        rpc.check().sessionRequests(2).executeDataRequests(0).deleteSessionRequests(0);
        rpc.nextCreateSession().completeSuccess();
        rpc.nextCreateSession().completeSuccess();

        Thread.sleep(200);

        rpc.check().sessionRequests(0).executeDataRequests(2).deleteSessionRequests(0);
        rpc.nextExecuteDataQuery().completeOverloaded();
        rpc.nextExecuteDataQuery().completeOverloaded();

        Thread.sleep(200);

        rpc.check().sessionRequests(0).executeDataRequests(2).deleteSessionRequests(0);
        rpc.nextExecuteDataQuery().completeSuccess();
        rpc.nextExecuteDataQuery().completeSuccess();

        futureIsReady(f1);
        futureIsReady(f2);

        client.close();
        rpc.check().deleteSessionRequests(2);
        rpc.completeSessionDeleteRequests();
        custom.shutdown();
    }

    /**
     * SUCCESS SUPPLIER
     */
    private static final class SuccessSupplier implements SessionSupplier {
        @Override
        public CompletableFuture<Result<Session>> createSession(Duration timeout) {
            return completedFuture(Result.success(new SessionStub()));
        }

        @Override
        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }
    }

    /**
     * FAIL SUPPLIER
     */
    private static final class FailSupplier implements SessionSupplier {
        private final int maxFails;
        private final StatusCode statusCode;
        private final AtomicInteger requestsCount = new AtomicInteger();

        FailSupplier(int maxFails, StatusCode statusCode) {
            this.maxFails = maxFails;
            this.statusCode = statusCode;
        }

        @Override
        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }

        int getRequestsCount() {
            return requestsCount.get();
        }

        @Override
        public CompletableFuture<Result<Session>> createSession(Duration timeout) {
            if (requestsCount.getAndIncrement() >= maxFails) {
                return completedFuture(Result.success(new SessionStub()));
            }
            return completedFuture(Result.fail(Status.of(statusCode)));
        }
    }

    /**
     * EXCEPTION SUPPLIER
     */
    private static final class ExceptionSupplier implements SessionSupplier {
        private final AtomicInteger retriesCount = new AtomicInteger();

        int getRetriesCount() {
            return retriesCount.get();
        }

        @Override
        public ScheduledExecutorService getScheduler() {
            return scheduler;
        }

        @Override
        public CompletableFuture<Result<Session>> createSession(Duration timeout) {
            retriesCount.incrementAndGet();
            return FutureTools.failedFuture(new RuntimeException("something goes wrong here"));
        }
    }

}
