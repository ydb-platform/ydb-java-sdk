package tech.ydb.core.tracing;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class NoopTracerTest {
    @Test
    public void singletonTest() {
        Tracer t1 = NoopTracer.getInstance();
        Tracer t2 = NoopTracer.getInstance();

        Assert.assertSame(t1, t2);
    }

    @Test
    public void spanTest() {
        Tracer tracer = NoopTracer.getInstance();

        Assert.assertSame(Span.NOOP, tracer.startSpan("test", SpanKind.CLIENT));
        Assert.assertSame(Span.NOOP, tracer.startSpan("test", SpanKind.INTERNAL));
        Assert.assertSame(Span.NOOP, tracer.startSpan(null, null));
        Assert.assertSame(Span.NOOP, tracer.currentSpan());
    }
}
