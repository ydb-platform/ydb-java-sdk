package tech.ydb.core.tracing;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;

/**
 * Common helper to build DB operation spans with YDB attributes.
 */
public final class DbSpanFactory {
    private static final int MAX_ISSUES_LENGTH = 512;

    private final Tracer tracer;
    private final String dbNamespace;
    private final String serverAddress;
    private final int serverPort;
    private final String scheme;

    public DbSpanFactory(GrpcTransport transport) {
        this.tracer = transport.getTracer();
        this.dbNamespace = transport.getDatabase();
        this.serverAddress = transport.getServerAddress();
        this.serverPort = transport.getServerPort();
        this.scheme = transport.getTransportScheme();
    }

    public OperationSpan startClientSpan(String operationName, @Nullable String traceId, @Nullable String sessionId) {
        SpanBuilder spanBuilder = tracer
                .spanBuilder(operationName)
                .setAttribute("db.system.name", "ydb")
                .setAttribute("db.namespace", dbNamespace)
                .setAttribute("server.address", serverAddress)
                .setAttribute("server.port", serverPort)
                .setAttribute("url.scheme", scheme);

        if (traceId != null && !traceId.isEmpty()) {
            spanBuilder.setAttribute("ydb.trace_id", traceId);
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            spanBuilder.setAttribute("ydb.session_id", sessionId);
        }

        return new OperationSpan(spanBuilder.startSpan());
    }

    public static final class OperationSpan {
        private final Span span;
        private final AtomicBoolean finished = new AtomicBoolean(false);

        OperationSpan(Span span) {
            this.span = span;
        }

        public Span getSpan() {
            return span;
        }

        public OperationSpan setAttribute(String key, String value) {
            if (value != null && !value.isEmpty()) {
                span.setAttribute(key, value);
            }
            return this;
        }

        public void finishStatus(Status status) {
            if (status == null) {
                finishError(new IllegalStateException("Operation finished without status"));
                return;
            }

            if (!status.isSuccess()) {
                StatusCode code = status.getCode();
                span.setAttribute("db.response.status_code", code.getCode());
                span.setAttribute("error.type", toErrorType(code));
                span.setAttribute("ydb.status_code", code.getCode());
                span.setAttribute("ydb.status_name", code.name());
                span.setAttribute("ydb.issues_count", status.getIssues().length);
                String issuesText = formatIssues(status.getIssues());
                if (issuesText != null) {
                    span.setAttribute("ydb.issues", issuesText);
                }
            }

            endOnce();
        }

        public void finishError(Throwable error) {
            if (error == null) {
                finishStatus(Status.of(StatusCode.CLIENT_INTERNAL_ERROR));
                return;
            }

            span.recordException(error).setAttribute("error.type", normalizeThrowable(error));
            endOnce();
        }

        private void endOnce() {
            if (finished.compareAndSet(false, true)) {
                span.end();
            }
        }

        private static String toErrorType(StatusCode code) {
            return "ydb." + code.name().toLowerCase(Locale.ROOT);
        }

        private static String normalizeThrowable(Throwable error) {
            String className = error.getClass().getSimpleName();
            if (className == null || className.isEmpty()) {
                return "java.throwable";
            }
            return "java." + className.toLowerCase(Locale.ROOT);
        }

        @Nullable
        private static String formatIssues(Issue[] issues) {
            if (issues == null || issues.length == 0) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (Issue issue : issues) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(issue);
                if (sb.length() > MAX_ISSUES_LENGTH) {
                    return sb.substring(0, MAX_ISSUES_LENGTH);
                }
            }
            return sb.toString();
        }
    }
}
