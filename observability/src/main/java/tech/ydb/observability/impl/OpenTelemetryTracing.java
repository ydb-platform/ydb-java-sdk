package tech.ydb.observability.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

import tech.ydb.core.Status;
import tech.ydb.observability.TracingSpan;
import tech.ydb.observability.YdbTracing;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;

public class OpenTelemetryTracing implements YdbTracing {
    private final Tracer tracer;
    private final String serverAddress;
    private final int serverPort;

    public OpenTelemetryTracing(OpenTelemetry openTelemetry, String serverAddress, int serverPort) {
        this.tracer = openTelemetry.getTracer("ydb-java-sdk", "2.3.31");
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public TracingSpan createSpan(String operationName, String query, Params params) {
        String spanName = "ydb." + operationName;

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.CLIENT)
                .setParent(Context.current())
                .startSpan();

        span.setAttribute("db.system.name", "ydb");
        span.setAttribute("server.address", serverAddress);
        span.setAttribute("server.port", serverPort);

        if ("ExecuteQuery".equals(operationName)) {
            if (query != null && !query.isEmpty()) {
                span.setAttribute("db.operation.name", parseOperationType(query));
                span.setAttribute("db.collection.name", extractTableName(query));
                span.setAttribute("db.query.text", query.length() > 1000 ? query.substring(0, 1000) + "..." : query);
            }
            if (params != null) {
                span.setAttribute("db.parameters_count", params.values().size());
            }
        }

        return new OpenTelemetrySpan(span);
    }

    @Override
    public <T> T trace(String operationName, String query, Params params, Supplier<T> operation) {
        TracingSpan tracingSpan = createSpan(operationName, query, params);

        try {
            T result = operation.get();
            if (result instanceof Status) {
                tracingSpan.setStatus((Status) result);
            }

            if (result instanceof DataQueryResult && "ExecuteQuery".equals(operationName)) {
                int rows = ((DataQueryResult) result).getResultSet(0).getRowCount();
                tracingSpan.setAttribute("db.response.returned_rows", rows);
            }
            return result;
        } catch (Throwable t) {
            tracingSpan.recordException(t);
            throw t;
        } finally {
            tracingSpan.end();
        }
    }

    @Override
    public <T> CompletableFuture<T> traceAsync(String operationName, String query, Params params,
                                               Supplier<CompletableFuture<T>> operation) {
        TracingSpan tracingSpan = createSpan(operationName, query, params);

        CompletableFuture<T> future;

        try {
            future = operation.get();
        } catch (Throwable t) {
            tracingSpan.recordException(t);
            tracingSpan.end();
            throw t;
        }

        return future.whenComplete((result, error) -> {
            if (error != null) {
                Throwable actualError = error instanceof CompletionException ? error.getCause() : error;
                tracingSpan.recordException(actualError);
                tracingSpan.setAttribute("error.type", actualError.getClass().getSimpleName());
            } else if (result instanceof Status) {
                tracingSpan.setStatus((Status) result);
            }

            if (result instanceof DataQueryResult && "ExecuteQuery".equals(operationName)) {
                int rows = ((DataQueryResult) result).getResultSet(0).getRowCount();
                tracingSpan.setAttribute("db.response.returned_rows", rows);
            }
            tracingSpan.end();
        });
    }

    private String parseOperationType(String query) {
        if (query == null || query.isEmpty()) {
            return "UNKNOWN";
        }

        String upper = query.trim().toUpperCase();
        if (upper.startsWith("SELECT")) {
            return "SELECT";
        }
        if (upper.startsWith("INSERT")) {
            return "INSERT";
        }
        if (upper.startsWith("UPDATE")) {
            return "UPDATE";
        }
        if (upper.startsWith("DELETE")) {
            return "DELETE";
        }
        if (upper.startsWith("UPSERT")) {
            return "UPSERT";
        }

        return "OTHER";
    }

    private String extractTableName(String query) {
        if (query == null || query.isEmpty()) {
            return "unknown";
        }

        Pattern pattern = Pattern.compile("\\b(FROM|INTO|UPDATE)\\s+([a-zA-Z_][a-zA-Z0-9_]*)",
                Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(query);
        if (matcher.find()) {
            return matcher.group(2);
        }

        return "unknown";
    }

    private static final class OpenTelemetrySpan implements TracingSpan {
        private final Span span;

        OpenTelemetrySpan(Span span) {
            this.span = span;
        }

        @Override
        public TracingSpan setAttribute(String key, String value) {
            span.setAttribute(key, value);
            return this;
        }

        @Override
        public TracingSpan setAttribute(String key, long value) {
            span.setAttribute(key, value);
            return this;
        }

        @Override
        public TracingSpan setAttribute(String key, boolean value) {
            span.setAttribute(key, value);
            return this;
        }

        @Override
        public TracingSpan setStatus(Status status) {
            if (status.isSuccess()) {
                span.setStatus(StatusCode.OK);
                span.setAttribute("db.response.status_code", "SUCCESS");
            } else {
                span.setStatus(StatusCode.ERROR, status.getCode().toString());
                span.setAttribute("db.response.status_code", status.getCode().toString());
                span.setAttribute("error.type", status.getCode().toString());
                if (status.getIssues() != null) {
                    span.setAttribute("ydb.issues", status.getIssues().toString());
                }
            }
            return this;
        }

        @Override
        public TracingSpan recordException(Throwable error) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.setAttribute("error.type", error.getClass().getSimpleName());
            return this;
        }

        @Override
        public void end() {
            span.end();
        }
    }
}
