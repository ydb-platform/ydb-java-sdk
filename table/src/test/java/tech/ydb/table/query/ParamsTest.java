package tech.ydb.table.query;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class ParamsTest {

    @Test
    public void empty() {
        Params params = Params.empty();

        Assert.assertTrue(params.isEmpty());

        assertImmutable(params);

        Assert.assertTrue(params.toPb().isEmpty());
    }

    @Test
    public void single() {
        Params params = Params.of("one", PrimitiveValue.newUint32(1));

        Assert.assertFalse(params.isEmpty());

        assertImmutable(params);

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(1, pb.size());

        assertProtoUint32(pb.get("one"), 1);
    }

    @Test
    public void two() {
        Params params = Params.of(
            "one", PrimitiveValue.newUint32(1),
            "two", PrimitiveValue.newUint32(2));

        Assert.assertFalse(params.isEmpty());

        assertImmutable(params);

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(2, pb.size());

        assertProtoUint32(pb.get("one"), 1);
        assertProtoUint32(pb.get("two"), 2);
    }

    @Test
    public void three() {
        Params params = Params.of(
            "one", PrimitiveValue.newUint32(1),
            "two", PrimitiveValue.newUint32(2),
            "three", PrimitiveValue.newUint32(3));

        Assert.assertFalse(params.isEmpty());

        assertImmutable(params);

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(3, pb.size());

        assertProtoUint32(pb.get("one"), 1);
        assertProtoUint32(pb.get("two"), 2);
        assertProtoUint32(pb.get("three"), 3);
    }

    @Test
    public void four() {
        Params params = Params.of(
            "one", PrimitiveValue.newUint32(1),
            "two", PrimitiveValue.newUint32(2),
            "three", PrimitiveValue.newUint32(3),
            "four", PrimitiveValue.newUint32(4));

        Assert.assertFalse(params.isEmpty());

        assertImmutable(params);

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(4, pb.size());

        assertProtoUint32(pb.get("one"), 1);
        assertProtoUint32(pb.get("two"), 2);
        assertProtoUint32(pb.get("three"), 3);
        assertProtoUint32(pb.get("four"), 4);
    }

    @Test
    public void five() {
        Params params = Params.of(
            "one", PrimitiveValue.newUint32(1),
            "two", PrimitiveValue.newUint32(2),
            "three", PrimitiveValue.newUint32(3),
            "four", PrimitiveValue.newUint32(4),
            "five", PrimitiveValue.newUint32(5));

        Assert.assertFalse(params.isEmpty());

        assertImmutable(params);

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(5, pb.size());

        assertProtoUint32(pb.get("one"), 1);
        assertProtoUint32(pb.get("two"), 2);
        assertProtoUint32(pb.get("three"), 3);
        assertProtoUint32(pb.get("four"), 4);
        assertProtoUint32(pb.get("five"), 5);
    }

    @Test
    public void copyOf() {
        Params params1 = Params.copyOf(ImmutableMap.of("name", PrimitiveValue.newText("Jamel")));
        params1.put("age", PrimitiveValue.newUint32(99));

        {
            Map<String, ValueProtos.TypedValue> pb = params1.toPb();
            Assert.assertEquals(2, pb.size());
            assertProtoUtf8(pb.get("name"), "Jamel");
            assertProtoUint32(pb.get("age"), 99);
        }

        Params params2 = Params.copyOf(params1);
        params2.put("phone", PrimitiveValue.newText("+7-916-012-34-56"));

        {
            Map<String, ValueProtos.TypedValue> pb = params2.toPb();
            Assert.assertEquals(3, pb.size());
            assertProtoUtf8(pb.get("name"), "Jamel");
            assertProtoUint32(pb.get("age"), 99);
            assertProtoUtf8(pb.get("phone"), "+7-916-012-34-56");
        }

        // not changed
        {
            Map<String, ValueProtos.TypedValue> pb = params1.toPb();
            Assert.assertEquals(2, pb.size());
            assertProtoUtf8(pb.get("name"), "Jamel");
            assertProtoUint32(pb.get("age"), 99);
        }
    }

    @Test
    public void create() {
        Params params = Params.create();

        for (int i = 0; i < 100; i++) {
            params.put("a" + i, PrimitiveValue.newUint32(i));
        }

        Map<String, ValueProtos.TypedValue> pb = params.toPb();
        Assert.assertEquals(100, pb.size());

        for (int i = 0; i < 100; i++) {
            assertProtoUint32(pb.get("a" + i), i);
        }

        try {
            params.put("a0", PrimitiveValue.newUint32(777));
            Assert.fail("expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("duplicate parameter: a0", e.getMessage());
        }
    }

    private static void assertProtoUtf8(ValueProtos.TypedValue one, String value) {
        Assert.assertEquals(ValueProtos.TypedValue.newBuilder()
                .setType(ProtoType.getText())
                .setValue(ProtoValue.fromText(value))
                .build(), one);
    }

    private static void assertProtoUint32(ValueProtos.TypedValue one, int value) {
        Assert.assertEquals(ValueProtos.TypedValue.newBuilder()
                .setType(ProtoType.getUint32())
                .setValue(ProtoValue.fromUint32(value))
                .build(), one);
    }

    private static void assertImmutable(Params params) {
        try {
            params.put("some", PrimitiveValue.newBool(false));
            Assert.fail("expected exception was not thrown");
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals("cannot put parameter into immutable params map", e.getMessage());
        }
    }
}
