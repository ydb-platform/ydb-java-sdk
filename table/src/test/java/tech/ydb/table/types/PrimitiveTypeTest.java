package tech.ydb.table.types;

import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos.Value;
import tech.ydb.table.values.PrimitiveValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class PrimitiveTypeTest {

    @Test
    public void timestampTest() {
        PrimitiveValue min = PrimitiveValue.newTimestamp(Instant.EPOCH);
        Assert.assertEquals(min, PrimitiveValue.newTimestamp(0));
        Value minValue = min.toPb();

        Assert.assertEquals(0, minValue.getUint32Value());
        Assert.assertEquals(0, minValue.getUint64Value());
        Assert.assertEquals(0, minValue.getInt32Value());
        Assert.assertEquals(0, minValue.getInt64Value());
        Assert.assertEquals(0, minValue.getLow128());
        Assert.assertEquals(0, minValue.getHigh128());

        PrimitiveValue max = PrimitiveValue.newTimestamp(Instant.parse("2105-12-31T23:59:59.999999Z"));
        Assert.assertEquals(max, PrimitiveValue.newTimestamp(4291747199999999l));
        Value maxValue = max.toPb();

        Assert.assertEquals(0, maxValue.getUint32Value());
        Assert.assertEquals(4291747199999999l, maxValue.getUint64Value());
        Assert.assertEquals(0, maxValue.getInt32Value());
        Assert.assertEquals(0, maxValue.getInt64Value());
        Assert.assertEquals(0, maxValue.getLow128());
        Assert.assertEquals(0, maxValue.getHigh128());

        IllegalArgumentException err1 = Assert.assertThrows(
                IllegalArgumentException.class, () -> PrimitiveValue.newTimestamp(-1)
        );
        Assert.assertEquals("Negative microsSinceEpoch: -1", err1.getMessage());

        IllegalArgumentException err2 = Assert.assertThrows(
                IllegalArgumentException.class, () -> PrimitiveValue.newTimestamp(Instant.EPOCH.minusNanos(1))
        );
        Assert.assertEquals("Instant before epoch: 1969-12-31T23:59:59.999999999Z", err2.getMessage());
    }
}
