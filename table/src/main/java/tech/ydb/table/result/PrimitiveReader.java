package tech.ydb.table.result;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import tech.ydb.table.values.DecimalValue;


/**
 * @author Sergey Polovko
 */
public interface PrimitiveReader {

    boolean getBool();

    byte getInt8();

    int getUint8();

    short getInt16();

    int getUint16();

    int getInt32();

    long getUint32();

    long getInt64();

    long getUint64();

    float getFloat();

    double getDouble();

    LocalDate getDate();

    LocalDateTime getDatetime();

    Instant getTimestamp();

    Duration getInterval();

    ZonedDateTime getTzDate();

    ZonedDateTime getTzDatetime();

    ZonedDateTime getTzTimestamp();

    byte[] getBytes();

    default String getBytesAsString(Charset charset) {
        return new String(getBytes(), charset);
    }

    UUID getUuid();

    String getText();

    byte[] getYson();

    String getJson();

    String getJsonDocument();

    DecimalValue getDecimal();
}
