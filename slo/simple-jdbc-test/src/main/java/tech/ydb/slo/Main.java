package tech.ydb.slo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public final class Main {

    // Приватный конструктор, чтобы запретить создание экземпляров
    private Main() {
        // Utility class
    }

    public static void main(String[] args) {
        String url = "jdbc:ydb:grpc://localhost:2136" +
                "/local" +
                "?useTls=false";
        try (Connection connection = DriverManager.getConnection(url);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            if (rs.next()) {
                int value = rs.getInt(1);
                System.out.println("value = " + value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
