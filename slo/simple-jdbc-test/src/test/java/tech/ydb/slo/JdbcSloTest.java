package tech.ydb.slo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Полноценный SLO тест для YDB JDBC Driver
 * Реализует алгоритм из C# SloTableContext
 */
@SpringBootTest(classes = SimpleJdbcConfig.class)
class JdbcSloTest {
    private static final Logger log = LoggerFactory.getLogger(JdbcSloTest.class);

    @Autowired
    DataSource dataSource;

    // Конфигурация теста из переменных окружения
    private static int testDuration;          // Длительность теста в секундах
    private static int readRps;               // Read операций в секунду
    private static int writeRps;              // Write операций в секунду
    private static int readTimeout;           // Timeout для read в ms
    private static int writeTimeout;          // Timeout для write в ms
    private static String promPgw;            // Prometheus Push Gateway URL
    private static int initialDataCount;     // Количество начальных записей
    private static int reportPeriod;         // Период отправки метрик в ms

    // SLO пороги (как в C# тесте)
    private static final double MAX_P50_LATENCY_MS = 10.0;
    private static final double MAX_P95_LATENCY_MS = 50.0;
    private static final double MAX_P99_LATENCY_MS = 100.0;
    private static final double MIN_SUCCESS_RATE = 99.9;

    @BeforeAll
    static void setup() {
        // Читаем конфигурацию из переменных окружения
        testDuration = Integer.parseInt(System.getenv().getOrDefault("TEST_DURATION", "60"));
        readRps = Integer.parseInt(System.getenv().getOrDefault("READ_RPS", "1000"));
        writeRps = Integer.parseInt(System.getenv().getOrDefault("WRITE_RPS", "100"));
        readTimeout = Integer.parseInt(System.getenv().getOrDefault("READ_TIMEOUT", "1000"));
        writeTimeout = Integer.parseInt(System.getenv().getOrDefault("WRITE_TIMEOUT", "1000"));
        promPgw = System.getenv().getOrDefault("PROM_PGW", "http://localhost:9091");
        reportPeriod = Integer.parseInt(System.getenv().getOrDefault("REPORT_PERIOD", "10000"));

        // Количество начальных записей = write_rps * duration / 10
        // (меньше чем будет создано, для предварительного заполнения)
        initialDataCount = Math.max(1000, writeRps * testDuration / 10);

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║          YDB JDBC SLO Test Configuration                   ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  Duration:          {} seconds", String.format("%6d", testDuration));
        log.info("║  Read RPS:          {}", String.format("%6d", readRps));
        log.info("║  Write RPS:         {}", String.format("%6d", writeRps));
        log.info("║  Read Timeout:      {} ms", String.format("%6d", readTimeout));
        log.info("║  Write Timeout:     {} ms", String.format("%6d", writeTimeout));
        log.info("║  Initial Data:      {} rows", String.format("%6d", initialDataCount));
        log.info("║  Report Period:     {} ms", String.format("%6d", reportPeriod));
        log.info("║  Prometheus:        {}", promPgw);
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  SLO Thresholds:                                           ║");
        log.info("║    P50 Latency:     < {} ms", MAX_P50_LATENCY_MS);
        log.info("║    P95 Latency:     < {} ms", MAX_P95_LATENCY_MS);
        log.info("║    P99 Latency:     < {} ms", MAX_P99_LATENCY_MS);
        log.info("║    Success Rate:    > {}%", MIN_SUCCESS_RATE);
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    @Test
    void sloFullTest() throws Exception {
        JdbcSloTableContext context = new JdbcSloTableContext(dataSource);
        MetricsReporter metrics = new MetricsReporter(promPgw, "jdbc-slo-full-test");

        log.info("");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  PHASE 1: TABLE INITIALIZATION");
        log.info("═══════════════════════════════════════════════════════════");

        // 1. Создание таблицы
        log.info("Creating table...");
        context.createTable(writeTimeout);
        assertTrue(context.tableExists(), "Table should exist after creation");

        log.info("");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  PHASE 2: DATA PREPARATION");
        log.info("═══════════════════════════════════════════════════════════");

        // 2. Подготовка тестовых данных
        log.info("Preparing {} test records...", initialDataCount);
        List<SloTableRow> testData = prepareTestData(initialDataCount);

        // 3. Инициальная запись данных
        log.info("Writing initial data ({} rows)...", testData.size());
        long writeStart = System.currentTimeMillis();
        writeInitialData(context, testData, writeTimeout, metrics);
        long writeEnd = System.currentTimeMillis();
        log.info("✅ Initial write completed in {} ms", writeEnd - writeStart);

        // 4. Верификация начальных данных
        int actualCount = context.selectCount();
        log.info("Verification: {} rows in table", actualCount);
        assertTrue(actualCount >= testData.size() * 0.99,
                "At least 99% of initial data should be written");

        log.info("");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  PHASE 3: SLO TEST EXECUTION ({} seconds)", testDuration);
        log.info("═══════════════════════════════════════════════════════════");

        // 5. Запуск SLO теста
        SloTestResult result = runSloTest(context, testData, metrics);

        log.info("");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  PHASE 4: RESULTS VALIDATION");
        log.info("═══════════════════════════════════════════════════════════");

        // 6. Валидация результатов
        validateSloResults(result);

        // 7. Финальная верификация данных
        int finalCount = context.selectMaxId();
        int expectedMinCount = initialDataCount + (int)(writeRps * testDuration * 0.95);
        log.info("Final data check: {} records (expected >= {})", finalCount, expectedMinCount);
        assertTrue(finalCount >= expectedMinCount,
                "At least 95% of writes should succeed");

        log.info("");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("  PHASE 5: METRICS EXPORT");
        log.info("═══════════════════════════════════════════════════════════");

        // 8. Отправка финальных метрик
        metrics.push();
        metrics.saveToFile("target/test-metrics.txt", result.avgLatencySeconds);
        log.info("✅ Metrics exported");

        // 9. Вывод итогового отчёта
        printDetailedSummary(result);
    }

    /**
     * PHASE 2: Подготовка тестовых данных
     */
    private List<SloTableRow> prepareTestData(int count) {
        log.info("Generating {} SloTableRow objects...", count);
        List<SloTableRow> data = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            data.add(SloTableRow.generate(i));

            if ((i + 1) % 10000 == 0) {
                log.info("  Generated {}/{} rows", i + 1, count);
            }
        }

        log.info("✅ Test data prepared");
        return data;
    }

    /**
     * PHASE 2: Инициальная запись данных (как в C#)
     */
    private void writeInitialData(
            JdbcSloTableContext context,
            List<SloTableRow> data,
            int timeout,
            MetricsReporter metrics
    ) throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();
        AtomicInteger written = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        for (SloTableRow row : data) {
            futures.add(executor.submit(() -> {
                long start = System.nanoTime();
                try {
                    int attempts = context.save(row, timeout);
                    long end = System.nanoTime();
                    double latency = (end - start) / 1_000_000_000.0;

                    metrics.recordSuccess("write_initial", latency);

                    int count = written.incrementAndGet();
                    if (count % 1000 == 0) {
                        log.info("  Written {}/{} rows (attempts: {})", count, data.size(), attempts);
                    }

                } catch (SQLException e) {
                    errors.incrementAndGet();
                    metrics.recordError("write_initial", e.getClass().getSimpleName());
                    log.warn("  Failed to write row {}: {}", row.id, e.getMessage());
                }
                return null;
            }));
        }

        // Ожидание завершения всех записей
        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        log.info("Initial write summary: {} written, {} errors", written.get(), errors.get());

        if (errors.get() > data.size() * 0.01) {
            throw new RuntimeException("Too many errors during initial write: " + errors.get());
        }
    }

    /**
     * PHASE 3: Основной SLO тест (аналог C# while loop)
     */
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
        long lastLogTime = testStartTime;

        int nextWriteId = testData.size();
        List<Future<?>> activeFutures = new ArrayList<>();

        log.info("Starting SLO test loop...");

        // Основной цикл (как в C# WHILE NOW() < endTime)
        while (System.currentTimeMillis() < testEndTime) {
            long iterationStart = System.currentTimeMillis();

            // READ операции (параллельно)
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

            // WRITE операции (параллельно)
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

            // Периодическая отправка метрик в Prometheus (как в C#)
            long now = System.currentTimeMillis();
            if (now - lastReportTime >= reportPeriod) {
                metrics.push();
                lastReportTime = now;
            }

            // Логирование прогресса каждые 10 секунд
            if (now - lastLogTime >= 10000) {
                long elapsed = (now - testStartTime) / 1000;
                long remaining = (testEndTime - now) / 1000;
                int totalOps = successCount.get() + errorCount.get();
                double currentRps = totalOps / (double)elapsed;

                log.info("Progress: {}s elapsed, {}s remaining | {} ops ({} RPS) | {} errors",
                        elapsed, remaining, totalOps, String.format("%.1f", currentRps), errorCount.get());
                lastLogTime = now;
            }

            // Rate limiting - ждём до следующей секунды
            long iterationDuration = System.currentTimeMillis() - iterationStart;
            if (iterationDuration < 1000) {
                Thread.sleep(1000 - iterationDuration);
            }
        }

        log.info("Test loop completed. Waiting for pending operations...");

        // Ожидание завершения всех операций
        for (Future<?> future : activeFutures) {
            try {
                future.get(writeTimeout * 2L, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
            } catch (Exception e) {
                log.warn("Operation failed: {}", e.getMessage());
            }
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.SECONDS);
        if (!terminated) {
            log.warn("⚠️ Executor did not terminate in time");
            executor.shutdownNow();
        }

        log.info("✅ All operations completed");

        // Расчёт метрик
        return calculateMetrics(successCount.get(), errorCount.get(),
                totalLatencyNanos.get(), totalAttempts.get(), latencies);
    }

    /**
     * Расчёт финальных метрик (percentiles, success rate)
     */
    private SloTestResult calculateMetrics(
            int successCount,
            int errorCount,
            long totalLatencyNanos,
            int totalAttempts,
            List<Double> latencies
    ) {
        log.info("Calculating metrics from {} operations...", latencies.size());

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

    /**
     * PHASE 4: Валидация SLO (Assert на пороги)
     */
    private void validateSloResults(SloTestResult result) {
        log.info("Validating SLO compliance...");

        boolean allPassed = true;

        // Проверка P50 latency
        if (result.p50Latency * 1000 <= MAX_P50_LATENCY_MS) {
            log.info("  ✅ P50 Latency: {:.2f}ms (threshold: {:.0f}ms)",
                    result.p50Latency * 1000, MAX_P50_LATENCY_MS);
        } else {
            log.error("  ❌ P50 Latency: {:.2f}ms EXCEEDS threshold {:.0f}ms",
                    result.p50Latency * 1000, MAX_P50_LATENCY_MS);
            allPassed = false;
        }

        // Проверка P95 latency
        if (result.p95Latency * 1000 <= MAX_P95_LATENCY_MS) {
            log.info("  ✅ P95 Latency: {:.2f}ms (threshold: {:.0f}ms)",
                    result.p95Latency * 1000, MAX_P95_LATENCY_MS);
        } else {
            log.error("  ❌ P95 Latency: {:.2f}ms EXCEEDS threshold {:.0f}ms",
                    result.p95Latency * 1000, MAX_P95_LATENCY_MS);
            allPassed = false;
        }

        // Проверка P99 latency
        if (result.p99Latency * 1000 <= MAX_P99_LATENCY_MS) {
            log.info("  ✅ P99 Latency: {:.2f}ms (threshold: {:.0f}ms)",
                    result.p99Latency * 1000, MAX_P99_LATENCY_MS);
        } else {
            log.error("  ❌ P99 Latency: {:.2f}ms EXCEEDS threshold {:.0f}ms",
                    result.p99Latency * 1000, MAX_P99_LATENCY_MS);
            allPassed = false;
        }

        // Проверка Success Rate
        if (result.successRate >= MIN_SUCCESS_RATE) {
            log.info("  ✅ Success Rate: {:.2f}% (threshold: {:.1f}%)",
                    result.successRate, MIN_SUCCESS_RATE);
        } else {
            log.error("  ❌ Success Rate: {:.2f}% BELOW threshold {:.1f}%",
                    result.successRate, MIN_SUCCESS_RATE);
            allPassed = false;
        }

        if (allPassed) {
            log.info("✅ ALL SLO CHECKS PASSED!");
        } else {
            log.error("❌ SOME SLO CHECKS FAILED!");
        }

        // Assert для JUnit
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

    /**
     * Вывод детального отчёта
     */
    private void printDetailedSummary(SloTestResult result) {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║              SLO TEST SUMMARY REPORT                       ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  Operations:                                               ║");
        log.info("║    Total Requests:    {}", String.format("%10d", result.totalRequests));
        log.info("║    Successful:        {} ({:.2f}%)",
                String.format("%10d", result.successCount), result.successRate);
        log.info("║    Failed:            {}", String.format("%10d", result.errorCount));
        log.info("║    Avg Attempts:      {}", String.format("%10.2f", result.avgAttempts));
        log.info("║                                                            ║");
        log.info("║  Latency Metrics:                                          ║");
        log.info("║    Average:           {} ms", String.format("%10.2f", result.avgLatencySeconds * 1000));
        log.info("║    P50:               {} ms", String.format("%10.2f", result.p50Latency * 1000));
        log.info("║    P95:               {} ms", String.format("%10.2f", result.p95Latency * 1000));
        log.info("║    P99:               {} ms", String.format("%10.2f", result.p99Latency * 1000));
        log.info("║                                                            ║");
        log.info("║  Throughput:                                               ║");
        log.info("║    Actual RPS:        {}",
                String.format("%10.1f", (double)result.totalRequests / testDuration));
        log.info("║    Expected RPS:      {}",
                String.format("%10d", readRps + writeRps));
        log.info("╚════════════════════════════════════════════════════════════╝");
    }

    /**
     * Результаты SLO теста
     */
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
