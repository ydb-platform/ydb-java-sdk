package tech.ydb.table.result.impl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.google.protobuf.ByteString;
import com.google.protobuf.NullValue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ValueReader;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ProtoValueReaderTest {

    private ValueProtos.Type primitiveType(ValueProtos.Type.PrimitiveTypeId typeId) {
        return ValueProtos.Type.newBuilder().setTypeId(typeId).build();
    }

    private ValueProtos.Type decimalType(int precision, int scale) {
        return ValueProtos.Type.newBuilder().setDecimalType(
                ValueProtos.DecimalType.newBuilder().setScale(scale).setPrecision(precision)
        ).build();
    }

    private ValueProtos.Type optionalType(ValueProtos.Type inner) {
        return ValueProtos.Type.newBuilder().setOptionalType(
                ValueProtos.OptionalType.newBuilder().setItem(inner).build()
        ).build();
    }

    private ValueProtos.Value nullValue() {
        return ValueProtos.Value.newBuilder().setNullFlagValue(NullValue.NULL_VALUE).build();
    }

    private ValueProtos.TypedValue typedValue(ValueProtos.Type type, ValueProtos.Value value) {
        return ValueProtos.TypedValue.newBuilder().setType(type).setValue(value).build();
    }

    private void assertNullPointerException(String message, ThrowingRunnable runnable) {
        NullPointerException ex = Assert.assertThrows(NullPointerException.class, runnable);
        Assert.assertEquals(message, ex.getMessage());
    }

    private void assertTypesMismatch(String expected, String type, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
        Assert.assertEquals("types mismatch, expected " + expected + ", but was " + type, ex.getMessage());
    }

    private void assertCannotCall(String method, String type, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
        Assert.assertEquals("cannot call " + method + ", actual type: " + type, ex.getMessage());
    }

    @Test
    public void emptyTextValueTest() {
        ValueProtos.Type textType = primitiveType(ValueProtos.Type.PrimitiveTypeId.UTF8);
        ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(optionalType(textType), nullValue()));

        Assert.assertSame(ProtoOptionalValueReader.class, vr.getClass());

        assertTypesMismatch("Bool", "Text", vr::getBool);

        assertTypesMismatch("Int8", "Text", vr::getInt8);
        assertTypesMismatch("Int16", "Text", vr::getInt16);
        assertTypesMismatch("Int32", "Text", vr::getInt32);
        assertTypesMismatch("Int64", "Text", vr::getInt64);

        assertTypesMismatch("Uint8", "Text", vr::getUint8);
        assertTypesMismatch("Uint16", "Text", vr::getUint16);
        assertTypesMismatch("Uint32", "Text", vr::getUint32);
        assertTypesMismatch("Uint64", "Text", vr::getUint64);

        assertTypesMismatch("Float", "Text", vr::getFloat);
        assertTypesMismatch("Double", "Text", vr::getDouble);

        Assert.assertNull(vr.getText());

        assertTypesMismatch("Bytes", "Text", vr::getBytes);
        assertTypesMismatch("Bytes", "Text", () -> vr.getBytesAsString(StandardCharsets.UTF_8));

        assertTypesMismatch("Json", "Text", vr::getJson);
        assertTypesMismatch("JsonDocument", "Text", vr::getJsonDocument);
        assertTypesMismatch("Yson", "Text", vr::getYson);
        assertTypesMismatch("Uuid", "Text", vr::getUuid);

        assertTypesMismatch("Date", "Text", vr::getDate);
        assertTypesMismatch("Datetime", "Text", vr::getDatetime);
        assertTypesMismatch("Timestamp", "Text", vr::getTimestamp);
        assertTypesMismatch("Interval", "Text", vr::getInterval);

        assertTypesMismatch("Date32", "Text", vr::getDate32);
        assertTypesMismatch("Datetime64", "Text", vr::getDatetime64);
        assertTypesMismatch("Timestamp64", "Text", vr::getTimestamp64);
        assertTypesMismatch("Interval64", "Text", vr::getInterval64);

        assertTypesMismatch("TzDate", "Text", vr::getTzDate);
        assertTypesMismatch("TzDatetime", "Text", vr::getTzDatetime);
        assertTypesMismatch("TzTimestamp", "Text", vr::getTzTimestamp);

        assertTypesMismatch("Decimal", "Text", vr::getDecimal);

        assertCannotCall("getStructMembersCount", "Text", vr::getStructMembersCount);
        assertCannotCall("getStructMember", "Text", () -> vr.getStructMember(0));
        assertCannotCall("getStructMember", "Text", () -> vr.getStructMember("id"));
        assertCannotCall("getStructMemberName", "Text", () -> vr.getStructMemberName(0));

        assertCannotCall("getDictItemsCount", "Text", vr::getDictItemsCount);
        assertCannotCall("getDictKey", "Text", () -> vr.getDictKey(0));
        assertCannotCall("getDictValue", "Text", () -> vr.getDictValue(0));

        assertCannotCall("getTupleElementsCount", "Text", vr::getTupleElementsCount);
        assertCannotCall("getTupleElement", "Text", () -> vr.getTupleElement(0));

        assertCannotCall("getListItemsCount", "Text", vr::getListItemsCount);
        assertCannotCall("getListItem", "Text", () -> vr.getListItem(0));

        assertCannotCall("getVariantTypeIndex", "Text", vr::getVariantTypeIndex);
        assertCannotCall("getVariantItem", "Text", vr::getVariantItem);
    }

    @Test
    public void emptyInt32ValueTest() {
        ValueProtos.Type uint32Type = primitiveType(ValueProtos.Type.PrimitiveTypeId.UINT32);
        ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(optionalType(uint32Type), nullValue()));

        Assert.assertSame(ProtoOptionalValueReader.class, vr.getClass());

        assertTypesMismatch("Bool", "Uint32", vr::getBool);

        assertTypesMismatch("Int8", "Uint32", vr::getInt8);
        assertTypesMismatch("Int16", "Uint32", vr::getInt16);
        assertTypesMismatch("Int32", "Uint32", vr::getInt32);
        assertTypesMismatch("Int64", "Uint32", vr::getInt64);

        assertTypesMismatch("Uint8", "Uint32", vr::getUint8);
        assertTypesMismatch("Uint16", "Uint32", vr::getUint16);
        assertNullPointerException("cannot call getUint32 for NULL value", vr::getUint32);
        assertTypesMismatch("Uint64", "Uint32", vr::getUint64);

        assertTypesMismatch("Float", "Uint32", vr::getFloat);
        assertTypesMismatch("Double", "Uint32", vr::getDouble);

        assertTypesMismatch("Text", "Uint32", vr::getText);

        assertTypesMismatch("Bytes", "Uint32", vr::getBytes);
        assertTypesMismatch("Bytes", "Uint32", () -> vr.getBytesAsString(StandardCharsets.UTF_8));

        assertTypesMismatch("Json", "Uint32", vr::getJson);
        assertTypesMismatch("JsonDocument", "Uint32", vr::getJsonDocument);
        assertTypesMismatch("Yson", "Uint32", vr::getYson);
        assertTypesMismatch("Uuid", "Uint32", vr::getUuid);

        assertTypesMismatch("Date", "Uint32", vr::getDate);
        assertTypesMismatch("Datetime", "Uint32", vr::getDatetime);
        assertTypesMismatch("Timestamp", "Uint32", vr::getTimestamp);
        assertTypesMismatch("Interval", "Uint32", vr::getInterval);

        assertTypesMismatch("Date32", "Uint32", vr::getDate32);
        assertTypesMismatch("Datetime64", "Uint32", vr::getDatetime64);
        assertTypesMismatch("Timestamp64", "Uint32", vr::getTimestamp64);
        assertTypesMismatch("Interval64", "Uint32", vr::getInterval64);

        assertTypesMismatch("TzDate", "Uint32", vr::getTzDate);
        assertTypesMismatch("TzDatetime", "Uint32", vr::getTzDatetime);
        assertTypesMismatch("TzTimestamp", "Uint32", vr::getTzTimestamp);

        assertTypesMismatch("Decimal", "Uint32", vr::getDecimal);

        assertCannotCall("getStructMembersCount", "Uint32", vr::getStructMembersCount);
        assertCannotCall("getStructMember", "Uint32", () -> vr.getStructMember(0));
        assertCannotCall("getStructMember", "Uint32", () -> vr.getStructMember("id"));
        assertCannotCall("getStructMemberName", "Uint32", () -> vr.getStructMemberName(0));

        assertCannotCall("getDictItemsCount", "Uint32", vr::getDictItemsCount);
        assertCannotCall("getDictKey", "Uint32", () -> vr.getDictKey(0));
        assertCannotCall("getDictValue", "Uint32", () -> vr.getDictValue(0));

        assertCannotCall("getTupleElementsCount", "Uint32", vr::getTupleElementsCount);
        assertCannotCall("getTupleElement", "Uint32", () -> vr.getTupleElement(0));

        assertCannotCall("getListItemsCount", "Uint32", vr::getListItemsCount);
        assertCannotCall("getListItem", "Uint32", () -> vr.getListItem(0));

        assertCannotCall("getVariantTypeIndex", "Uint32", vr::getVariantTypeIndex);
        assertCannotCall("getVariantItem", "Uint32", vr::getVariantItem);
    }

    @Test
    public void booleanTypesTest() {
        ValueProtos.Value bool = ValueProtos.Value.newBuilder().setBoolValue(true).build();

        ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.BOOL));
        ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, bool));
        Assert.assertTrue(vr.getBool());
        assertTypesMismatch("Int16", "Bool", vr::getInt16);
        Assert.assertEquals("Some[true]", vr.toString());

        ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
        assertNullPointerException("cannot call getBool for NULL value", nr::getBool);
        Assert.assertEquals("Empty[]", nr.toString());
    }

    @Test
    public void floatTypesTest() {
        ValueProtos.Value float32 = ValueProtos.Value.newBuilder().setFloatValue(-34.12354f).build();

        ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.FLOAT));
        ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, float32));
        Assert.assertEquals(-34.12354f, vr.getFloat(), 1e-6);
        assertTypesMismatch("Double", "Float", vr::getDouble);
        Assert.assertEquals("Some[-34.12354]", vr.toString());

        ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
        assertNullPointerException("cannot call getFloat for NULL value", nr::getFloat);
        Assert.assertEquals("Empty[]", nr.toString());
    }

    @Test
    public void doubleTypesTest() {
        ValueProtos.Value double64 = ValueProtos.Value.newBuilder().setDoubleValue(-34.1235443543d).build();

        ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.DOUBLE));
        ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, double64));
        Assert.assertEquals(-34.1235443543d, vr.getDouble(), 1e-11);
        assertTypesMismatch("Float", "Double", vr::getFloat);
        Assert.assertEquals("Some[-34.1235443543]", vr.toString());

        ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
        assertNullPointerException("cannot call getDouble for NULL value", nr::getDouble);
        Assert.assertEquals("Empty[]", nr.toString());
    }

    @Test
    public void int32TypesTest() {
        // -994445 == FFF0D373 == LocalDate[-753-04-21] (Rome foundation)
        ValueProtos.Value int32 = ValueProtos.Value.newBuilder().setInt32Value(-994445).build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INT8));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int32));
            Assert.assertEquals(0x73, vr.getInt8());
            assertTypesMismatch("Int16", "Int8", vr::getInt16);
            assertTypesMismatch("Int32", "Int8", vr::getInt32);
            assertTypesMismatch("Int64", "Int8", vr::getInt64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getInt8 for NULL value", nr::getInt8);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INT16));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int32));
            assertTypesMismatch("Int8", "Int16", vr::getInt8);
            Assert.assertEquals((short) 0xD373, vr.getInt16());
            assertTypesMismatch("Int32", "Int16", vr::getInt32);
            assertTypesMismatch("Int64", "Int16", vr::getInt64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getInt16 for NULL value", nr::getInt16);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INT32));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int32));
            assertTypesMismatch("Int8", "Int32", vr::getInt8);
            assertTypesMismatch("Int16", "Int32", vr::getInt16);
            Assert.assertEquals(-994445, vr.getInt32());
            assertTypesMismatch("Int64", "Int32", vr::getInt64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getInt32 for NULL value", nr::getInt32);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.DATE32));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int32));
            Assert.assertEquals(LocalDate.of(-753, 04, 21), vr.getDate32());
            assertTypesMismatch("Date", "Date32", vr::getDate);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getDate32());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }

    @Test
    public void int64TypesTest() {
        // -12239384400 == FFFFFFFD 2679D0B0 == LocalDateTime[1582-02-24T11:00:00+00]  (Gregorian calendar bull)
        ValueProtos.Value int64 = ValueProtos.Value.newBuilder().setInt64Value(-12239384400l).build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INT64));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            assertTypesMismatch("Int8", "Int64", vr::getInt8);
            assertTypesMismatch("Int16", "Int64", vr::getInt16);
            assertTypesMismatch("Int32", "Int64", vr::getInt32);
            Assert.assertEquals(-12239384400l, vr.getInt64());
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getInt64 for NULL value", nr::getInt64);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.DATETIME64));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            Assert.assertEquals(LocalDateTime.of(1582, 02, 24, 11, 00, 00), vr.getDatetime64());
            assertTypesMismatch("Datetime", "Datetime64", vr::getDatetime);
            assertTypesMismatch("Int64", "Datetime64", vr::getInt64);
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getDatetime64());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.TIMESTAMP64));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            Assert.assertEquals(Instant.ofEpochSecond(-12240, 615600000), vr.getTimestamp64());
            assertTypesMismatch("Timestamp", "Timestamp64", vr::getTimestamp);
            assertTypesMismatch("Int64", "Timestamp64", vr::getInt64);
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getTimestamp64());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INTERVAL));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            Assert.assertEquals(Duration.ofSeconds(-12240, 615600000), vr.getInterval());
            assertTypesMismatch("Interval64", "Interval", vr::getInterval64);
            assertTypesMismatch("Int64", "Interval", vr::getInt64);
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getInterval());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.INTERVAL64));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            Assert.assertEquals(Duration.ofSeconds(-12240, 615600000), vr.getInterval64());
            assertTypesMismatch("Interval", "Interval64", vr::getInterval);
            assertTypesMismatch("Int64", "Interval64", vr::getInt64);
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getInterval64());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }

    @Test
    public void uint32TypesTest() {
        ValueProtos.Value uint32 = ValueProtos.Value.newBuilder().setUint32Value(0xFFF0D373).build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.UINT8));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, uint32));
            Assert.assertEquals(0x73, vr.getUint8());
            assertTypesMismatch("Uint16", "Uint8", vr::getUint16);
            assertTypesMismatch("Uint32", "Uint8", vr::getUint32);
            assertTypesMismatch("Uint64", "Uint8", vr::getUint64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getUint8 for NULL value", nr::getUint8);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.UINT16));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, uint32));
            assertTypesMismatch("Uint8", "Uint16", vr::getUint8);
            Assert.assertEquals(0xD373, vr.getUint16());
            assertTypesMismatch("Uint32", "Uint16", vr::getUint32);
            assertTypesMismatch("Uint64", "Uint16", vr::getUint64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getUint16 for NULL value", nr::getUint16);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.UINT32));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, uint32));
            assertTypesMismatch("Uint8", "Uint32", vr::getUint8);
            assertTypesMismatch("Uint16", "Uint32", vr::getUint16);
            Assert.assertEquals(0xFFF0D373l, vr.getUint32());
            assertTypesMismatch("Uint64", "Uint32", vr::getUint64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getUint32 for NULL value", nr::getUint32);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.DATE));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, uint32));
            Assert.assertEquals(LocalDate.of(11758468, 05, 10), vr.getDate());
            assertTypesMismatch("Date32", "Date", vr::getDate32);
            assertTypesMismatch("Datetime", "Date", vr::getDatetime);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getDate());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.DATETIME));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, uint32));
            Assert.assertEquals(LocalDate.of(2106, 01, 26).atTime(18, 14, 11), vr.getDatetime());
            assertTypesMismatch("Date", "Datetime", vr::getDate);
            assertTypesMismatch("Datetime64", "Datetime", vr::getDatetime64);
            Assert.assertEquals("Some[-994445]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getDatetime());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }

    @Test
    public void uint64TypesTest() {
        ValueProtos.Value int64 = ValueProtos.Value.newBuilder().setUint64Value(0xFFFFFFFD2679D0B0l).build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.UINT64));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            assertTypesMismatch("Uint8", "Uint64", vr::getUint8);
            assertTypesMismatch("Uint16", "Uint64", vr::getUint16);
            assertTypesMismatch("Uint32", "Uint64", vr::getUint32);
            Assert.assertEquals(0xFFFFFFFD2679D0B0l, vr.getUint64());
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            assertNullPointerException("cannot call getUint64 for NULL value", nr::getUint64);
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.TIMESTAMP));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, int64));
            // TODO: incorrect work with unsigned Timestamp
            Assert.assertEquals(Instant.ofEpochSecond(-12240, 615600000), vr.getTimestamp());
            assertTypesMismatch("Timestamp64", "Timestamp", vr::getTimestamp64);
            assertTypesMismatch("Uint64", "Timestamp", vr::getUint64);
            Assert.assertEquals("Some[-12239384400]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getTimestamp());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }

    @Test
    public void textTypesTest() {
        ValueProtos.Value json = ValueProtos.Value.newBuilder().setTextValue("[1, 2, 3]").build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.UTF8));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, json));
            Assert.assertEquals("[1, 2, 3]", vr.getText());
            assertTypesMismatch("Json", "Text", vr::getJson);
            assertTypesMismatch("JsonDocument", "Text", vr::getJsonDocument);
            Assert.assertEquals("Some[\"[1, 2, 3]\"]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getText());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.JSON));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, json));
            Assert.assertEquals("[1, 2, 3]", vr.getJson());
            assertTypesMismatch("Text", "Json", vr::getText);
            assertTypesMismatch("JsonDocument", "Json", vr::getJsonDocument);
            Assert.assertEquals("Some[\"[1, 2, 3]\"]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getJson());
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.JSON_DOCUMENT));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, json));
            Assert.assertEquals("[1, 2, 3]", vr.getJsonDocument());
            assertTypesMismatch("Text", "JsonDocument", vr::getText);
            assertTypesMismatch("Json", "JsonDocument", vr::getJson);
            Assert.assertEquals("Some[\"[1, 2, 3]\"]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getJsonDocument());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }

    @Test
    public void bytesTypesTest() {
        byte[] arr = new byte[] { '[', '1', ',', '2', ',', '3', ']' };
        ValueProtos.Value bytes = ValueProtos.Value.newBuilder().setBytesValue(ByteString.copyFrom(arr)).build();

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.STRING));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, bytes));
            Assert.assertArrayEquals(arr, vr.getBytes());
            Assert.assertEquals("[1,2,3]", vr.getBytesAsString(StandardCharsets.UTF_8));
            assertTypesMismatch("Yson", "Bytes", vr::getYson);
            Assert.assertEquals("Some[\"[1,2,3]\"]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getBytes());
            Assert.assertNull(nr.getBytesAsString(StandardCharsets.UTF_8));
            Assert.assertEquals("Empty[]", nr.toString());
        }

        {
            ValueProtos.Type type = optionalType(primitiveType(ValueProtos.Type.PrimitiveTypeId.YSON));
            ValueReader vr = ProtoValueReaders.forTypedValue(typedValue(type, bytes));
            Assert.assertArrayEquals(arr, vr.getYson());
            assertTypesMismatch("Bytes", "Yson", vr::getBytes);
            assertTypesMismatch("Bytes", "Yson", () -> vr.getBytesAsString(StandardCharsets.UTF_8));
            Assert.assertEquals("Some[\"[1,2,3]\"]", vr.toString());

            ValueReader nr = ProtoValueReaders.forTypedValue(typedValue(type, nullValue()));
            Assert.assertNull(nr.getYson());
            Assert.assertEquals("Empty[]", nr.toString());
        }
    }
}
