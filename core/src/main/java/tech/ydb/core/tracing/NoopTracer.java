package tech.ydb.core.tracing;

public final class NoopTracer implements Tracer {
    private static final NoopTracer INSTANCE = new NoopTracer();
    private static final NoopSpanBuilder SPAN_BUILDER = new NoopSpanBuilder();
    private static final NoopSpan SPAN = new NoopSpan();
    private static final Scope NOOP_SCOPE = () -> { };

    private NoopTracer() { }

    public static NoopTracer getInstance() {
        return INSTANCE;
    }

    public static Span getSpan() {
        return SPAN;
    }

    @Override
    public SpanBuilder spanBuilder(String spanName) {
        return SPAN_BUILDER;
    }

    private static final class NoopSpanBuilder implements SpanBuilder {
        @Override
        public SpanBuilder setAttribute(String key, String value) {
            return this;
        }

        @Override
        public SpanBuilder setAttribute(String key, long value) {
            return this;
        }

        @Override
        public Span startSpan() {
            return SPAN;
        }
    }

    private static final class NoopSpan implements Span {
        @Override
        public Scope makeCurrent() {
            return NOOP_SCOPE;
        }

        @Override
        public void injectHeaders(TraceHeaderSetter headerSetter) {
            // nothing
        }

        @Override
        public Span setAttribute(String key, String value) {
            return this;
        }

        @Override
        public Span setAttribute(String key, long value) {
            return this;
        }

        @Override
        public Span setAttribute(String key, boolean value) {
            return this;
        }

        @Override
        public Span setStatus(SpanStatusCode code) {
            return this;
        }

        @Override
        public Span setStatus(SpanStatusCode code, String description) {
            return this;
        }

        @Override
        public Span recordException(Throwable error) {
            return this;
        }

        @Override
        public void end() {
            // nothing
        }
    }
}
