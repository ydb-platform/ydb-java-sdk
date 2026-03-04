package tech.ydb.opentelemetry;

import java.time.Duration;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
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

    private static InMemorySpanExporter spanExporter;
    private static SdkTracerProvider tracerProvider;
    private static Tracer appTracer;
    private static GrpcTransport transport;

    private QueryClient queryClient;
    private TableClient tableClient;

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
        tableClient = null;
    }

    @After
    public void closeClients() {
        if (tableClient != null) {
            tableClient.close();
        }
        queryClient.close();
    }

    @Test
    public void queryClientSpansHaveBaseAttributes() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();

            Result<QueryTransaction> txCommit = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txCommit.getStatus().expectSuccess();
            Result<QueryInfo> queryInCommitTx = txCommit.getValue().createQuery("SELECT 1").execute().join();
            queryInCommitTx.getStatus().expectSuccess();
            Result<QueryInfo> commitResult = txCommit.getValue().commit().join();
            commitResult.getStatus().expectSuccess();

            Result<QueryTransaction> txRollback = session.beginTransaction(TxMode.SERIALIZABLE_RW).join();
            txRollback.getStatus().expectSuccess();
            Result<QueryInfo> queryInRollbackTx = txRollback.getValue().createQuery("SELECT 1").execute().join();
            queryInRollbackTx.getStatus().expectSuccess();
            Status rollbackStatus = txRollback.getValue().rollback().join();
            rollbackStatus.expectSuccess();
        }

        assertSpanWithBaseAttributes("ydb.CreateSession");
        assertSpanWithBaseAttributes("ydb.ExecuteQuery");
        assertSpanWithBaseAttributes("ydb.Commit");
        assertSpanWithBaseAttributes("ydb.Rollback");
        Assert.assertEquals(1, countFinishedSpans("ydb.CreateSession"));
        Assert.assertEquals(3, countFinishedSpans("ydb.ExecuteQuery"));
        Assert.assertEquals(1, countFinishedSpans("ydb.Commit"));
        Assert.assertEquals(1, countFinishedSpans("ydb.Rollback"));
    }

    @Test
    public void tableClientProxySpansHaveBaseAttributes() {
        tableClient = QueryClient.newTableClient(transport).build();
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

        assertSpanWithBaseAttributes("ydb.CreateSession");
        assertSpanWithBaseAttributes("ydb.ExecuteQuery");
        assertSpanWithBaseAttributes("ydb.Commit");
        assertSpanWithBaseAttributes("ydb.Rollback");
        Assert.assertEquals(1, countFinishedSpans("ydb.CreateSession"));
        Assert.assertEquals(2, countFinishedSpans("ydb.ExecuteQuery"));
        Assert.assertEquals(1, countFinishedSpans("ydb.Commit"));
        Assert.assertEquals(1, countFinishedSpans("ydb.Rollback"));
    }

    @Test
    public void sdkSpanIsChildOfApplicationSpan() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            io.opentelemetry.api.trace.Span appSpan = appTracer.spanBuilder("app.parent").startSpan();
            try (Scope ignored = appSpan.makeCurrent()) {
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
            Assert.assertEquals(1, countFinishedSpans("app.parent"));
            Assert.assertEquals(1, countFinishedSpans("ydb.ExecuteQuery"));
        }
    }

    private int countFinishedSpans(String spanName) {
        int count = 0;
        for (SpanData span : spanExporter.getFinishedSpanItems()) {
            if (spanName.equals(span.getName())) {
                count++;
            }
        }
        return count;
    }

    private void assertSpanWithBaseAttributes(String spanName) {
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        for (SpanData span : spans) {
            if (!spanName.equals(span.getName())) {
                continue;
            }
            Assert.assertEquals(io.opentelemetry.api.trace.SpanKind.CLIENT, span.getKind());
            Assert.assertEquals("ydb", span.getAttributes().get(DB_SYSTEM_NAME));
            Assert.assertEquals(YDB.database(), span.getAttributes().get(DB_NAMESPACE));
            Assert.assertNotNull(span.getAttributes().get(SERVER_ADDRESS));
            Long port = span.getAttributes().get(SERVER_PORT);
            Assert.assertNotNull(port);
            Assert.assertTrue(port > 0L);
            return;
        }
        Assert.fail("Span not found: " + spanName + ", finished spans: " + spans.size());
    }
}
