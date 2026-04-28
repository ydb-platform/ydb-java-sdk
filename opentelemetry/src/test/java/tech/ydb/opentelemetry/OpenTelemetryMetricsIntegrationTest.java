package tech.ydb.opentelemetry;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.After;
import org.testng.annotations.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.test.junit4.YdbHelperRule;

public class OpenTelemetryMetricsIntegrationTest {
    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static final AttributeKey<String> DB_SYSTEM_NAME = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_NAMESPACE = AttributeKey.stringKey("db.namespace");
    private static final AttributeKey<String> SERVER_ADDRESS = AttributeKey.stringKey("server.address");
    private static final AttributeKey<Long> SERVER_PORT = AttributeKey.longKey("server.port");
    private static final AttributeKey<String> OPERATION_NAME = AttributeKey.stringKey("ydb.operation.name");

    private static InMemoryMetricReader metricReader;
    private static SdkMeterProvider meterProvider;
    private static GrpcTransport transport;

    private QueryClient queryClient;

    @BeforeClass
    public static void initTransport() {
        metricReader = InMemoryMetricReader.create();
        meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .build();

        String host = extractHost(YDB.endpoint());
        int port = extractPort(YDB.endpoint());

        transport = GrpcTransport.forEndpoint(YDB.endpoint(), YDB.database())
                .withAuthProvider(new TokenAuthProvider(YDB.authToken()))
                .withMeter(OpenTelemetryMeter.fromOpenTelemetry(openTelemetry, YDB.database(), host, port))
                .build();
    }

    @AfterClass
    public static void closeTransport() throws IOException {
        transport.close();
        meterProvider.close();
        metricReader.close();
    }

    @Before
    public void initClient() {
        queryClient = QueryClient.newClient(transport).build();
    }

    @After
    public void closeClient() {
        queryClient.close();
    }

    @Test
    public void executeQueryRecordsOperationDuration() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
        }

        MetricData metric = findMetric("db.client.operation.duration");
        Assert.assertNotNull("db.client.operation.duration metric not found", metric);

        HistogramPointData point = findHistogramPoint(metric, "ydb.ExecuteQuery");
        Assert.assertNotNull("No histogram point for ydb.ExecuteQuery", point);
        Assert.assertTrue("Duration must be > 0", point.getSum() > 0);
        Assert.assertEquals("ydb", point.getAttributes().get(DB_SYSTEM_NAME));
        Assert.assertEquals(YDB.database(), point.getAttributes().get(DB_NAMESPACE));
        Assert.assertNotNull(point.getAttributes().get(SERVER_ADDRESS));
        Assert.assertNotNull(point.getAttributes().get(SERVER_PORT));
    }

    @Test
    public void commitAndRollbackRecordOperationDuration() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            QueryTransaction txCommit = session.beginTransaction(TxMode.SERIALIZABLE_RW)
                    .join().getValue();
            txCommit.createQuery("SELECT 1").execute().join().getStatus().expectSuccess();
            txCommit.commit().join().getStatus().expectSuccess();

            QueryTransaction txRollback = session.beginTransaction(TxMode.SERIALIZABLE_RW)
                    .join().getValue();
            txRollback.createQuery("SELECT 1").execute().join().getStatus().expectSuccess();
            txRollback.rollback().join().expectSuccess();
        }

        MetricData metric = findMetric("db.client.operation.duration");
        Assert.assertNotNull(metric);

        Assert.assertNotNull("No histogram point for ydb.Commit",
                findHistogramPoint(metric, "ydb.Commit"));
        Assert.assertNotNull("No histogram point for ydb.Rollback",
                findHistogramPoint(metric, "ydb.Rollback"));
    }

    @Test
    public void failedOperationRecordsFailedCounter() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT * FROM __nonexistent_table__", TxMode.NONE)
                    .execute().join();
        }

        MetricData metric = findMetric("ydb.client.operation.failed");
        Assert.assertNotNull("ydb.client.operation.failed metric not found", metric);

        Collection<LongPointData> points = metric.getLongSumData().getPoints();
        Assert.assertFalse("Failed counter must have at least one point", points.isEmpty());
        long total = points.stream().mapToLong(LongPointData::getValue).sum();
        Assert.assertTrue("Failed counter must be > 0", total > 0);
    }

    @Test
    public void sessionPoolMetricsAreReported() {
        // создаём сессию чтобы пул оживился
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
        }

        MetricData metric = findMetric("ydb.query.session.count");
        Assert.assertNotNull("ydb.query.session.count metric not found", metric);
    }

    // --- helpers ---

    private MetricData findMetric(String name) {
        Collection<MetricData> metrics = metricReader.collectAllMetrics();
        for (MetricData m : metrics) {
            if (name.equals(m.getName())) {
                return m;
            }
        }
        return null;
    }

    private HistogramPointData findHistogramPoint(MetricData metric, String operationName) {
        for (HistogramPointData point : metric.getHistogramData().getPoints()) {
            String op = point.getAttributes().get(OPERATION_NAME);
            if (operationName.equals(op)) {
                return point;
            }
        }
        return null;
    }

    private static String extractHost(String endpoint) {
        // endpoint вида grpc://host:port или host:port
        String stripped = endpoint.replaceFirst("grpcs?://", "");
        int colon = stripped.lastIndexOf(':');
        return colon >= 0 ? stripped.substring(0, colon) : stripped;
    }

    private static int extractPort(String endpoint) {
        String stripped = endpoint.replaceFirst("grpcs?://", "");
        int colon = stripped.lastIndexOf(':');
        if (colon >= 0) {
            try {
                return Integer.parseInt(stripped.substring(colon + 1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 2135;
    }
}
