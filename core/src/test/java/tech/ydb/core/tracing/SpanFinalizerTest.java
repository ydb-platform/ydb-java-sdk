package tech.ydb.core.tracing;

import java.util.concurrent.CompletionException;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

public class SpanFinalizerTest {
    @Test
    public void finishByStatusSuccessEndsWithoutError() {
        RecordingSpan span = new RecordingSpan();

        SpanFinalizer.finishByStatus(span, Status.SUCCESS);

        Assert.assertTrue(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertNull(span.throwableError);
    }

    @Test
    public void finishByStatusFailSetsStatusErrorAndEnds() {
        RecordingSpan span = new RecordingSpan();
        Status failed = Status.of(StatusCode.BAD_REQUEST);

        SpanFinalizer.finishByStatus(span, failed);

        Assert.assertTrue(span.ended);
        Assert.assertEquals(failed, span.statusError);
        Assert.assertNull(span.throwableError);
    }

    @Test
    public void finishByErrorUnwrapsCompletionException() {
        RecordingSpan span = new RecordingSpan();
        RuntimeException root = new RuntimeException("root");

        SpanFinalizer.finishByError(span, new CompletionException(root));

        Assert.assertTrue(span.ended);
        Assert.assertNull(span.statusError);
        Assert.assertSame(root, span.throwableError);
    }

    @Test
    public void whenCompleteHandlesStatusAndErrorPaths() {
        RecordingSpan statusSpan = new RecordingSpan();
        RecordingSpan errorSpan = new RecordingSpan();
        RuntimeException root = new RuntimeException("root");

        SpanFinalizer.whenComplete(statusSpan).accept(Status.of(StatusCode.INTERNAL_ERROR), null);
        SpanFinalizer.whenComplete(errorSpan).accept(null, new CompletionException(root));

        Assert.assertTrue(statusSpan.ended);
        Assert.assertEquals(StatusCode.INTERNAL_ERROR, statusSpan.statusError.getCode());
        Assert.assertNull(statusSpan.throwableError);

        Assert.assertTrue(errorSpan.ended);
        Assert.assertNull(errorSpan.statusError);
        Assert.assertSame(root, errorSpan.throwableError);
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
        public void setError(Status status) {
            this.statusError = status;
        }

        @Override
        public void setError(Throwable error) {
            this.throwableError = error;
        }

        @Override
        public void end() {
            this.ended = true;
        }
    }
}
