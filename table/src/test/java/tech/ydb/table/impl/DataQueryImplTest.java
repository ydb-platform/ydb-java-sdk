package tech.ydb.table.impl;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.extensions.proto.ProtoTruth;
import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.ValueProtos.TypedValue;
import tech.ydb.table.query.Params;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


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

        assertThat(params.isEmpty())
            .isFalse();

        Map<String, TypedValue> pb = params.toPb();
        assertThat(pb).isNotEmpty();

        ProtoTruth.assertThat(pb.get("name"))
            .isEqualTo(TypedValue.newBuilder()
                .setType(ProtoType.getText())
                .setValue(ProtoValue.fromText("Jamel"))
                .build());

        ProtoTruth.assertThat(pb.get("age"))
            .isEqualTo(TypedValue.newBuilder()
                .setType(ProtoType.getUint8())
                .setValue(ProtoValue.fromUint8((byte) 99))
                .build());

        // duplicate parameter
        try {
            params.put("name", PrimitiveValue.newText("Another Name"));
            fail("expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("duplicate parameter: name", e.getMessage());
        }

        // wrong type
        try {
            params.put("name", PrimitiveValue.newUint32(1));
            fail("expected exception was not thrown");
        } catch (IllegalArgumentException e) {
            // TODO: do not check types anymore
            // assertEquals("types mismatch: expected Utf8, got Uint32", e.getMessage());
            assertEquals("duplicate parameter: name", e.getMessage());
        }
    }
}
