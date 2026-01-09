package tech.ydb.slo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SimpleJdbcConfig.class)
class JdbcSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(JdbcSmokeTest.class);
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void selectOneWorks() {
        String promPgw = System.getenv().getOrDefault("PROM_PGW", "http://localhost:9091");
        MetricsReporter metrics = new MetricsReporter(promPgw, "jdbc-smoke-test");

        try {
            long startTime = System.nanoTime();

            Integer value = jdbc.queryForObject("SELECT 1", Integer.class);

            long endTime = System.nanoTime();
            double latencySeconds = (endTime - startTime) / 1_000_000_000.0;

            log.info("value={}, latency={}s", value, latencySeconds);
            assertEquals(1, value);

            // Записываем успешную метрику
            metrics.recordSuccess("select_one", latencySeconds);
            metrics.setActiveConnections(1);

        } catch (Exception e) {
            log.error("Test failed", e);
            metrics.recordError("select_one", e.getClass().getSimpleName());
            throw e;
        } finally {
            // Отправляем метрики
            metrics.push();
        }
    }
}
