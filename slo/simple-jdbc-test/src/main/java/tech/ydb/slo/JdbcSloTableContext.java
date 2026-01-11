package tech.ydb.slo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * JDBC реализация SLO Table Context для тестирования YDB
 * Аналог C# SloTableContext<YdbDataSource>
 */
public class JdbcSloTableContext {
    private static final Logger log = LoggerFactory.getLogger(JdbcSloTableContext.class);

    private static final String TABLE_NAME = "slo_table";
    private static final String JOB_NAME = "JdbcSlo";

    // Конфигурация retry
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final int INITIAL_BACKOFF_MS = 100;

    private final DataSource dataSource;

    public JdbcSloTableContext(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Создание таблицы для SLO тестов
     * Аналог Create() в C#
     */
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

        log.info("Creating table: {} with timeout {}ms", TABLE_NAME, operationTimeoutMs);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(operationTimeoutMs / 1000);
            stmt.execute(createTableSql);

            log.info("✅ Table created successfully");
        } catch (SQLException e) {
            log.error("❌ Failed to create table", e);
            throw e;
        }
    }

    /**
     * Запись данных в таблицу (UPSERT) с retry logic
     * Аналог Save() в C# - использует ExecuteAsync с автоматическим retry
     *
     * @return количество попыток выполнения (для метрик)
     */
    public int save(SloTableRow row, int writeTimeoutMs) throws SQLException {
        String upsertSql = String.format("""
            UPSERT INTO `%s` (Guid, Id, PayloadStr, PayloadDouble, PayloadTimestamp)
            VALUES (?, ?, ?, ?, ?)
            """, TABLE_NAME);

        int attempts = 0;
        SQLException lastException = null;

        // Retry logic с exponential backoff (как в C# RetryPolicy)
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

                if (attempts > 1) {
                    log.debug("✅ Saved row after {} attempts: guid={}, id={}",
                            attempts, row.guid, row.id);
                } else {
                    log.trace("Saved row: guid={}, id={}", row.guid, row.id);
                }

                return attempts;

            } catch (SQLException e) {
                lastException = e;

                // Определяем, стоит ли повторять
                if (!isRetryableError(e) || attempts >= MAX_RETRY_ATTEMPTS) {
                    log.error("❌ Non-retryable error or max attempts reached: {}", e.getMessage());
                    throw new SQLException("Failed to save after " + attempts + " attempts", e);
                }

                log.warn("⚠️ Save attempt {}/{} failed: {}. Retrying...",
                        attempts, MAX_RETRY_ATTEMPTS, e.getMessage());

                // Exponential backoff: 100ms, 200ms, 400ms, 800ms, 1600ms
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

    /**
     * Чтение данных из таблицы (SELECT) с retry logic
     * Аналог Select() в C# - использует OpenRetryableConnectionAsync
     */
    public SloTableRow select(UUID guid, int id, int readTimeoutMs) throws SQLException {
        String selectSql = String.format("""
            SELECT Guid, Id, PayloadStr, PayloadDouble, PayloadTimestamp
            FROM `%s` WHERE Guid = ? AND Id = ?
            """, TABLE_NAME);

        int attempts = 0;
        SQLException lastException = null;

        // Retry для read операций (как RetryableConnection в C#)
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

                        if (attempts > 1) {
                            log.debug("✅ Selected row after {} attempts: guid={}, id={}",
                                    attempts, guid, id);
                        }

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

                log.warn("⚠️ Select attempt {}/{} failed: {}. Retrying...",
                        attempts, MAX_RETRY_ATTEMPTS, e.getMessage());

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

    /**
     * Подсчёт максимального ID в таблице
     * Аналог SelectCount() в C#
     */
    public int selectMaxId() throws SQLException {
        String selectSql = String.format("SELECT MAX(Id) as MaxId FROM `%s`", TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            if (rs.next()) {
                int maxId = rs.getInt("MaxId");
                log.debug("Max ID in table: {}", maxId);
                return maxId;
            } else {
                return 0;
            }
        }
    }

    /**
     * Подсчёт общего количества записей
     */
    public int selectCount() throws SQLException {
        String selectSql = String.format("SELECT COUNT(*) as cnt FROM `%s`", TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            if (rs.next()) {
                int count = rs.getInt("cnt");
                log.debug("Total rows in table: {}", count);
                return count;
            } else {
                return 0;
            }
        }
    }

    /**
     * Проверка существования таблицы
     */
    public boolean tableExists() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Пытаемся выполнить простой запрос
            stmt.executeQuery("SELECT 1 FROM `" + TABLE_NAME + "` LIMIT 1").close();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Удаление таблицы
     */
    public void dropTable() throws SQLException {
        String dropSql = String.format("DROP TABLE `%s`", TABLE_NAME);

        log.info("Dropping table: {}", TABLE_NAME);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(dropSql);
            log.info("✅ Table dropped successfully");
        } catch (SQLException e) {
            log.error("❌ Failed to drop table", e);
            throw e;
        }
    }

    /**
     * Определяет, является ли ошибка временной (retryable)
     * Аналог логики RetryPolicy в C#
     */
    private boolean isRetryableError(SQLException e) {
        String message = e.getMessage().toLowerCase();
        String sqlState = e.getSQLState();

        // Transient errors (временные сбои)
        if (message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("network") ||
                message.contains("unavailable")) {
            return true;
        }

        // Overloaded состояния YDB
        if (message.contains("overload") ||
                message.contains("too many requests") ||
                message.contains("throttle")) {
            return true;
        }

        // Session expired
        if (message.contains("session") && message.contains("expired")) {
            return true;
        }

        // YDB specific error codes
        if (sqlState != null) {
            // CLIENT_INTERNAL_ERROR, UNAVAILABLE, OVERLOADED
            if (sqlState.startsWith("YDB")) {
                return true;
            }
        }

        // Non-retryable: schema errors, constraint violations, syntax errors
        if (message.contains("already exists") ||
                message.contains("not found") ||
                message.contains("syntax error") ||
                message.contains("constraint") ||
                message.contains("duplicate key")) {
            return false;
        }

        // По умолчанию считаем retryable
        return true;
    }

    public String getJobName() {
        return JOB_NAME;
    }

    public String getTableName() {
        return TABLE_NAME;
    }
}