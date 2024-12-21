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
        Value minValue = min.toPb();

        Assert.assertEquals(0, minValue.getUint32Value());
        Assert.assertEquals(0, minValue.getUint64Value());
        Assert.assertEquals(0, minValue.getInt32Value());
        Assert.assertEquals(0, minValue.getInt64Value());
        Assert.assertEquals(0, minValue.getLow128());
        Assert.assertEquals(0, minValue.getHigh128());

        PrimitiveValue max = PrimitiveValue.newTimestamp(Instant.parse("2105-12-31T23:59:59.999999Z"));
        Value maxValue = max.toPb();

        Assert.assertEquals(0, maxValue.getUint32Value());
        Assert.assertEquals(4291747199999999l, maxValue.getUint64Value());
        Assert.assertEquals(0, maxValue.getInt32Value());
        Assert.assertEquals(0, maxValue.getInt64Value());
        Assert.assertEquals(0, maxValue.getLow128());
        Assert.assertEquals(0, maxValue.getHigh128());
    }
}
