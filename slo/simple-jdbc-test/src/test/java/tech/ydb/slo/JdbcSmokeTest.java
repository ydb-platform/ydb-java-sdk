package tech.ydb.slo;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = SimpleJdbcConfig.class)
class JdbcSmokeTest {

    private static final Logger log = LoggerFactory.getLogger(JdbcSmokeTest.class);

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void selectOneWorks() throws InterruptedException {
        final int maxRetries = 20; // количество попыток
        final long retryDelayMs = 3000; // пауза между попытками 3 секунды
        Integer value = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                value = jdbc.queryForObject("SELECT 1", Integer.class);
                log.info("Connected successfully on attempt {}", attempt);
                break; // успех, выходим из цикла
            } catch (CannotGetJdbcConnectionException e) {
                log.warn("Attempt {} failed, retrying in {} ms...", attempt, retryDelayMs, e);
                Thread.sleep(retryDelayMs);
            }
        }

        if (value == null) {
            throw new RuntimeException("Failed to connect to YDB after " + maxRetries + " attempts");
        }

        log.info("value={}", value);
        assertEquals(1, value);
    }
}
