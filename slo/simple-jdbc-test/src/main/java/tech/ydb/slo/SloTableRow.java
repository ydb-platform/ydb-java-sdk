package tech.ydb.slo;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

/**
 * Представление строки таблицы для SLO тестов
 */
public class SloTableRow {
    public UUID guid;
    public int id;
    public String payloadStr;
    public double payloadDouble;
    public Timestamp payloadTimestamp;

    public SloTableRow() {
    }

    /**
     * Создание строки с заданными значениями
     */
    public SloTableRow(UUID guid, int id, String payloadStr, double payloadDouble, Timestamp payloadTimestamp) {
        this.guid = guid;
        this.id = id;
        this.payloadStr = payloadStr;
        this.payloadDouble = payloadDouble;
        this.payloadTimestamp = payloadTimestamp;
    }

    /**
     * Генерация случайной строки для тестирования
     */
    public static SloTableRow generate(int id) {
        return new SloTableRow(
                UUID.randomUUID(),
                id,
                generatePayloadString(1024), // 1KB payload
                Math.random() * 1000.0,
                new Timestamp(System.currentTimeMillis())
        );
    }

    /**
     * Генерация строки заданного размера
     */
    private static String generatePayloadString(int sizeBytes) {
        StringBuilder sb = new StringBuilder(sizeBytes);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < sizeBytes; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SloTableRow that = (SloTableRow) o;
        return id == that.id &&
                Double.compare(that.payloadDouble, payloadDouble) == 0 &&
                Objects.equals(guid, that.guid) &&
                Objects.equals(payloadStr, that.payloadStr) &&
                Objects.equals(payloadTimestamp, that.payloadTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid, id, payloadStr, payloadDouble, payloadTimestamp);
    }

    @Override
    public String toString() {
        return "SloTableRow{" +
                "guid=" + guid +
                ", id=" + id +
                ", payloadStr.length=" + (payloadStr != null ? payloadStr.length() : 0) +
                ", payloadDouble=" + payloadDouble +
                ", payloadTimestamp=" + payloadTimestamp +
                '}';
    }
}