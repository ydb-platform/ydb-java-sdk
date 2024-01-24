package tech.ydb.table.impl;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.TypedValue;
import tech.ydb.table.query.Params;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
public class DataQueryImplTest {

    @Test
    public void params() {
        ImmutableMap<String, Type> types = ImmutableMap.of(
            "name", PrimitiveType.Text,
            "age", PrimitiveType.Uint8);

        ImmutableMap<String, ValueProtos.Type> typesPb = ImmutableMap.of(
            "name", PrimitiveType.Text.toPb(),
            "age", PrimitiveType.Uint8.toPb());

        Params params = new DataQueryImpl.DataQueryParams(types, typesPb)
            .put("name", PrimitiveValue.newText("Jamel"))
            .put("age", PrimitiveValue.newUint8((byte) 99));

        Assert.assertFalse(params.isEmpty());

        Map<String, TypedValue> pb = params.toPb();
        Assert.assertFalse(pb.isEmpty());

        Assert.assertEquals(TypedValue.newBuilder()
                .setType(ProtoType.getText())
                .setValue(ProtoValue.fromText("Jamel"))
                .build(), pb.get("name"));

        Assert.assertEquals(TypedValue.newBuilder()
                .setType(ProtoType.getUint8())
                .setValue(ProtoValue.fromUint8((byte) 99))
                .build(), pb.get("age"));

        // duplicate parameter
        try {
            params.put("name", PrimitiveValue.newText("Another Name"));
            Assert.fail("expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("duplicate parameter: name", e.getMessage());
        }

        // wrong type
        try {
            params.put("name", PrimitiveValue.newUint32(1));
            Assert.fail("expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            // TODO: do not check types anymore
            // assertEquals("types mismatch: expected Utf8, got Uint32", e.getMessage());
            Assert.assertEquals("duplicate parameter: name", e.getMessage());
        }
    }
}
