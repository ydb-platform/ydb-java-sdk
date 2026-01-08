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
        Integer value = jdbc.queryForObject("SELECT 1", Integer.class);
        log.info("value={}", value);
        assertEquals(1, value);
    }
}
