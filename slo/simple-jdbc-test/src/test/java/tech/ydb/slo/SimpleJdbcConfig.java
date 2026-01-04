package tech.ydb.slo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class SimpleJdbcConfig {

    @Bean
    DataSource dataSource() {
        String url =
                System.getProperty("spring.datasource.url",
                        System.getenv().getOrDefault(
                                "YDB_JDBC_URL",
                                "jdbc:ydb:grpc://localhost:2136/local?useTls=false"
                        ));

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("tech.ydb.jdbc.YdbDriver");
        ds.setUrl(url);

        return ds;
    }

    @Bean
    JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }
}