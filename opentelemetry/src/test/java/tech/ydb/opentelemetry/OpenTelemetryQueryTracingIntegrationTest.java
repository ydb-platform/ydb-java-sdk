package tech.ydb.opentelemetry;

import java.time.Duration;
import java.util.List;

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
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.query.result.QueryInfo;
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

            List<SpanData> spans = spanExporter.getFinishedSpanItems();
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
        int count = 0;
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        for (SpanData span : spans) {
            if (!spanName.equals(span.getName())) {
                continue;
            }
            count++;

            Assert.assertEquals(status.getCode().toString(), span.getAttributes().get(STATUS_CODE));
            Assert.assertEquals("ydb_error", span.getAttributes().get(ERROR_TYPE));
            Assert.assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, span.getStatus().getStatusCode());

            assertBaseAttributes(span);
            return;
        }
        Assert.assertEquals("Unexpected count of span  " + spanName, expectedCount, count);
    }

    private void assertSpanOK(String spanName, int expectedCount) {
        int count = 0;
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        for (SpanData span : spans) {
            if (!spanName.equals(span.getName())) {
                continue;
            }
            count++;
            Assert.assertEquals(io.opentelemetry.api.trace.StatusCode.OK, span.getStatus().getStatusCode());
            assertBaseAttributes(span);
        }
        Assert.assertEquals("Unexpected count of span  " + spanName, expectedCount, count);
    }
}
