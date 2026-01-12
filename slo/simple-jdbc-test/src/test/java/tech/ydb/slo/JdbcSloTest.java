package tech.ydb.slo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SimpleJdbcConfig.class)
public class JdbcSloTest {

    @Autowired
    DataSource dataSource;

    private static int testDuration;
    private static int readRps;
    private static int writeRps;
    private static int readTimeout;
    private static int writeTimeout;
    private static String promPgw;
    private static int initialDataCount;
    private static int reportPeriod;

    private static final double MAX_P50_LATENCY_MS = 10.0;
    private static final double MAX_P95_LATENCY_MS = 50.0;
    private static final double MAX_P99_LATENCY_MS = 100.0;
    private static final double MIN_SUCCESS_RATE = 99.9;

    @BeforeAll
    static void setup() {
        testDuration = Integer.parseInt(System.getenv().getOrDefault("TEST_DURATION", "60"));
        readRps = Integer.parseInt(System.getenv().getOrDefault("READ_RPS", "100"));
        writeRps = Integer.parseInt(System.getenv().getOrDefault("WRITE_RPS", "10"));
        readTimeout = Integer.parseInt(System.getenv().getOrDefault("READ_TIMEOUT", "1000"));
        writeTimeout = Integer.parseInt(System.getenv().getOrDefault("WRITE_TIMEOUT", "1000"));
        promPgw = System.getenv().getOrDefault("PROM_PGW", "http://localhost:9091");
        reportPeriod = Integer.parseInt(System.getenv().getOrDefault("REPORT_PERIOD", "10000"));
        initialDataCount = Math.max(100, writeRps * testDuration / 10);
    }

    @Test
    void sloFullTest() throws Exception {
        JdbcSloTableContext context = new JdbcSloTableContext(dataSource);
        String jobName = System.getenv().getOrDefault("WORKLOAD_NAME", "jdbc-slo-test");
        MetricsReporter metrics = new MetricsReporter(promPgw, jobName);

        context.createTable(writeTimeout);
        assertTrue(context.tableExists());

        List<SloTableRow> testData = new ArrayList<>();
        for (int i = 0; i < initialDataCount; i++) {
            testData.add(SloTableRow.generate(i));
        }

        writeInitialData(context, testData, writeTimeout, metrics);
        assertTrue(context.selectCount() >= testData.size() * 0.99);

        SloTestResult result = runSloTest(context, testData, metrics);

        validateSloResults(result);

        int finalCount = context.selectCount();
        int expectedMinCount = initialDataCount + (int)(writeRps * testDuration * 0.95);
        assertTrue(finalCount >= expectedMinCount);

        metrics.push();
        metrics.saveToFile("target/test-metrics.txt", result.avgLatencySeconds);
    }

    private void writeInitialData(
            JdbcSloTableContext context,
            List<SloTableRow> data,
            int timeout,
            MetricsReporter metrics
    ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();
        AtomicInteger errors = new AtomicInteger(0);

        for (SloTableRow row : data) {
            futures.add(executor.submit(() -> {
                long start = System.nanoTime();
                try {
                    context.save(row, timeout);
                    metrics.recordSuccess("write_initial", (System.nanoTime() - start) / 1_000_000_000.0);
                } catch (SQLException e) {
                    errors.incrementAndGet();
                    metrics.recordError("write_initial", e.getClass().getSimpleName());
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        if (errors.get() > data.size() * 0.01) {
            throw new RuntimeException("Too many errors: " + errors.get());
        }
    }

    private SloTestResult runSloTest(
            JdbcSloTableContext context,
            List<SloTableRow> testData,
            MetricsReporter metrics
    ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(30);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalLatencyNanos = new AtomicLong(0);
        AtomicInteger totalAttempts = new AtomicInteger(0);
        List<Double> latencies = new CopyOnWriteArrayList<>();

        long testStartTime = System.currentTimeMillis();
        long testEndTime = testStartTime + (testDuration * 1000L);
        long lastReportTime = testStartTime;

        int nextWriteId = testData.size();
        List<Future<?>> activeFutures = new ArrayList<>();

        while (System.currentTimeMillis() < testEndTime) {
            long iterationStart = System.currentTimeMillis();

            for (int i = 0; i < readRps && System.currentTimeMillis() < testEndTime; i++) {
                SloTableRow row = testData.get(ThreadLocalRandom.current().nextInt(testData.size()));

                activeFutures.add(executor.submit(() -> {
                    long opStart = System.nanoTime();
                    try {
                        context.select(row.guid, row.id, readTimeout);
                        long opEnd = System.nanoTime();
                        double latency = (opEnd - opStart) / 1_000_000_000.0;

                        successCount.incrementAndGet();
                        totalLatencyNanos.addAndGet(opEnd - opStart);
                        latencies.add(latency);
                        metrics.recordSuccess("read", latency);
                    } catch (SQLException e) {
                        errorCount.incrementAndGet();
                        metrics.recordError("read", e.getClass().getSimpleName());
                    }
                }));
            }

            for (int i = 0; i < writeRps && System.currentTimeMillis() < testEndTime; i++) {
                final int writeId = nextWriteId++;
                SloTableRow row = SloTableRow.generate(writeId);

                activeFutures.add(executor.submit(() -> {
                    long opStart = System.nanoTime();
                    try {
                        int attempts = context.save(row, writeTimeout);
                        long opEnd = System.nanoTime();
                        double latency = (opEnd - opStart) / 1_000_000_000.0;

                        successCount.incrementAndGet();
                        totalLatencyNanos.addAndGet(opEnd - opStart);
                        totalAttempts.addAndGet(attempts);
                        latencies.add(latency);
                        metrics.recordSuccess("write", latency);
                    } catch (SQLException e) {
                        errorCount.incrementAndGet();
                        metrics.recordError("write", e.getClass().getSimpleName());
                    }
                }));
            }

            long now = System.currentTimeMillis();
            if (now - lastReportTime >= reportPeriod) {
                metrics.push();
                lastReportTime = now;
            }

            long iterationDuration = System.currentTimeMillis() - iterationStart;
            if (iterationDuration < 1000) {
                Thread.sleep(1000 - iterationDuration);
            }
        }

        for (Future<?> future : activeFutures) {
            try {
                future.get(writeTimeout * 2L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception ignored) {
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        return calculateMetrics(successCount.get(), errorCount.get(),
                totalLatencyNanos.get(), totalAttempts.get(), latencies);
    }

    private SloTestResult calculateMetrics(
            int successCount,
            int errorCount,
            long totalLatencyNanos,
            int totalAttempts,
            List<Double> latencies
    ) {
        List<Double> sortedLatencies = new ArrayList<>(latencies);
        sortedLatencies.sort(Double::compareTo);

        int totalRequests = successCount + errorCount;
        double avgLatency = totalRequests > 0 ?
                totalLatencyNanos / 1_000_000_000.0 / totalRequests : 0.0;
        double avgAttempts = successCount > 0 ?
                (double)totalAttempts / successCount : 0.0;

        return new SloTestResult(
                successCount,
                errorCount,
                totalRequests,
                avgLatency,
                avgAttempts,
                getPercentile(sortedLatencies, 0.50),
                getPercentile(sortedLatencies, 0.95),
                getPercentile(sortedLatencies, 0.99),
                totalRequests > 0 ? (double)successCount / totalRequests * 100.0 : 0.0
        );
    }

    private double getPercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil(sortedValues.size() * percentile) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private void validateSloResults(SloTestResult result) {
        assertTrue(result.p50Latency * 1000 <= MAX_P50_LATENCY_MS,
                String.format("P50 latency %.2fms exceeds threshold %.0fms",
                        result.p50Latency * 1000, MAX_P50_LATENCY_MS));

        assertTrue(result.p95Latency * 1000 <= MAX_P95_LATENCY_MS,
                String.format("P95 latency %.2fms exceeds threshold %.0fms",
                        result.p95Latency * 1000, MAX_P95_LATENCY_MS));

        assertTrue(result.p99Latency * 1000 <= MAX_P99_LATENCY_MS,
                String.format("P99 latency %.2fms exceeds threshold %.0fms",
                        result.p99Latency * 1000, MAX_P99_LATENCY_MS));

        assertTrue(result.successRate >= MIN_SUCCESS_RATE,
                String.format("Success rate %.2f%% below threshold %.1f%%",
                        result.successRate, MIN_SUCCESS_RATE));
    }

    static class SloTestResult {
        final int successCount;
        final int errorCount;
        final int totalRequests;
        final double avgLatencySeconds;
        final double avgAttempts;
        final double p50Latency;
        final double p95Latency;
        final double p99Latency;
        final double successRate;

        SloTestResult(int successCount, int errorCount, int totalRequests,
                      double avgLatencySeconds, double avgAttempts,
                      double p50, double p95, double p99, double successRate) {
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.totalRequests = totalRequests;
            this.avgLatencySeconds = avgLatencySeconds;
            this.avgAttempts = avgAttempts;
            this.p50Latency = p50;
            this.p95Latency = p95;
            this.p99Latency = p99;
            this.successRate = successRate;
        }
    }
}