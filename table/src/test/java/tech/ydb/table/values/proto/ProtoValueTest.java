package tech.ydb.table.values.proto;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.table.values.PrimitiveValue;



/**
 * @author Sergey Polovko
 */
public class ProtoValueTest {

    @Test
    public void toTzDate() {
        ZonedDateTime dateTime = ProtoValue.toTzDate("2018-08-20,Europe/Moscow");

        Assert.assertEquals(2018, dateTime.getYear());
        Assert.assertEquals(Month.AUGUST, dateTime.getMonth());
        Assert.assertEquals(20, dateTime.getDayOfMonth());
        Assert.assertEquals(0, dateTime.getHour());
        Assert.assertEquals(0, dateTime.getMinute());
        Assert.assertEquals(0, dateTime.getSecond());
        Assert.assertEquals(0, dateTime.getNano());
        Assert.assertEquals(ZoneId.of("Europe/Moscow"), dateTime.getZone());
        Assert.assertEquals("2018-08-20T00:00+03:00[Europe/Moscow]", dateTime.toString());
    }

    @Test
    public void tzDatetime() {
        ZonedDateTime dateTime = ProtoValue.toTzDatetime("2018-09-21T01:23:45.678901,Asia/Novosibirsk");

        Assert.assertEquals(2018, dateTime.getYear());
        Assert.assertEquals(Month.SEPTEMBER, dateTime.getMonth());
        Assert.assertEquals(21, dateTime.getDayOfMonth());
        Assert.assertEquals(1, dateTime.getHour());
        Assert.assertEquals(23, dateTime.getMinute());
        Assert.assertEquals(45, dateTime.getSecond());
        Assert.assertEquals(0, dateTime.getNano());
        Assert.assertEquals(ZoneId.of("Asia/Novosibirsk"), dateTime.getZone());
        Assert.assertEquals("2018-09-21T01:23:45+07:00[Asia/Novosibirsk]", dateTime.toString());
    }

    @Test
    public void tzTimestamp() {
        ZonedDateTime dateTime = ProtoValue.toTzTimestamp("2018-10-22T01:23:45.678901,America/Chicago");

        Assert.assertEquals(2018, dateTime.getYear());
        Assert.assertEquals(Month.OCTOBER, dateTime.getMonth());
        Assert.assertEquals(22, dateTime.getDayOfMonth());
        Assert.assertEquals(1, dateTime.getHour());
        Assert.assertEquals(23, dateTime.getMinute());
        Assert.assertEquals(45, dateTime.getSecond());
        Assert.assertEquals(TimeUnit.MICROSECONDS.toNanos(678901), dateTime.getNano());
        Assert.assertEquals(ZoneId.of("America/Chicago"), dateTime.getZone());
        Assert.assertEquals("2018-10-22T01:23:45.678901-05:00[America/Chicago]", dateTime.toString());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void uuid() {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        long low = 0x12d3e89b123e4567L, high = 0x00401714664256a4L;

        PrimitiveValue u1 = ProtoValue.newUuid(uuid);
        PrimitiveValue u2 = ProtoValue.newUuid(UUID.fromString(uuid));
        PrimitiveValue u3 = ProtoValue.newUuid(high, low);

        Assert.assertEquals(u1, u2);
        Assert.assertEquals(u1, u3);

        Assert.assertEquals(uuid, u1.getUuidString());
        Assert.assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), u1.getUuidJdk());
        Assert.assertEquals(low, u1.getUuidLow());
        Assert.assertEquals(high, u1.getUuidHigh());
    }
}
