package tech.ydb.topic.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;

public class TopicRetryableStreamTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicStreamTest.class);
    private static final Empty EMPTY = Empty.getDefaultInstance();

    /**
     * Pairs a mock GrpcReadWriteStream with a concrete TopicStream backed by it.
     * Completing grpcFuture simulates the underlying gRPC stream finishing.
     */
    private static class StreamHandle {
        @SuppressWarnings("unchecked")
        private final GrpcReadWriteStream<Empty, Empty> grpc = Mockito.mock(GrpcReadWriteStream.class);

        private final CompletableFuture<Status> grpcFuture = new CompletableFuture<>();
        private final TopicStream<Empty, Empty> stream;

        StreamHandle(TopicStream<Empty, Empty> mocked) {
            this.stream = mocked;
            Mockito.when(mocked.start(Mockito.any(), Mockito.any())).thenReturn(grpcFuture);
        }

        StreamHandle() {
            Mockito.when(grpc.authToken()).thenReturn("token");
            Mockito.when(grpc.start(Mockito.any())).thenReturn(grpcFuture);

            stream = new TopicStream<Empty, Empty>(logger, "inner", grpc) {
                @Override
                protected Empty updateTokenMessage(String token) {
                    return EMPTY;
                }

                @Override
                protected Status parseMessageStatus(Empty message) {
                    return Status.SUCCESS;
                }
            };
        }

        void complete(Status status) {
            grpcFuture.complete(status);
        }

        void fail(Throwable th) {
            grpcFuture.completeExceptionally(th);
        }
    }

    private static class TestStream extends TopicRetryableStream<Empty, Empty> {
        private final List<StreamHandle> handles;
        private int handleIndex = 0;

        final List<Status> retryStatuses = new ArrayList<>();
        final List<Status> closeStatuses = new ArrayList<>();
        final List<Empty> receivedMessages = new ArrayList<>();

        TestStream(List<StreamHandle> handles, RetryConfig retryConfig, ScheduledExecutorService scheduler) {
            super(logger, "test", retryConfig, scheduler);
            this.handles = handles;
        }

        @Override
        protected TopicStream<Empty, Empty> createNewStream(String debugId) {
            return handles.get(handleIndex++).stream;
        }

        @Override
        protected Empty getInitRequest() {
            return EMPTY;
        }

        @Override
        protected void onNext(Empty message) {
            receivedMessages.add(message);
        }

        @Override
        protected void onRetry(Status status) {
            retryStatuses.add(status);
        }

        @Override
        protected void onClose(Status status) {
            closeStatuses.add(status);
        }
    }

    private ScheduledExecutorService mockScheduler() {
        return Mockito.mock(ScheduledExecutorService.class);
    }

    @Test
    public void simpleStartAndCloseTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(Arrays.asList(h), RetryConfig.noRetries(), mockScheduler());

        retryable.start();
        retryable.send(EMPTY);

        Mockito.verify(h.grpc).start(Mockito.any());
        Mockito.verify(h.grpc, Mockito.times(2)).sendNext(EMPTY); // init + sent request

        retryable.close();

        h.complete(Status.SUCCESS);

        Mockito.verify(h.grpc).close();
        Mockito.verify(h.grpc, Mockito.never()).cancel();

        Assert.assertEquals(Arrays.asList(Status.SUCCESS), retryable.closeStatuses);
    }

    @Test
    public void doubleStartTest() {
        StreamHandle h1 = new StreamHandle();
        StreamHandle h2 = new StreamHandle();
        TestStream retryable = new TestStream(Arrays.asList(h1, h2), RetryConfig.noRetries(), mockScheduler());

        retryable.start(); // sets realStream = h1.topicStream
        retryable.start(); // compareAndSet fails → h2.topicStream is closed

        Mockito.verify(h1.grpc).start(Mockito.any());
        Mockito.verify(h2.grpc, Mockito.never()).start(Mockito.any()); // h2 was never started
        Mockito.verify(h2.grpc).close();                               // h2 was closed immediately
    }

    @Test
    public void sendBeforeStartIsIgnoredTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(Arrays.asList(h), RetryConfig.noRetries(), mockScheduler());

        retryable.send(EMPTY); // just skipping

        Mockito.verify(h.grpc, Mockito.never()).sendNext(Mockito.any());
    }

    @Test
    public void closeBeforeStartIsNoOpTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(Arrays.asList(h), RetryConfig.noRetries(), mockScheduler());

        retryable.close(); // no stream yet, should not throw

        Mockito.verify(h.grpc, Mockito.never()).close();
    }

    @Test
    public void noRetriesErrorStatusTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(Arrays.asList(h), RetryConfig.noRetries(), mockScheduler());

        retryable.start();
        h.complete(Status.of(StatusCode.ABORTED));

        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.ABORTED)), retryable.closeStatuses);
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
    }

    @Test
    public void noRetriesExceptionStatusTest() {
        @SuppressWarnings("unchecked")
        StreamHandle h = new StreamHandle(Mockito.mock(TopicStream.class));
        TestStream retryable = new TestStream(Arrays.asList(h), RetryConfig.noRetries(), mockScheduler());

        retryable.start();
        RuntimeException ex = new RuntimeException("fail");
        h.fail(ex);

        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, ex)), retryable.closeStatuses);
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
    }

    @Test
    public void immediateRetryTest() {
        StreamHandle h1 = new StreamHandle();
        StreamHandle h2 = new StreamHandle();
        StreamHandle h3 = new StreamHandle();

        Status s1 = Status.of(StatusCode.UNAVAILABLE);
        Status s2 = Status.of(StatusCode.BAD_SESSION);
        Status s3 = Status.of(StatusCode.BAD_REQUEST);

        // Policy: immediate retry (0ms) on all attempts, then no more
        RetryConfig config = status -> (retryCount, elapsed) -> (status.getCode() != StatusCode.BAD_REQUEST) ? 0 : -1;

        TestStream retryable = new TestStream(Arrays.asList(h1, h2, h3), config, mockScheduler());

        retryable.start();

        Mockito.verify(h1.grpc).start(Mockito.any()); // first stream was started

        retryable.send(EMPTY);
        h1.complete(s1);

        Mockito.verify(h2.grpc).start(Mockito.any()); // second stream was started
        retryable.send(EMPTY);
        retryable.send(EMPTY);
        h2.complete(s2);

        Mockito.verify(h3.grpc).start(Mockito.any()); // third stream was started
        retryable.send(EMPTY);
        h3.complete(s3);

        retryable.close(); // no effect

        Mockito.verify(h1.grpc, Mockito.times(2)).sendNext(EMPTY); // init req + send
        Mockito.verify(h1.grpc, Mockito.never()).close();  // stream was closed by error

        Mockito.verify(h2.grpc, Mockito.times(3)).sendNext(EMPTY); // init req + 2 * send
        Mockito.verify(h2.grpc, Mockito.never()).close();  // stream was closed by error

        Mockito.verify(h3.grpc, Mockito.times(2)).sendNext(EMPTY); // init req + send
        Mockito.verify(h3.grpc, Mockito.never()).close();  // stream was closed by error

        Assert.assertEquals(Arrays.asList(s1, s2), retryable.retryStatuses);
        Assert.assertEquals(Arrays.asList(s3), retryable.closeStatuses);
    }

    @Test
    public void closeOnWrongSchedulerTest() {
        StreamHandle h = new StreamHandle();
        long delayMs = 500L;
        RetryConfig config = status -> (retryCount, elapsed) -> delayMs;

        TestStream retryable = new TestStream(Arrays.asList(h), config, null);

        retryable.start();
        h.complete(Status.of(StatusCode.UNAVAILABLE));

        Assert.assertEquals(1, retryable.retryStatuses.size());
        Assert.assertEquals(1, retryable.closeStatuses.size());
    }

    @Test
    public void scheduledRetryWithCorrectDelayTest() {
        StreamHandle h = new StreamHandle();
        ScheduledExecutorService scheduler = mockScheduler();
        long delayMs = 500L;
        RetryConfig config = status -> (retryCount, elapsed) -> delayMs;

        TestStream retryable = new TestStream(
                Arrays.asList(h), config, scheduler);

        retryable.start();
        h.complete(Status.of(StatusCode.UNAVAILABLE));

        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.UNAVAILABLE)), retryable.retryStatuses);
        Assert.assertTrue(retryable.closeStatuses.isEmpty());
        Mockito.verify(scheduler)
                .schedule(Mockito.any(Runnable.class), Mockito.eq(delayMs), Mockito.eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void testResetRetriesAllowsRetryingAgainFromZero() {
        StreamHandle h1 = new StreamHandle();
        StreamHandle h2 = new StreamHandle();
        StreamHandle h3 = new StreamHandle();
        // Policy: one immediate retry (retryCount 0), then no more
        RetryConfig config = status -> (retryCount, elapsed) -> retryCount == 0 ? 0 : -1;

        TestStream retryable = new TestStream(Arrays.asList(h1, h2, h3), config, mockScheduler());

        Status error = Status.of(StatusCode.UNAVAILABLE);
        retryable.start();
        h1.complete(error); // retry fires (retryCount 0 → 0ms)

        Assert.assertEquals(Arrays.asList(error), retryable.retryStatuses);
        Assert.assertTrue(retryable.closeStatuses.isEmpty());

        // Reset the retry counter so we get another chance
        retryable.resetRetries();
        h2.complete(error); // retry fires again (retryCount reset to 0)

        Assert.assertEquals(Arrays.asList(error, error), retryable.retryStatuses);
        Assert.assertTrue(retryable.closeStatuses.isEmpty());

        Mockito.verify(h3.grpc).start(Mockito.any());
    }
}
