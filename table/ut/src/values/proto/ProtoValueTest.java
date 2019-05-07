package ru.yandex.ydb.table.values.proto;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class ProtoValueTest {

    @Test
    public void toTzDate() {
        ZonedDateTime dateTime = ProtoValue.toTzDate("2018-08-20,Europe/Moscow");

        assertThat(dateTime.getYear()).isEqualTo(2018);
        assertThat(dateTime.getMonth()).isEqualTo(Month.AUGUST);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(20);
        assertThat(dateTime.getHour()).isEqualTo(0);
        assertThat(dateTime.getMinute()).isEqualTo(0);
        assertThat(dateTime.getSecond()).isEqualTo(0);
        assertThat(dateTime.getNano()).isEqualTo(0);
        assertThat(dateTime.getZone()).isEqualTo(ZoneId.of("Europe/Moscow"));
        assertThat(dateTime.toString()).isEqualTo("2018-08-20T00:00+03:00[Europe/Moscow]");
    }

    @Test
    public void tzDatetime() {
        ZonedDateTime dateTime = ProtoValue.toTzDatetime("2018-09-21T01:23:45.678901,Asia/Novosibirsk");

        assertThat(dateTime.getYear()).isEqualTo(2018);
        assertThat(dateTime.getMonth()).isEqualTo(Month.SEPTEMBER);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(dateTime.getHour()).isEqualTo(1);
        assertThat(dateTime.getMinute()).isEqualTo(23);
        assertThat(dateTime.getSecond()).isEqualTo(45);
        assertThat(dateTime.getNano()).isEqualTo(0);
        assertThat(dateTime.getZone()).isEqualTo(ZoneId.of("Asia/Novosibirsk"));
        assertThat(dateTime.toString()).isEqualTo("2018-09-21T01:23:45+07:00[Asia/Novosibirsk]");
    }

    @Test
    public void tzTimestamp() {
        ZonedDateTime dateTime = ProtoValue.toTzTimestamp("2018-10-22T01:23:45.678901,America/Chicago");

        assertThat(dateTime.getYear()).isEqualTo(2018);
        assertThat(dateTime.getMonth()).isEqualTo(Month.OCTOBER);
        assertThat(dateTime.getDayOfMonth()).isEqualTo(22);
        assertThat(dateTime.getHour()).isEqualTo(1);
        assertThat(dateTime.getMinute()).isEqualTo(23);
        assertThat(dateTime.getSecond()).isEqualTo(45);
        assertThat(dateTime.getNano()).isEqualTo(TimeUnit.MICROSECONDS.toNanos(678901));
        assertThat(dateTime.getZone()).isEqualTo(ZoneId.of("America/Chicago"));
        assertThat(dateTime.toString()).isEqualTo("2018-10-22T01:23:45.678901-05:00[America/Chicago]");
    }
}
