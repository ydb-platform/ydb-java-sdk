package tech.ydb.query.opentelemetry;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.tracing.OpenTelemetryTracer;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
import tech.ydb.query.tools.SessionRetryContext;
import tech.ydb.table.Session;
import tech.ydb.table.TableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.query.Params;
import tech.ydb.table.transaction.TableTransaction;
import tech.ydb.test.junit4.YdbHelperRule;

public class OpenTelemetryQueryTracingIntegrationTest {
    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static final AttributeKey<String> DB_SYSTEM_NAME = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_NAMESPACE = AttributeKey.stringKey("db.namespace");
    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");

    private static final AttributeKey<String> STATUS_CODE = AttributeKey.stringKey("db.response.status_code");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    private static InMemorySpanExporter spanExporter;
    private static SdkTracerProvider tracerProvider;
    private static Tracer appTracer;
    private static GrpcTransport transport;

    private QueryClient queryClient;

    @BeforeClass
    public static void initTransport() {
        spanExporter = InMemorySpanExporter.create();
        tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();
        appTracer = openTelemetry.getTracer("test.app");
        transport = GrpcTransport.forEndpoint(YDB.endpoint(), YDB.database())
                .withAuthProvider(new TokenAuthProvider(YDB.authToken()))
                .withTracer(OpenTelemetryTracer.fromOpenTelemetry(openTelemetry))
                .build();
    }

    @AfterClass
    public static void closeTransport() {
        transport.close();
        tracerProvider.close();
        spanExporter.close();
    }

    @Before
    public void initClients() {
        spanExporter.reset();
        queryClient = QueryClient.newClient(transport).build();
    }

    @After
    public void closeClients() {
        queryClient.close();
    }

    /**
     * Finished spans for assertions. {@link SimpleSpanProcessor} exports to {@link InMemorySpanExporter}
     * synchronously from {@code Span.end()}, so {@link SdkTracerProvider#forceFlush()} is unnecessary here
     * and was flaky under load (timeouts / non-success completion codes).
     */
    private static List<SpanData> exportedSpans() {
        return spanExporter.getFinishedSpanItems();
    }

    /**
     * After {@code CompletableFuture#join()} the outer {@code ydb.RunWithRetry} span may not be
     * ended/exported yet; poll until it appears (or timeout) before counting it in assertions.
     */
    private List<SpanData> exportedSpansWhenRunWithRetryPresent() {
        long deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (System.currentTimeMillis() < deadlineMs) {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            boolean hasOuter = spans.stream().anyMatch(s -> "ydb.RunWithRetry".equals(s.getName()));
            if (hasOuter) {
                return spanExporter.getFinishedSpanItems();
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
        return exportedSpans();
    }

    @Test
    public void queryClientSpansHaveBaseAttributes() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            assertSpanOK("ydb.CreateSession", 1);
            assertSpanOK("ydb.ExecuteQuery", 0);
            assertSpanOK("ydb.Commit", 0);
            assertSpanOK("ydb.Rollback", 0);

            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();

            assertSpanOK("ydb.ExecuteQuery", 1);
            assertSpanOK("ydb.Commit", 0);
            assertSpanOK("ydb.Rollback", 0);

            Result<QueryTransaction> txCommit = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txCommit.getStatus().expectSuccess();

            assertSpanOK("ydb.ExecuteQuery", 1);
            assertSpanOK("ydb.Commit", 0);
            assertSpanOK("ydb.Rollback", 0);

            Result<QueryInfo> queryInCommitTx = txCommit.getValue().createQuery("SELECT 1").execute().join();
            queryInCommitTx.getStatus().expectSuccess();
            Result<QueryInfo> commitResult = txCommit.getValue().commit().join();
            commitResult.getStatus().expectSuccess();

            assertSpanOK("ydb.ExecuteQuery", 2);
            assertSpanOK("ydb.Commit", 1);
            assertSpanOK("ydb.Rollback", 0);

            Result<QueryTransaction> txRollback = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txRollback.getStatus().expectSuccess();
            Result<QueryInfo> queryInRollbackTx = txRollback.getValue().createQuery("SELECT 1").execute().join();
            queryInRollbackTx.getStatus().expectSuccess();
            Status rollbackStatus = txRollback.getValue().rollback().join();
            rollbackStatus.expectSuccess();
        }

        assertSpanOK("ydb.CreateSession", 1);
        assertSpanOK("ydb.ExecuteQuery", 3);
        assertSpanOK("ydb.Commit", 1);
        assertSpanOK("ydb.Rollback", 1);
    }

    @Test
    public void queryClientErrorSpans() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            assertSpanOK("ydb.CreateSession", 1);

            Status status = session.createQuery("SELECT 'u'u - 1;", TxMode.NONE).execute().join().getStatus();
            Assert.assertEquals(StatusCode.GENERIC_ERROR, status.getCode());

            assertSpanError("ydb.ExecuteQuery", 1, status);
        }
    }

    @Test
    public void tableClientProxySpansHaveBaseAttributes() {
        try (TableClient tableClient = QueryClient.newTableClient(transport).build()) {
            try (Session session = tableClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                TableTransaction txCommit = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                Result<DataQueryResult> queryInCommitTx = txCommit.executeDataQuery("SELECT 1", Params.empty()).join();
                queryInCommitTx.getStatus().expectSuccess();
                Status commitStatus = txCommit.commit().join();
                commitStatus.expectSuccess();

                TableTransaction txRollback = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                Result<DataQueryResult> queryInRollbackTx = txRollback.executeDataQuery("SELECT 1", Params.empty()).join();
                queryInRollbackTx.getStatus().expectSuccess();
                Status rollbackStatus = txRollback.rollback().join();
                rollbackStatus.expectSuccess();
            }
        }

        assertSpanOK("ydb.CreateSession", 1);
        assertSpanOK("ydb.ExecuteQuery", 2);
        assertSpanOK("ydb.Commit", 1);
        assertSpanOK("ydb.Rollback", 1);
    }

    @Test
    public void sdkSpanIsChildOfApplicationSpan() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            io.opentelemetry.api.trace.Span appSpan = appTracer.spanBuilder("app.parent").startSpan();
            try (Scope scope = appSpan.makeCurrent()) {
                Assert.assertNotNull(scope);
                session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
            } finally {
                appSpan.end();
            }

            List<SpanData> spans = exportedSpans();
            SpanData sdkSpan = null;
            for (SpanData span : spans) {
                if ("ydb.ExecuteQuery".equals(span.getName())) {
                    sdkSpan = span;
                    break;
                }
            }

            Assert.assertNotNull("SDK span not found", sdkSpan);
            Assert.assertEquals(appSpan.getSpanContext().getTraceId(), sdkSpan.getTraceId());
            Assert.assertEquals(appSpan.getSpanContext().getSpanId(), sdkSpan.getParentSpanId());
        }
    }

    @Test
    public void retrySpanIsParentForRpcSpans() {
        AtomicInteger attempt = new AtomicInteger();
        SessionRetryContext retryContext = SessionRetryContext.create(queryClient)
                .maxRetries(1)
                .backoffSlot(Duration.ofMillis(1))
                .fastBackoffSlot(Duration.ofMillis(1))
                .build();

        io.opentelemetry.api.trace.Span appSpan = appTracer.spanBuilder("app.parent.retry").startSpan();
        try (Scope scope = appSpan.makeCurrent()) {
            Assert.assertNotNull(scope);
            Status status = retryContext.supplyStatus(session -> {
                if (attempt.getAndIncrement() == 0) {
                    return CompletableFuture.completedFuture(Status.of(StatusCode.OVERLOADED));
                }
                return session.createQuery("SELECT 1", TxMode.NONE).execute().thenApply(Result::getStatus);
            }).join();
            status.expectSuccess();
        } finally {
            appSpan.end();
        }

        List<SpanData> spans = exportedSpans();
        SpanData querySpan = null;
        String executeSpanId = null;
        Set<String> retrySpanIds = new HashSet<>();
        int executeSpansCount = 0;
        int retrySpansCount = 0;

        for (SpanData span : spans) {
            if ("ydb.RunWithRetry".equals(span.getName())) {
                executeSpansCount++;
                executeSpanId = span.getSpanId();
                Assert.assertEquals(appSpan.getSpanContext().getTraceId(), span.getTraceId());
                Assert.assertEquals(appSpan.getSpanContext().getSpanId(), span.getParentSpanId());
            }
            if ("ydb.Try".equals(span.getName())) {
                retrySpansCount++;
                retrySpanIds.add(span.getSpanId());
                Assert.assertEquals(appSpan.getSpanContext().getTraceId(), span.getTraceId());
            }
            if ("ydb.ExecuteQuery".equals(span.getName())) {
                querySpan = span;
            }
        }

        Assert.assertEquals(1, executeSpansCount);
        Assert.assertNotNull(executeSpanId);
        Assert.assertEquals(2, retrySpansCount);
        for (SpanData span : spans) {
            if ("ydb.Try".equals(span.getName())) {
                Assert.assertEquals(executeSpanId, span.getParentSpanId());
            }
        }
        Assert.assertNotNull("Retry should produce query rpc span", querySpan);
        Assert.assertEquals(appSpan.getSpanContext().getTraceId(), querySpan.getTraceId());
        Assert.assertTrue("RPC span parent must be Retry span",
                retrySpanIds.contains(querySpan.getParentSpanId()));
    }

    @Test
    public void tableProxyRetrySpanIsParentForRpcSpans() {
        AtomicInteger attempt = new AtomicInteger();
        io.opentelemetry.api.trace.Span appSpan = appTracer.spanBuilder("app.parent.table.retry").startSpan();
        try (Scope scope = appSpan.makeCurrent()) {
            Assert.assertNotNull(scope);
            try (TableClient tableClient = QueryClient.newTableClient(transport).build()) {
                tech.ydb.table.SessionRetryContext retryContext = tech.ydb.table.SessionRetryContext.create(tableClient)
                        .maxRetries(1)
                        .backoffSlot(Duration.ofMillis(1))
                        .fastBackoffSlot(Duration.ofMillis(1))
                        .build();

                Status status = retryContext.supplyStatus(session -> {
                    if (attempt.getAndIncrement() == 0) {
                        return CompletableFuture.completedFuture(Status.of(StatusCode.OVERLOADED));
                    }
                    TableTransaction tx = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                    Result<DataQueryResult> queryResult = tx.executeDataQuery("SELECT 1", Params.empty()).join();
                    queryResult.getStatus().expectSuccess();
                    return tx.commit();
                }).join();
                status.expectSuccess();
            }
        } finally {
            appSpan.end();
        }

        assertSpanOK("ydb.CreateSession", 1);
        assertSpanOK("ydb.ExecuteQuery", 1);
        assertSpanOK("ydb.Commit", 1);

        List<SpanData> spans = exportedSpansWhenRunWithRetryPresent();
        String executeSpanId = null;
        int executeSpansCount = 0;
        Set<String> retrySpanIds = new HashSet<>();

        for (SpanData span : spans) {
            if ("ydb.RunWithRetry".equals(span.getName())) {
                executeSpansCount++;
                executeSpanId = span.getSpanId();
                Assert.assertEquals(appSpan.getSpanContext().getTraceId(), span.getTraceId());
                Assert.assertEquals(appSpan.getSpanContext().getSpanId(), span.getParentSpanId());
            }
            if ("ydb.Try".equals(span.getName())) {
                retrySpanIds.add(span.getSpanId());
                Assert.assertEquals(appSpan.getSpanContext().getTraceId(), span.getTraceId());
            }
        }
        Assert.assertEquals(1, executeSpansCount);
        Assert.assertNotNull(executeSpanId);
        for (SpanData span : spans) {
            if ("ydb.Try".equals(span.getName())) {
                Assert.assertEquals(executeSpanId, span.getParentSpanId());
            }
        }
        Assert.assertEquals(2, retrySpanIds.size());

        int createSessionChildren = 0;
        int executeQueryChildren = 0;
        int commitChildren = 0;

        for (SpanData span : spans) {
            if ("ydb.CreateSession".equals(span.getName()) && retrySpanIds.contains(span.getParentSpanId())) {
                createSessionChildren++;
            }
            if ("ydb.ExecuteQuery".equals(span.getName()) && retrySpanIds.contains(span.getParentSpanId())) {
                executeQueryChildren++;
            }
            if ("ydb.Commit".equals(span.getName()) && retrySpanIds.contains(span.getParentSpanId())) {
                commitChildren++;
            }
        }

        Assert.assertTrue("CreateSession span must be child of Retry", createSessionChildren >= 1);
        Assert.assertTrue("ExecuteQuery span must be child of Retry", executeQueryChildren >= 1);
        Assert.assertTrue("Commit span must be child of Retry", commitChildren >= 1);
    }

    @Test
    public void nonRetryableExceptionProducesErrorRetrySpan() {
        SessionRetryContext retryContext = SessionRetryContext.create(queryClient)
                .maxRetries(5)
                .backoffSlot(Duration.ofMillis(1))
                .fastBackoffSlot(Duration.ofMillis(1))
                .build();

        RuntimeException thrown;
        try {
            retryContext.supplyStatus(session -> {
                throw new IllegalStateException("boom");
            }).join();
            throw new AssertionError("Exception expected");
        } catch (RuntimeException ex) {
            thrown = ex;
        }

        Assert.assertNotNull(thrown.getCause());
        Assert.assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertSpanOK("ydb.CreateSession", 1);
        assertSpanOK("ydb.ExecuteQuery", 0);

        List<SpanData> spans = exportedSpansWhenRunWithRetryPresent();
        int executeSpans = 0;
        int errorExecuteSpans = 0;
        int retrySpans = 0;
        int errorRetrySpans = 0;
        for (SpanData span : spans) {
            if ("ydb.RunWithRetry".equals(span.getName())) {
                executeSpans++;
                if (io.opentelemetry.api.trace.StatusCode.ERROR.equals(span.getStatus().getStatusCode())) {
                    errorExecuteSpans++;
                }
            }
            if (!"ydb.Try".equals(span.getName())) {
                continue;
            }
            retrySpans++;
            if (io.opentelemetry.api.trace.StatusCode.ERROR.equals(span.getStatus().getStatusCode())) {
                errorRetrySpans++;
                Assert.assertEquals(IllegalStateException.class.getName(),
                        span.getAttributes().get(ERROR_TYPE));
            }
        }
        Assert.assertEquals(1, executeSpans);
        Assert.assertEquals(1, errorExecuteSpans);
        Assert.assertEquals(1, retrySpans);
        Assert.assertEquals(1, errorRetrySpans);
    }

    @Test
    public void retryableUnexpectedResultExceptionRetriesAndSetsErrorType() {
        AtomicInteger attempt = new AtomicInteger();
        SessionRetryContext retryContext = SessionRetryContext.create(queryClient)
                .maxRetries(5)
                .backoffSlot(Duration.ofMillis(1))
                .fastBackoffSlot(Duration.ofMillis(1))
                .build();

        Status status = retryContext.supplyStatus(session -> {
            if (attempt.getAndIncrement() == 0) {
                throw new UnexpectedResultException("retryable", Status.of(StatusCode.OVERLOADED));
            }
            return session.createQuery("SELECT 1", TxMode.NONE).execute().thenApply(Result::getStatus);
        }).join();
        status.expectSuccess();

        assertSpanOK("ydb.ExecuteQuery", 1);

        List<SpanData> spans = exportedSpansWhenRunWithRetryPresent();
        int executeSpans = 0;
        int okExecuteSpans = 0;
        int retrySpans = 0;
        int errorRetrySpans = 0;
        int okRetrySpans = 0;
        for (SpanData span : spans) {
            if ("ydb.RunWithRetry".equals(span.getName())) {
                executeSpans++;
                if (io.opentelemetry.api.trace.StatusCode.OK.equals(span.getStatus().getStatusCode())) {
                    okExecuteSpans++;
                }
            }
            if (!"ydb.Try".equals(span.getName())) {
                continue;
            }
            retrySpans++;
            if (io.opentelemetry.api.trace.StatusCode.ERROR.equals(span.getStatus().getStatusCode())) {
                errorRetrySpans++;
                Assert.assertEquals("ydb_error", span.getAttributes().get(ERROR_TYPE));
                Assert.assertEquals(StatusCode.OVERLOADED.toString(), span.getAttributes().get(STATUS_CODE));
            } else if (io.opentelemetry.api.trace.StatusCode.OK.equals(span.getStatus().getStatusCode())) {
                okRetrySpans++;
            }
        }
        Assert.assertEquals(1, executeSpans);
        Assert.assertEquals(1, okExecuteSpans);
        Assert.assertEquals(2, retrySpans);
        Assert.assertEquals(1, errorRetrySpans);
        Assert.assertEquals(1, okRetrySpans);
    }

    @Test
    public void tableProxySpansAreChildrenOfApplicationSpan() {
        io.opentelemetry.api.trace.Span appSpan = appTracer.spanBuilder("app.parent.table").startSpan();
        try (Scope scope = appSpan.makeCurrent()) {
            Assert.assertNotNull(scope);
            try (TableClient tableClient = QueryClient.newTableClient(transport).build()) {
                try (Session session = tableClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                    TableTransaction tx = session.createNewTransaction(TxMode.SERIALIZABLE_RW);
                    Result<DataQueryResult> queryResult = tx.executeDataQuery("SELECT 1", Params.empty()).join();
                    queryResult.getStatus().expectSuccess();
                    Status commitStatus = tx.commit().join();
                    commitStatus.expectSuccess();
                }
            }
        } finally {
            appSpan.end();
        }

        List<SpanData> spans = exportedSpans();
        int matched = 0;
        for (SpanData span : spans) {
            if (!span.getName().startsWith("ydb.")) {
                continue;
            }
            Assert.assertEquals(appSpan.getSpanContext().getTraceId(), span.getTraceId());
            Assert.assertEquals(appSpan.getSpanContext().getSpanId(), span.getParentSpanId());
            matched++;
        }
        Assert.assertEquals(3, matched);
    }

    private void assertBaseAttributes(SpanData span) {
        Assert.assertEquals(io.opentelemetry.api.trace.SpanKind.CLIENT, span.getKind());
        Assert.assertEquals("ydb", span.getAttributes().get(DB_SYSTEM_NAME));
        Assert.assertEquals(YDB.database(), span.getAttributes().get(DB_NAMESPACE));
        Assert.assertNotNull(span.getAttributes().get(SERVER_ADDRESS));
        Long port = span.getAttributes().get(SERVER_PORT);
        Assert.assertNotNull(port);
        Assert.assertTrue(port > 0L);
    }

    private void assertSpanError(String spanName, int expectedCount, Status status) {
        long deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        int count = 0;
        while (true) {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            count = 0;
            for (SpanData span : spans) {
                if (!spanName.equals(span.getName())) {
                    continue;
                }
                count++;

                Assert.assertEquals(status.getCode().toString(), span.getAttributes().get(STATUS_CODE));
                Assert.assertEquals("ydb_error", span.getAttributes().get(ERROR_TYPE));
                Assert.assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, span.getStatus().getStatusCode());

                assertBaseAttributes(span);
            }
            if (count == expectedCount) {
                return;
            }
            if (System.currentTimeMillis() >= deadlineMs) {
                Assert.assertEquals("Unexpected count of span  " + spanName, expectedCount, count);
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    private void assertSpanOK(String spanName, int expectedCount) {
        long deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        int count = 0;
        while (true) {
            List<SpanData> spans = spanExporter.getFinishedSpanItems();
            count = 0;
            for (SpanData span : spans) {
                if (!spanName.equals(span.getName())) {
                    continue;
                }
                count++;
                Assert.assertEquals(io.opentelemetry.api.trace.StatusCode.OK, span.getStatus().getStatusCode());
                assertBaseAttributes(span);
            }
            if (count == expectedCount) {
                return;
            }
            if (System.currentTimeMillis() >= deadlineMs) {
                Assert.assertEquals("Unexpected count of span  " + spanName, expectedCount, count);
                return;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }
}
