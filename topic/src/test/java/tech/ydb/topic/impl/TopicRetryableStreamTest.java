package tech.ydb.topic.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.Empty;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryConfig;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.topic.utils.HideLoggers;
import tech.ydb.topic.utils.HideLoggersRule;

public class TopicRetryableStreamTest {
    private static final Logger logger = LoggerFactory.getLogger(TopicRetryableStreamTest.class);
    private static final Empty EMPTY = Empty.getDefaultInstance();

    @Rule
    public final HideLoggersRule hideLogger = new HideLoggersRule();

    /**
     * Pairs a mock GrpcReadWriteStream with a concrete TopicStream backed by it.
     * Completing grpcFuture simulates the underlying gRPC stream finishing.
     */
    private static class StreamHandle {
        @SuppressWarnings("unchecked")
        private final GrpcReadWriteStream<Empty, Empty> grpc = Mockito.mock(GrpcReadWriteStream.class);

        private final CompletableFuture<Status> grpcFuture = new CompletableFuture<>();
        private final TopicStreamBase<Empty, Empty> stream;

        StreamHandle(TopicStreamBase<Empty, Empty> mocked) {
            this.stream = mocked;
            Mockito.when(mocked.start(Mockito.any())).thenReturn(grpcFuture);
        }

        StreamHandle() {
            Mockito.when(grpc.authToken()).thenReturn("token");
            Mockito.when(grpc.start(Mockito.any())).thenReturn(grpcFuture);

            stream = new TopicStreamBase<Empty, Empty>(logger, "inner", grpc, EMPTY) {
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

    private static class TestStream extends TopicRetryableStream<Empty, Empty, TopicStreamBase<Empty, Empty>> {
        private final List<CompletableFuture<Result<StreamHandle>>> handles = new ArrayList<>();
        private final AtomicInteger handleIndex = new AtomicInteger(0);

        final List<Status> retryStatuses = new ArrayList<>();
        final List<Status> closeStatuses = new ArrayList<>();
        final List<Empty> receivedMessages = new ArrayList<>();

        TestStream(RetryConfig retryConfig, ScheduledExecutorService scheduler, StreamHandle... initList) {
            super(logger, "test", retryConfig, scheduler);
            for (StreamHandle handle: initList) {
                handles.add(CompletableFuture.completedFuture(Result.success(handle)));
            }
        }

        public void addHandle(CompletableFuture<Result<StreamHandle>> handle) {
            handles.add(handle);
        }

        @Override
        protected CompletableFuture<Result<TopicStreamBase<Empty, Empty>>> createNewStream(String debugId) {
            return handles.get(handleIndex.getAndIncrement()).thenApply(r -> r.map(h -> h.stream));
        }

        @Override
        protected void onNext(TopicStreamBase<Empty, Empty> stream, Empty message) {
            receivedMessages.add(message);
        }

        @Override
        protected void onRetry(TopicStreamBase<Empty, Empty> stream, Status status) {
            retryStatuses.add(status);
        }

        @Override
        protected void onClose(TopicStreamBase<Empty, Empty> stream, Status status) {
            closeStatuses.add(status);
        }
    }

    private ScheduledExecutorService mockScheduler() {
        return Mockito.mock(ScheduledExecutorService.class);
    }

    @Test
    public void simpleStartAndCloseTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        retryable.start();

        retryable.send(EMPTY);

        Mockito.verify(h.grpc).start(Mockito.any());
        Mockito.verify(h.grpc, Mockito.times(2)).sendNext(EMPTY); // init + sent request

        Assert.assertFalse(retryable.isClosed());
        Assert.assertTrue(retryable.close());
        Assert.assertTrue(retryable.isClosed());

        h.complete(Status.SUCCESS);

        Mockito.verify(h.grpc).close();
        Mockito.verify(h.grpc, Mockito.never()).cancel();

        Assert.assertEquals(Arrays.asList(Status.SUCCESS), retryable.closeStatuses);
    }

    @Test
    public void failStreamTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        retryable.start();

        retryable.send(EMPTY);

        Mockito.verify(h.grpc).start(Mockito.any());
        Mockito.verify(h.grpc, Mockito.times(2)).sendNext(EMPTY); // init + sent request

        Assert.assertFalse(retryable.isClosed());
        retryable.fail(Status.of(StatusCode.ABORTED));
        retryable.fail(Status.of(StatusCode.CLIENT_INTERNAL_ERROR)); // will be ignored

        Assert.assertTrue(retryable.isClosed());
        Assert.assertFalse(retryable.close());

        Mockito.verify(h.grpc).close();
        Mockito.verify(h.grpc, Mockito.never()).cancel();

        h.complete(Status.SUCCESS);
        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.ABORTED)), retryable.closeStatuses);
    }

    @Test
    public void doubleStartTest() {
        StreamHandle h1 = new StreamHandle();
        StreamHandle h2 = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h1, h2);

        retryable.start(); // sets realStream = h1.topicStream
        retryable.start(); // compareAndSet fails → h2.topicStream is closed byt not started

        Mockito.verify(h1.grpc).start(Mockito.any());
        Mockito.verify(h2.grpc, Mockito.never()).start(Mockito.any()); // h2 was never started
        Mockito.verify(h2.grpc).close();
    }

    @Test
    public void doubleCloseTest() {
        StreamHandle h1 = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h1);

        retryable.start();

        Assert.assertTrue(retryable.close());

        Assert.assertFalse(retryable.close());

        Mockito.verify(h1.grpc).start(Mockito.any());
        Mockito.verify(h1.grpc).close();
    }

    @Test
    public void startAfterCloseTest() {
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());
        Assert.assertFalse(retryable.close());
        retryable.start(); // nothing
    }

    @Test
    public void asyncStreamCreationTest() {
        StreamHandle streamHandle = new StreamHandle();
        CompletableFuture<Result<StreamHandle>> creation = new CompletableFuture<>();

        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());
        retryable.addHandle(creation);

        retryable.start(); // must return without waiting for the creation future
        Mockito.verify(streamHandle.grpc, Mockito.never()).start(Mockito.any());

        retryable.send(EMPTY); // stream is not ready yet, message is skipped
        Mockito.verify(streamHandle.grpc, Mockito.never()).sendNext(Mockito.any());

        creation.complete(Result.success(streamHandle));

        Mockito.verify(streamHandle.grpc).start(Mockito.any());
        retryable.send(EMPTY);
        Mockito.verify(streamHandle.grpc, Mockito.times(2)).sendNext(EMPTY); // init + sent request

        Assert.assertTrue(retryable.close());
        Mockito.verify(streamHandle.grpc).close();
    }

    @Test
    public void closeWhileAsyncInitializationTest() {
        StreamHandle streamHandle = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());

        CompletableFuture<Result<StreamHandle>> creation = new CompletableFuture<>();
        retryable.addHandle(creation);

        retryable.start();
        Assert.assertTrue(retryable.close());

        creation.complete(Result.success(streamHandle));

        Mockito.verify(streamHandle.grpc, Mockito.never()).start(Mockito.any());
        Mockito.verify(streamHandle.grpc).close();

        Assert.assertFalse(retryable.close());

        Assert.assertTrue(retryable.retryStatuses.isEmpty());
        Assert.assertEquals(Arrays.asList(Status.SUCCESS), retryable.closeStatuses);
    }

    @Test
    public void closeWhileAsyncInitializationFailedTest() {
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());

        CompletableFuture<Result<StreamHandle>> creation = new CompletableFuture<>();
        retryable.addHandle(creation);

        retryable.start();
        Assert.assertTrue(retryable.close());
        creation.complete(Result.fail(Status.of(StatusCode.BAD_REQUEST))); // will be lost

        Assert.assertFalse(retryable.close());
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
        Assert.assertEquals(Arrays.asList(Status.SUCCESS), retryable.closeStatuses);
    }

    @Test
    public void closeWhileAsyncInitializationErrorTest() {
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());

        CompletableFuture<Result<StreamHandle>> creation = new CompletableFuture<>();
        retryable.addHandle(creation);

        retryable.start();
        Assert.assertTrue(retryable.close());
        creation.completeExceptionally(new IllegalArgumentException("error")); // will be lost

        Assert.assertFalse(retryable.close());
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
        Assert.assertEquals(Arrays.asList(Status.SUCCESS), retryable.closeStatuses);
    }

    @Test
    @HideLoggers({TopicRetryableStreamTest.class})
    public void streamCreationFailedTest() {
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler());

        CompletableFuture<Result<StreamHandle>> creation = new CompletableFuture<>();
        retryable.addHandle(creation);

        retryable.start();
        creation.completeExceptionally(new RuntimeException("cannot create stream"));

        Assert.assertEquals(1, retryable.closeStatuses.size());
        Assert.assertEquals(StatusCode.CLIENT_INTERNAL_ERROR, retryable.closeStatuses.get(0).getCode());
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
    }

    @Test
    public void sendBeforeStartIsIgnoredTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        retryable.send(EMPTY); // just skipping

        Mockito.verify(h.grpc, Mockito.never()).sendNext(Mockito.any());
    }

    @Test
    public void closeBeforeStartIsNoOpTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        Assert.assertFalse(retryable.close()); // no stream yet, should not throw

        Mockito.verify(h.grpc, Mockito.never()).close();
    }

    @Test
    public void noRetriesErrorStatusTest() {
        StreamHandle h = new StreamHandle();
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        retryable.start();
        h.complete(Status.of(StatusCode.ABORTED));

        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.ABORTED)), retryable.closeStatuses);
        Assert.assertTrue(retryable.retryStatuses.isEmpty());
    }

    @Test
    public void noRetriesExceptionStatusTest() {
        @SuppressWarnings("unchecked")
        StreamHandle h = new StreamHandle(Mockito.mock(TopicStreamBase.class));
        TestStream retryable = new TestStream(RetryConfig.noRetries(), mockScheduler(), h);

        retryable.start();
        RuntimeException ex = new RuntimeException("fail");
        Assert.assertFalse(retryable.isClosed());
        h.fail(ex);
        Assert.assertTrue(retryable.isClosed());

        Assert.assertEquals(Arrays.asList(Status.of(StatusCode.CLIENT_INTERNAL_ERROR, ex)), retryable.closeStatuses);
        Assert.assertTrue(retryable.retryStatuses.isEmpty());

        Assert.assertFalse(retryable.close());
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

        TestStream retryable = new TestStream(config, mockScheduler(), h1, h2, h3);
        Assert.assertFalse(retryable.isClosed());

        retryable.start();

        Mockito.verify(h1.grpc).start(Mockito.any()); // first stream was started

        retryable.send(EMPTY);
        h1.complete(s1);
        Assert.assertFalse(retryable.isClosed());

        Mockito.verify(h2.grpc).start(Mockito.any()); // second stream was started
        retryable.send(EMPTY);
        retryable.send(EMPTY);
        h2.complete(s2);
        Assert.assertFalse(retryable.isClosed());

        Mockito.verify(h3.grpc).start(Mockito.any()); // third stream was started
        retryable.send(EMPTY);
        h3.complete(s3);
        Assert.assertTrue(retryable.isClosed());

        Assert.assertFalse(retryable.close()); // no effect

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
    @HideLoggers({TopicRetryableStreamTest.class})
    public void closeOnWrongSchedulerTest() {
        StreamHandle h = new StreamHandle();
        long delayMs = 500L;
        RetryConfig config = status -> (retryCount, elapsed) -> delayMs;

        TestStream retryable = new TestStream(config, null, h);

        retryable.start();
        Assert.assertFalse(retryable.isClosed());

        h.complete(Status.of(StatusCode.UNAVAILABLE));

        Assert.assertTrue(retryable.isClosed());
        Assert.assertEquals(1, retryable.retryStatuses.size());
        Assert.assertEquals(1, retryable.closeStatuses.size());
    }

    @Test
    public void scheduledRetryWithCorrectDelayTest() {
        StreamHandle h = new StreamHandle();
        ScheduledExecutorService scheduler = mockScheduler();
        long delayMs = 500L;
        RetryConfig config = status -> (retryCount, elapsed) -> delayMs;

        TestStream retryable = new TestStream(config, scheduler, h);

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

        TestStream retryable = new TestStream(config, mockScheduler(), h1, h2, h3);

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
