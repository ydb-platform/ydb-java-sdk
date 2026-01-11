package tech.ydb.slo;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

public class JdbcSloTableContext {

    private static final String TABLE_NAME = "slo_table";
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int INITIAL_BACKOFF_MS = 100;

    private final DataSource dataSource;

    public JdbcSloTableContext(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTable(int operationTimeoutMs) throws SQLException {
        String createTableSql = String.format("""
            CREATE TABLE `%s` (
                Guid             Utf8,
                Id               Int32,
                PayloadStr       Utf8,
                PayloadDouble    Double,
                PayloadTimestamp Timestamp,
                PRIMARY KEY (Guid, Id)
            )
            """, TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(operationTimeoutMs / 1000);
            stmt.execute(createTableSql);
        }
    }

    public int save(SloTableRow row, int writeTimeoutMs) throws SQLException {
        String upsertSql = String.format("""
            UPSERT INTO `%s` (Guid, Id, PayloadStr, PayloadDouble, PayloadTimestamp)
            VALUES (?, ?, ?, ?, ?)
            """, TABLE_NAME);

        int attempts = 0;
        SQLException lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            attempts++;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(upsertSql)) {

                stmt.setQueryTimeout(writeTimeoutMs / 1000);
                stmt.setString(1, row.guid.toString());
                stmt.setInt(2, row.id);
                stmt.setString(3, row.payloadStr);
                stmt.setDouble(4, row.payloadDouble);
                stmt.setTimestamp(5, new Timestamp(row.payloadTimestamp.getTime()));

                stmt.executeUpdate();
                return attempts;

            } catch (SQLException e) {
                lastException = e;

                if (!isRetryableError(e) || attempts >= MAX_RETRY_ATTEMPTS) {
                    throw new SQLException("Failed to save after " + attempts + " attempts", e);
                }

                try {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempts - 1));
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted during retry", ie);
                }
            }
        }

        throw new SQLException("Failed to save after " + attempts + " attempts", lastException);
    }

    public SloTableRow select(UUID guid, int id, int readTimeoutMs) throws SQLException {
        String selectSql = String.format("""
            SELECT Guid, Id, PayloadStr, PayloadDouble, PayloadTimestamp
            FROM `%s` WHERE Guid = ? AND Id = ?
            """, TABLE_NAME);

        int attempts = 0;
        SQLException lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            attempts++;

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(selectSql)) {

                stmt.setQueryTimeout(readTimeoutMs / 1000);
                stmt.setString(1, guid.toString());
                stmt.setInt(2, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        SloTableRow row = new SloTableRow();
                        row.guid = UUID.fromString(rs.getString("Guid"));
                        row.id = rs.getInt("Id");
                        row.payloadStr = rs.getString("PayloadStr");
                        row.payloadDouble = rs.getDouble("PayloadDouble");
                        row.payloadTimestamp = rs.getTimestamp("PayloadTimestamp");
                        return row;
                    } else {
                        throw new SQLException("Row not found: guid=" + guid + ", id=" + id);
                    }
                }

            } catch (SQLException e) {
                lastException = e;

                if (!isRetryableError(e) || attempts >= MAX_RETRY_ATTEMPTS) {
                    throw new SQLException("Failed to select after " + attempts + " attempts", e);
                }

                try {
                    long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempts - 1));
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted during retry", ie);
                }
            }
        }

        throw new SQLException("Failed to select after " + attempts + " attempts", lastException);
    }

    public int selectCount() throws SQLException {
        String selectSql = String.format("SELECT COUNT(*) as cnt FROM `%s`", TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            return rs.next() ? rs.getInt("cnt") : 0;
        }
    }

    public boolean tableExists() {
        String checkSql = String.format("SELECT 1 FROM `%s` WHERE 1=0", TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isRetryableError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        String sqlState = e.getSQLState();

        if (message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("network") ||
                message.contains("unavailable") ||
                message.contains("overload") ||
                message.contains("too many requests") ||
                message.contains("throttle")) {
            return true;
        }

        if (message.contains("session") && message.contains("expired")) {
            return true;
        }

        if (sqlState != null && sqlState.startsWith("YDB")) {
            return true;
        }

        if (message.contains("already exists") ||
                message.contains("not found") ||
                message.contains("syntax error") ||
                message.contains("constraint") ||
                message.contains("duplicate key")) {
            return false;
        }

        return true;
    }
}