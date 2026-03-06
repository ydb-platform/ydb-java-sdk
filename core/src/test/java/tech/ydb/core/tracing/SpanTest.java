package tech.ydb.core.tracing;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class SpanTest {

    @Test
    public void finishByStatusExceptionTest() {
        RecordingSpan span = new RecordingSpan();
        IOException ex = new IOException("test message");

        CompletableFuture<Status> future = new CompletableFuture<>();

        Assert.assertSame(future, Span.endOnStatus(null, future));
        Assert.assertSame(future, Span.endOnStatus(span, future));

        Assert.assertFalse(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertNull(span.throwableError);

        future.completeExceptionally(ex);

        Assert.assertTrue(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertSame(ex, span.throwableError);
    }

    @Test
    public void finishByResultTest() {
        RecordingSpan span = new RecordingSpan();
        Status fail = Status.of(StatusCode.BAD_REQUEST);

        CompletableFuture<Result<String>> future = new CompletableFuture<>();

        Assert.assertSame(future, Span.endOnResult(span, future));

        Assert.assertFalse(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertNull(span.throwableError);

        future.complete(Result.fail(fail));

        Assert.assertTrue(span.ended);
        Assert.assertSame(fail, span.statusError);
        Assert.assertNull(span.throwableError);
    }

    @Test
    public void finishByResultExceptionTest() {
        RecordingSpan span = new RecordingSpan();
        IOException ex = new IOException("test message");

        CompletableFuture<Result<String>> future = new CompletableFuture<>();

        Assert.assertSame(future, Span.endOnResult(null, future));
        Assert.assertSame(future, Span.endOnResult(span, future));

        Assert.assertFalse(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertNull(span.throwableError);

        future.completeExceptionally(ex);

        Assert.assertTrue(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertSame(ex, span.throwableError);
    }

    private static final class RecordingSpan implements Span {
        private Status statusError;
        private Throwable throwableError;
        private boolean ended;

        @Override
        public String getId() {
            return "test-span";
        }

        @Override
        public void setAttribute(String key, String value) {
            // not needed in this test
        }

        @Override
        public void setAttribute(String key, long value) {
            // not needed in this test
        }

        @Override
        public void setStatus(Status status, Throwable error) {
            this.statusError = status;
            this.throwableError = error;
        }

        @Override
        public void end() {
            this.ended = true;
        }
    }
}
