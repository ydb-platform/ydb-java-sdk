package tech.ydb.slo;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JdbcSmokeTest {

    @Test
    void testJdbcConnection() throws Exception {
        // Локальный сервер YDB
        String url = "jdbc:ydb:grpc://localhost:2136/local?useTls=false";

        try (Connection connection = DriverManager.getConnection(url);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            rs.next();
            int value = rs.getInt(1);
            System.out.println("value = " + value);

            assertEquals(1, value);
        }
    }
}