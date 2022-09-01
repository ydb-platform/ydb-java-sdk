package tech.ydb.table.types;

import java.math.BigDecimal;

import com.google.common.truth.extensions.proto.ProtoTruth;
import tech.ydb.ValueProtos;
import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.proto.ProtoType;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author Sergey Polovko
 */
public class DecimalTypeTest {

    @Test
    public void contract() {
        DecimalType t = DecimalType.of(13, 2);

        assertThat(t.getKind()).isEqualTo(Type.Kind.DECIMAL);
        assertThat(t.getPrecision()).isEqualTo(13);
        assertThat(t.getScale()).isEqualTo(2);

        assertThat(t).isEqualTo(DecimalType.of(13, 2));
        assertThat(t).isNotEqualTo(DecimalType.of(11, 2));
        assertThat(t).isNotEqualTo(DecimalType.of(13, 1));

        assertThat(t.hashCode()).isEqualTo(DecimalType.of(13, 2).hashCode());
        assertThat(t.hashCode()).isNotEqualTo(DecimalType.of(11, 2).hashCode());
        assertThat(t.hashCode()).isNotEqualTo(DecimalType.of(13, 1).hashCode());

        assertThat(t.toString()).isEqualTo("Decimal(13, 2)");
    }

    @Test
    public void protobuf() {
        DecimalType type = DecimalType.of(10, 5);
        ValueProtos.Type typePb = type.toPb();

        ProtoTruth.assertThat(typePb).isEqualTo(ProtoType.getDecimal(10, 5));

        Type typeX = ProtoType.fromPb(typePb);
        assertThat(type).isEqualTo(typeX);
    }

    @Test
    public void bigDecimalConv() {
        BigDecimal orig, dest;

        orig = new BigDecimal("-1.0");
        dest = DecimalType.of(22, 9).fromBigDecimal(orig).toBigDecimal();
        assertThat(orig.compareTo(dest)).isEqualTo(0);

        orig = new BigDecimal("0.023");
        dest = DecimalType.of(22, 9).fromBigDecimal(orig).toBigDecimal();
        assertThat(orig.compareTo(dest)).isEqualTo(0);

        orig = new BigDecimal("10000.52");
        dest = DecimalType.of(22, 9).fromBigDecimal(orig).toBigDecimal();
        assertThat(orig.compareTo(dest)).isEqualTo(0);
    }
}
