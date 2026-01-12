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

        double latencySeconds = 0;

        try {
            long startTime = System.nanoTime();

            Integer value = jdbc.queryForObject("SELECT 1", Integer.class);

            long endTime = System.nanoTime();
            latencySeconds = (endTime - startTime) / 1_000_000_000.0;

            log.info("value={}, latency={}ms", value, latencySeconds * 1000);
            assertEquals(1, value);

            metrics.recordSuccess("select_one", latencySeconds);

        } catch (Exception e) {
            log.error("Test failed", e);
            metrics.recordError("select_one", e.getClass().getSimpleName());
            throw e;
        } finally {
            metrics.push();
            metrics.saveToFile("target/test-metrics.txt", latencySeconds);
        }
    }
}