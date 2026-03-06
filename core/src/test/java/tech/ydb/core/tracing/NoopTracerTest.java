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

        Assert.assertNull(tracer.startSpan("test", SpanKind.CLIENT));
        Assert.assertNull(tracer.startSpan("test", SpanKind.INTERNAL));
        Assert.assertNull(tracer.startSpan(null, null));
    }
}
