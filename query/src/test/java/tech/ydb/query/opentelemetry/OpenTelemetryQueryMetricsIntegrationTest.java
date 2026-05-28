package tech.ydb.query.opentelemetry;

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
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.auth.TokenAuthProvider;
import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.metrics.OpenTelemetryMeter;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryTransaction;
import tech.ydb.test.junit4.YdbHelperRule;

public class OpenTelemetryQueryMetricsIntegrationTest {
    @ClassRule
    public static final YdbHelperRule YDB = new YdbHelperRule();

    private static final AttributeKey<String> DATABASE = AttributeKey.stringKey("database");
    private static final AttributeKey<String> ENDPOINT = AttributeKey.stringKey("endpoint");
    private static final AttributeKey<String> OPERATION_NAME = AttributeKey.stringKey("operation.name");
    private static final AttributeKey<String> STATUS_CODE = AttributeKey.stringKey("status_code");
    private static final AttributeKey<String> POOL_NAME = AttributeKey.stringKey("ydb.query.session.pool.name");
    private static final AttributeKey<String> SESSION_STATE = AttributeKey.stringKey("ydb.query.session.state");

    private static InMemoryMetricReader metricReader;
    private static SdkMeterProvider meterProvider;
    private static OpenTelemetryMeter ydbMeter;
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

        ydbMeter = OpenTelemetryMeter.fromOpenTelemetry(openTelemetry);

        transport = GrpcTransport.forEndpoint(YDB.endpoint(), YDB.database())
                .withAuthProvider(new TokenAuthProvider(YDB.authToken()))
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
        queryClient = QueryClient.newClient(transport).withMeter(ydbMeter).build();
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

        MetricData metric = findMetric("ydb.client.operation.duration");
        Assert.assertNotNull("ydb.client.operation.duration metric not found", metric);
        Assert.assertEquals("s", metric.getUnit());

        HistogramPointData point = findHistogramPoint(metric, "ExecuteQuery");
        Assert.assertNotNull("No histogram point for ExecuteQuery", point);
        Assert.assertTrue("Duration must be > 0", point.getSum() > 0);
        Assert.assertEquals(YDB.database(), point.getAttributes().get(DATABASE));
        Assert.assertEquals(YDB.endpoint(), point.getAttributes().get(ENDPOINT));
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

        MetricData metric = findMetric("ydb.client.operation.duration");
        Assert.assertNotNull(metric);

        Assert.assertNotNull("No histogram point for Commit",
                findHistogramPoint(metric, "Commit"));
        Assert.assertNotNull("No histogram point for Rollback",
                findHistogramPoint(metric, "Rollback"));
    }

    @Test
    public void failedOperationRecordsFailedCounter() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT * FROM __nonexistent_table__", TxMode.NONE)
                    .execute().join();
        }

        MetricData metric = findMetric("ydb.client.operation.failed");
        Assert.assertNotNull("ydb.client.operation.failed metric not found", metric);
        Assert.assertEquals("{operation}", metric.getUnit());

        Collection<LongPointData> points = metric.getLongSumData().getPoints();
        Assert.assertFalse("Failed counter must have at least one point", points.isEmpty());
        long total = points.stream().mapToLong(LongPointData::getValue).sum();
        Assert.assertTrue("Failed counter must be > 0", total > 0);
        Assert.assertTrue("Failed counter must carry status_code attribute",
                points.stream().anyMatch(p -> p.getAttributes().get(STATUS_CODE) != null));
        Assert.assertTrue("Failed counter must carry database attribute",
                points.stream().anyMatch(p -> YDB.database().equals(p.getAttributes().get(DATABASE))));
        Assert.assertTrue("Failed counter must carry endpoint attribute",
                points.stream().anyMatch(p -> YDB.endpoint().equals(p.getAttributes().get(ENDPOINT))));
    }

    @Test
    public void sessionPoolMetricsAreReported() {
        try (QuerySession session = queryClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
            session.createQuery("SELECT 1", TxMode.NONE).execute().join().getStatus().expectSuccess();
        }

        MetricData count = findMetric("ydb.query.session.count");
        Assert.assertNotNull("ydb.query.session.count metric not found", count);
        Assert.assertEquals("{session}", count.getUnit());
        Assert.assertTrue("session.count must have idle/used buckets",
                count.getLongGaugeData().getPoints().stream()
                        .map(p -> p.getAttributes().get(SESSION_STATE))
                        .anyMatch("idle"::equals));

        MetricData min = findMetric("ydb.query.session.min");
        Assert.assertNotNull("ydb.query.session.min metric not found", min);
        Assert.assertEquals("{session}", min.getUnit());
        Assert.assertTrue("min must have a pool.name attribute",
                min.getLongGaugeData().getPoints().stream()
                        .anyMatch(p -> p.getAttributes().get(POOL_NAME) != null));

        MetricData max = findMetric("ydb.query.session.max");
        Assert.assertNotNull("ydb.query.session.max metric not found", max);
        Assert.assertEquals("{session}", max.getUnit());

        MetricData createTime = findMetric("ydb.query.session.create_time");
        Assert.assertNotNull("ydb.query.session.create_time metric not found", createTime);
        Assert.assertEquals("s", createTime.getUnit());
        Assert.assertFalse("session.create_time must have at least one point",
                createTime.getHistogramData().getPoints().isEmpty());
    }

    @Test
    public void sessionPendingAndTimeoutsMetricsAreCounters() {
        try (QueryClient tinyClient = QueryClient.newClient(transport)
                .withMeter(ydbMeter)
                .sessionPoolMaxSize(1)
                .sessionPoolName("tiny")
                .build()) {
            try (QuerySession s1 = tinyClient.createSession(Duration.ofSeconds(5)).join().getValue()) {
                Result<QuerySession> result = tinyClient.createSession(Duration.ofMillis(500)).join();
                Assert.assertFalse(
                        "waiter must time out (sessionPoolMaxSize=1 with one session held), but got " + result,
                        result.isSuccess());
                Assert.assertEquals(
                        "waiter must complete with CLIENT_DEADLINE_EXPIRED",
                        StatusCode.CLIENT_DEADLINE_EXPIRED, result.getStatus().getCode());
            }
        }

        // Metric reader observes counter increments synchronously, but give the runtime
        // a brief moment in case other threads still hold references in flight.
        Collection<MetricData> snapshot = metricReader.collectAllMetrics();

        MetricData pending = findMetric(snapshot, "ydb.query.session.pending_requests");
        Assert.assertNotNull("ydb.query.session.pending_requests metric not found", pending);
        Assert.assertEquals("{request}", pending.getUnit());
        long pendingTotal = pending.getLongSumData().getPoints().stream()
                .mapToLong(LongPointData::getValue).sum();
        Assert.assertTrue("pending_requests must be > 0", pendingTotal > 0);

        MetricData timeouts = findMetric(snapshot, "ydb.query.session.timeouts");
        Assert.assertNotNull("ydb.query.session.timeouts metric not found", timeouts);
        Assert.assertEquals("{timeout}", timeouts.getUnit());
        long timeoutTotal = timeouts.getLongSumData().getPoints().stream()
                .mapToLong(LongPointData::getValue).sum();
        Assert.assertTrue("timeouts must be > 0", timeoutTotal > 0);
    }

    private MetricData findMetric(String name) {
        return findMetric(metricReader.collectAllMetrics(), name);
    }

    private MetricData findMetric(Collection<MetricData> metrics, String name) {
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

}
