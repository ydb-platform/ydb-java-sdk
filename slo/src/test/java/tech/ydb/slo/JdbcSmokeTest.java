package tech.ydb.slo;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcSmokeTest {

    @Test
    public void simpleSelect1() throws Exception {

        String url =
                "jdbc:ydb:grpc://localhost:2136" +
                        "/local" +
                        "?useTls=false";

        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT 1 AS value")) {

            rs.next();
            int value = rs.getInt("value");

            System.out.println("value = " + value);
        }
    }
}
