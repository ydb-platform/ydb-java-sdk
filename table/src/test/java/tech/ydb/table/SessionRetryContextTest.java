package tech.ydb.table;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.table.utils.Async;
import org.junit.Assert;
import org.junit.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;


/**
 * @author Sergey Polovko
 */
public class SessionRetryContextTest {

    private static final Duration TEN_MILLIS = Duration.ofMillis(10);
    private static final Duration FIVE_SECONDS = Duration.ofSeconds(5);
    private static final Duration TEN_SECONDS = Duration.ofSeconds(10);

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
            Result<?> result = ctx.supplyResult(session -> {
                cnt.incrementAndGet();
                return completedFuture(Result.fail(StatusCode.CANCELLED));
            }).join();

            Assert.assertEquals(1, cnt.get());
            Assert.assertEquals(Result.fail(StatusCode.CANCELLED), result);
        }

        // retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Result<?> result = ctx.supplyResult(session -> {
                cnt.incrementAndGet();
                return completedFuture(Result.fail(StatusCode.OVERLOADED));
            }).join();

            Assert.assertEquals(3, cnt.get());
            Assert.assertEquals(Result.fail(StatusCode.OVERLOADED), result);
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
                Throwable cause = Async.unwrapCompletionException(t);
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
                    Result.fail(StatusCode.NOT_FOUND).expect("unexpected fail");
                    return null;
                }).join();
                Assert.fail("expected exception not thrown");
            } catch (Throwable t) {
                Throwable cause = Async.unwrapCompletionException(t);
                Assert.assertTrue(cause instanceof UnexpectedResultException);
                Assert.assertEquals(3, cnt.get());
                Assert.assertEquals("unexpected fail, code: NOT_FOUND", cause.getMessage());
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
                return completedFuture(Status.of(StatusCode.SCHEME_ERROR));
            }).join();

            Assert.assertEquals(1, cnt.get());
            Assert.assertEquals(Status.of(StatusCode.SCHEME_ERROR), status);
        }

        // retryable status code
        {
            AtomicInteger cnt = new AtomicInteger();
            Status status = ctx.supplyStatus(session -> {
                cnt.incrementAndGet();
                return completedFuture(Status.of(StatusCode.OVERLOADED));
            }).join();

            Assert.assertEquals(3, cnt.get());
            Assert.assertEquals(Status.of(StatusCode.OVERLOADED), status);
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
            Assert.assertEquals(Status.of(StatusCode.CLIENT_RESOURCE_EXHAUSTED), status);
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
        Assert.assertEquals(Status.of(StatusCode.TRANSPORT_UNAVAILABLE), status);
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
            Throwable cause = Async.unwrapCompletionException(t);
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
                return completedFuture(Status.of(StatusCode.SESSION_BUSY));
            }
            return completedFuture(Status.SUCCESS);
        }).join();

        Assert.assertEquals(Status.SUCCESS, status);
        Assert.assertEquals(2, cnt.get());
    }

    /**
     * SUCCESS SUPPLIER
     */
    private static final class SuccessSupplier implements SessionSupplier {
        @Override
        public CompletableFuture<Result<Session>> getOrCreateSession(Duration timeout) {
            return completedFuture(Result.success(new SessionStub()));
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

        int getRequestsCount() {
            return requestsCount.get();
        }

        @Override
        public CompletableFuture<Result<Session>> getOrCreateSession(Duration timeout) {
            if (requestsCount.getAndIncrement() >= maxFails) {
                return completedFuture(Result.success(new SessionStub()));
            }
            return completedFuture(Result.fail(statusCode));
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
        public CompletableFuture<Result<Session>> getOrCreateSession(Duration timeout) {
            retriesCount.incrementAndGet();
            return Async.failedFuture(new RuntimeException("something goes wrong here"));
        }
    }

}
