package tech.ydb.table.types;

import java.math.BigDecimal;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.proto.ProtoType;


/**
 * @author Sergey Polovko
 */
public class DecimalTypeTest {

    @Test
    public void contract() {
        DecimalType t = DecimalType.of(13, 2);

        Assert.assertEquals(Type.Kind.DECIMAL, t.getKind());
        Assert.assertEquals(13, t.getPrecision());
        Assert.assertEquals(2, t.getScale());

        Assert.assertEquals(DecimalType.of(13, 2), t);
        Assert.assertNotEquals(DecimalType.of(11, 2), t);
        Assert.assertNotEquals(DecimalType.of(13, 1), t);

        Assert.assertEquals(DecimalType.of(13, 2).hashCode(), t.hashCode());
        Assert.assertNotEquals(DecimalType.of(11, 2).hashCode(), t.hashCode());
        Assert.assertNotEquals(DecimalType.of(13, 1).hashCode(), t.hashCode());

        Assert.assertEquals("Decimal(13, 2)", t.toString());
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(10, 5);
        ValueProtos.Type typePb = type.toPb();

        Assert.assertEquals(ProtoType.getDecimal(10, 5), typePb);

        Type typeX = ProtoType.fromPb(typePb);
        Assert.assertEquals(typeX, type);
    }

    @Test
    public void bigDecimalConv() {
        BigDecimal orig, dest;

        orig = new BigDecimal("-1.0");
        dest = DecimalType.of(22, 9).newValue(orig).toBigDecimal();
        Assert.assertEquals(0, orig.compareTo(dest));

        orig = new BigDecimal("0.023");
        dest = DecimalType.of(22, 9).newValue(orig).toBigDecimal();
        Assert.assertEquals(0, orig.compareTo(dest));

        orig = new BigDecimal("10000.52");
        dest = DecimalType.of(22, 9).newValue(orig).toBigDecimal();
        Assert.assertEquals(0, orig.compareTo(dest));
    }
}
