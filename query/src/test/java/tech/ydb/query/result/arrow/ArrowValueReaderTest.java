package tech.ydb.query.result.arrow;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.FixedSizeBinaryVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarBinaryVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.util.Text;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.OptionalValue;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ArrowValueReaderTest {
    private RootAllocator allocator;

    @Before
    public void init() {
        this.allocator = new RootAllocator();
    }

    @After
    public void clean() {
        this.allocator.close();
    }
    private void assertIllegalStateException(String msg, ThrowingRunnable runnable) {
        IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    @Test
    public void unsupportedVectorTypeTest() {
        try (ListVector vector = ListVector.empty("testList", allocator)) {
            assertIllegalStateException(
                    "Unsupported ApacheArrow vector type: class org.apache.arrow.vector.complex.ListVector",
                    () -> ApacheArrowValueReader.createReader(vector, ListType.of(PrimitiveType.Int32), false)
            );
        }
    }

    @Test
    public void fieldVectorTest() {
        Field field = new Field("test", new FieldType(false, new ArrowType.Int(8, false), null, null), null);
        try (UInt1Vector vector = new UInt1Vector(field, allocator)) {
            vector.allocateNew(1);
            vector.set(0, 0);

            // not null reader
            ApacheArrowValueReader<?> reader = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, false);

            Assert.assertEquals(false, reader.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(false), (PrimitiveValue) reader.getValue());

            assertIllegalStateException("cannot call getInt8, actual type: Bool", reader::getInt8);
            assertIllegalStateException("cannot call getInt16, actual type: Bool", reader::getInt16);
            assertIllegalStateException("cannot call getInt32, actual type: Bool", reader::getInt32);
            assertIllegalStateException("cannot call getInt64, actual type: Bool", reader::getInt64);

            assertIllegalStateException("cannot call getUint8, actual type: Bool", reader::getUint8);
            assertIllegalStateException("cannot call getUint16, actual type: Bool", reader::getUint16);
            assertIllegalStateException("cannot call getUint32, actual type: Bool", reader::getUint32);
            assertIllegalStateException("cannot call getUint64, actual type: Bool", reader::getUint64);

            assertIllegalStateException("cannot call getFloat, actual type: Bool", reader::getFloat);
            assertIllegalStateException("cannot call getDouble, actual type: Bool", reader::getDouble);

            assertIllegalStateException("cannot call getText, actual type: Bool", reader::getText);
            assertIllegalStateException("cannot call getJson, actual type: Bool", reader::getJson);
            assertIllegalStateException("cannot call getJsonDocument, actual type: Bool", reader::getJsonDocument);

            assertIllegalStateException("cannot call getBytes, actual type: Bool", reader::getBytes);
            assertIllegalStateException("cannot call getYson, actual type: Bool", reader::getYson);
            assertIllegalStateException("cannot call getBytesAsString, actual type: Bool",
                    () -> reader.getBytesAsString(StandardCharsets.UTF_8));

            assertIllegalStateException("cannot call getUuid, actual type: Bool", reader::getUuid);

            assertIllegalStateException("cannot call getDate, actual type: Bool", reader::getDate);
            assertIllegalStateException("cannot call getDatetime, actual type: Bool", reader::getDatetime);
            assertIllegalStateException("cannot call getTimestamp, actual type: Bool", reader::getTimestamp);
            assertIllegalStateException("cannot call getInterval, actual type: Bool", reader::getInterval);

            assertIllegalStateException("cannot call getDate32, actual type: Bool", reader::getDate32);
            assertIllegalStateException("cannot call getDatetime64, actual type: Bool", reader::getDatetime64);
            assertIllegalStateException("cannot call getTimestamp64, actual type: Bool", reader::getTimestamp64);
            assertIllegalStateException("cannot call getInterval64, actual type: Bool", reader::getInterval64);

            assertIllegalStateException("cannot call getDecimal, actual type: Bool", reader::getDecimal);

            // not supported types
            assertIllegalStateException("cannot call getTzDate, actual type: Bool", reader::getTzDate);
            assertIllegalStateException("cannot call getTzDatetime, actual type: Bool", reader::getTzDatetime);
            assertIllegalStateException("cannot call getTzTimestamp, actual type: Bool", reader::getTzTimestamp);

            assertIllegalStateException("cannot call getListItemsCount, actual type: Bool", reader::getListItemsCount);
            assertIllegalStateException("cannot call getListItem, actual type: Bool", () -> reader.getListItem(0));

            assertIllegalStateException("cannot call getDictItemsCount, actual type: Bool", reader::getDictItemsCount);
            assertIllegalStateException("cannot call getDictKey, actual type: Bool", () -> reader.getDictKey(0));
            assertIllegalStateException("cannot call getDictValue, actual type: Bool", () -> reader.getDictValue(0));

            assertIllegalStateException("cannot call getTupleElementsCount, actual type: Bool",
                    reader::getTupleElementsCount);
            assertIllegalStateException("cannot call getTupleElement, actual type: Bool",
                    () -> reader.getTupleElement(0));

            assertIllegalStateException("cannot call getVariantTypeIndex, actual type: Bool",
                    reader::getVariantTypeIndex);
            assertIllegalStateException("cannot call getVariantItem, actual type: Bool", reader::getVariantItem);

            assertIllegalStateException("cannot call getStructMembersCount, actual type: Bool",
                    reader::getStructMembersCount);
            assertIllegalStateException("cannot call getStructMember, actual type: Bool",
                    () -> reader.getStructMember(0));
            assertIllegalStateException("cannot call getStructMemberName, actual type: Bool",
                    () -> reader.getStructMemberName(0));
            assertIllegalStateException("cannot call getStructMember, actual type: Bool",
                    () -> reader.getStructMember(""));
        }

        try (UInt2Vector vector = new UInt2Vector(Field.nullable("test", new ArrowType.Int(16, false)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 123);

            ApacheArrowValueReader<?> reader = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uint16, true);
            Assert.assertEquals(true, reader.isOptionalItemPresent());
            Assert.assertEquals(123, reader.getUint16());
            Assert.assertEquals(PrimitiveValue.newUint16(123).makeOptional(), (OptionalValue) reader.getValue());

            assertIllegalStateException("cannot call getBool, actual type: Uint16?", reader::getBool);
            assertIllegalStateException("cannot call getUint8, actual type: Uint16?", reader::getUint8);
        }
    }

    @Test
    public void nullabilityTest() {
        try (UInt1Vector vector = new UInt1Vector(Field.nullable("nullable", new ArrowType.Int(8, false)), allocator)) {
            vector.allocateNew(3);
            vector.set(0, 0);
            vector.setNull(1);
            vector.set(2, 1);

            // not null reader
            ApacheArrowValueReader<?> reader = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, false);
            reader.setRowIndex(0);
            assertIllegalStateException("cannot call isOptionalItemPresent, actual type: Bool",
                    reader::isOptionalItemPresent);
            assertIllegalStateException("cannot call getOptionalItem, actual type: Bool", reader::getOptionalItem);
            Assert.assertEquals(false, reader.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(false), (PrimitiveValue) reader.getValue());
            Assert.assertEquals("false", reader.toString());

            reader.setRowIndex(1);
            assertIllegalStateException("cannot call isOptionalItemPresent, actual type: Bool",
                    reader::isOptionalItemPresent);
            assertIllegalStateException("cannot call getOptionalItem, actual type: Bool", reader::getOptionalItem);
            assertIllegalStateException("Value at index is null", reader::getBool);
            assertIllegalStateException("Value at index is null", reader::getValue);
            assertIllegalStateException("Value at index is null", reader::toString);

            reader.setRowIndex(2);
            assertIllegalStateException("cannot call isOptionalItemPresent, actual type: Bool",
                    reader::isOptionalItemPresent);
            assertIllegalStateException("cannot call getOptionalItem, actual type: Bool", reader::getOptionalItem);
            Assert.assertEquals(true, reader.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(true), (PrimitiveValue) reader.getValue());
            Assert.assertEquals("true", reader.toString());

            // nullable reader
            ApacheArrowValueReader<?> reader2 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            reader2.setRowIndex(0);
            Assert.assertEquals(true, reader2.isOptionalItemPresent());
            Assert.assertEquals(false, reader2.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(false).makeOptional(), (OptionalValue) reader2.getValue());
            Assert.assertNotNull(reader2.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newBool(false), (PrimitiveValue) reader2.getOptionalItem().getValue());
            Assert.assertEquals("Some[false]", reader2.toString());

            reader2.setRowIndex(1);
            Assert.assertEquals(false, reader2.isOptionalItemPresent());
            assertIllegalStateException("Value at index is null", reader2::getBool);
            Assert.assertEquals(PrimitiveType.Bool.makeOptional().emptyValue(), (OptionalValue) reader2.getValue());
            Assert.assertNull(reader2.getOptionalItem());
            Assert.assertEquals("Empty[]", reader2.toString());

            reader2.setRowIndex(2);
            Assert.assertEquals(true, reader2.isOptionalItemPresent());
            Assert.assertEquals(true, reader2.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(true).makeOptional(), (OptionalValue) reader2.getValue());
            Assert.assertNotNull(reader2.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newBool(true), (PrimitiveValue) reader2.getOptionalItem().getValue());
            Assert.assertEquals("Some[true]", reader2.toString());
        }
    }

    @Test
    public void uint1VectorTest() {
        Field field = new Field("test", new FieldType(false, new ArrowType.Int(8, false), null, null), null);
        try (UInt1Vector vector = new UInt1Vector(field, allocator)) {
            vector.allocateNew(1);
            vector.set(0, 123);

            ApacheArrowValueReader<?> bool = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, false);
            assertIllegalStateException("cannot call getUint8, actual type: Bool", bool::getUint8);
            Assert.assertEquals(true, bool.getBool());
            Assert.assertEquals(PrimitiveValue.newBool(true), (PrimitiveValue) bool.getValue());
            Assert.assertEquals("true", bool.toString());

            ApacheArrowValueReader<?> uint8 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uint8, false);
            assertIllegalStateException("cannot call getBool, actual type: Uint8", uint8::getBool);
            Assert.assertEquals(123, uint8.getUint8());
            Assert.assertEquals(PrimitiveValue.newUint8(123), (PrimitiveValue) uint8.getValue());
            Assert.assertEquals("123", uint8.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int8, false);
            assertIllegalStateException("cannot call getBool, actual type: Int8", wrong::getBool);
            assertIllegalStateException("cannot call getUint8, actual type: Int8", wrong::getUint8);
            assertIllegalStateException("cannot call getValue, actual type: Int8", wrong::getValue);
            Assert.assertEquals("Unreadable UInt1Vector[Int8]", wrong.toString());
        }
    }

    @Test
    public void uint2VectorTest() {
        try (UInt2Vector vector = new UInt2Vector(Field.nullable("test", new ArrowType.Int(16, false)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 123);

            ApacheArrowValueReader<?> uint16 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uint16, true);
            Assert.assertEquals(123, uint16.getUint16());
            assertIllegalStateException("cannot call getDate, actual type: Uint16?", uint16::getDate);
            Assert.assertEquals(PrimitiveValue.newUint16(123).makeOptional(), (OptionalValue) uint16.getValue());
            Assert.assertEquals("Some[123]", uint16.toString());
            Assert.assertNotNull(uint16.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newUint16(123), (PrimitiveValue) uint16.getOptionalItem().getValue());

            ApacheArrowValueReader<?> date = ApacheArrowValueReader.createReader(vector, PrimitiveType.Date, true);
            assertIllegalStateException("cannot call getUint16, actual type: Date?", date::getUint16);
            Assert.assertEquals(LocalDate.ofEpochDay(123), date.getDate());
            Assert.assertEquals(PrimitiveValue.newDate(123).makeOptional(), (OptionalValue) date.getValue());
            Assert.assertEquals("Some[1970-05-04]", date.toString());
            Assert.assertNotNull(date.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newDate(123), (PrimitiveValue) date.getOptionalItem().getValue());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int8, true);
            assertIllegalStateException("cannot call getDate, actual type: Int8?", wrong::getDate);
            assertIllegalStateException("cannot call getUint16, actual type: Int8?", wrong::getUint16);
            assertIllegalStateException("cannot call getValue, actual type: Int8?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable UInt2Vector[Int8]]", wrong.toString());
        }
    }

    @Test
    public void uint4VectorTest() {
        try (UInt4Vector vector = new UInt4Vector(Field.nullable("test", new ArrowType.Int(32, false)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 1234567);

            ApacheArrowValueReader<?> uint32 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uint32, true);
            Assert.assertEquals(1234567, uint32.getUint32());
            assertIllegalStateException("cannot call getDatetime, actual type: Uint32?", uint32::getDatetime);
            Assert.assertEquals(PrimitiveValue.newUint32(1234567).makeOptional(), (OptionalValue) uint32.getValue());
            Assert.assertEquals("Some[1234567]", uint32.toString());
            Assert.assertNotNull(uint32.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newUint32(1234567), (PrimitiveValue) uint32.getOptionalItem().getValue());

            ApacheArrowValueReader<?> datetime = ApacheArrowValueReader.createReader(vector, PrimitiveType.Datetime, true);
            assertIllegalStateException("cannot call getUint32, actual type: Datetime?", datetime::getUint32);
            Assert.assertEquals(LocalDateTime.ofEpochSecond(1234567, 0, ZoneOffset.UTC), datetime.getDatetime());
            Assert.assertEquals(PrimitiveValue.newDatetime(1234567).makeOptional(), (OptionalValue) datetime.getValue());
            Assert.assertEquals("Some[1970-01-15T06:56:07]", datetime.toString());
            Assert.assertNotNull(datetime.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newDatetime(1234567), (PrimitiveValue) datetime.getOptionalItem().getValue());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getUint32, actual type: Bool?", wrong::getUint32);
            assertIllegalStateException("cannot call getDatetime, actual type: Bool?", wrong::getDatetime);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable UInt4Vector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void uint8VectorTest() {
        try (UInt8Vector vector = new UInt8Vector(Field.nullable("test", new ArrowType.Int(64, false)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 0x1234567812345L);

            ApacheArrowValueReader<?> uint64 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uint64, true);
            Assert.assertEquals(0x1234567812345L, uint64.getUint64());
            assertIllegalStateException("cannot call getTimestamp, actual type: Uint64?", uint64::getTimestamp);
            Assert.assertEquals(PrimitiveValue.newUint64(0x1234567812345L).makeOptional(),
                    (OptionalValue) uint64.getValue());
            Assert.assertEquals("Some[320255972942661]", uint64.toString());
            Assert.assertNotNull(uint64.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newUint64(0x1234567812345L),
                    (PrimitiveValue) uint64.getOptionalItem().getValue());

            ApacheArrowValueReader<?> timestamp = ApacheArrowValueReader.createReader(vector, PrimitiveType.Timestamp, true);
            assertIllegalStateException("cannot call getUint64, actual type: Timestamp?", timestamp::getUint64);
            Assert.assertEquals(Instant.ofEpochSecond(320255972L, 942661000), timestamp.getTimestamp());
            Assert.assertEquals(PrimitiveValue.newTimestamp(0x1234567812345L).makeOptional(),
                    (OptionalValue) timestamp.getValue());
            Assert.assertEquals("Some[1980-02-24T15:59:32.942661Z]", timestamp.toString());
            Assert.assertNotNull(timestamp.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newTimestamp(0x1234567812345L),
                    (PrimitiveValue) timestamp.getOptionalItem().getValue());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getUint64, actual type: Bool?", wrong::getUint64);
            assertIllegalStateException("cannot call getTimestamp, actual type: Bool?", wrong::getTimestamp);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable UInt8Vector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void tinyIntVectorTest() {
        try (TinyIntVector vector = new TinyIntVector(Field.nullable("test", new ArrowType.Int(8, true)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 123);

            ApacheArrowValueReader<?> int8 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int8, true);
            assertIllegalStateException("cannot call getBool, actual type: Int8?", int8::getBool);
            Assert.assertEquals(123, int8.getInt8());
            Assert.assertEquals(PrimitiveValue.newInt8((byte) 123).makeOptional(), (OptionalValue) int8.getValue());
            Assert.assertNotNull(int8.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInt8((byte) 123), (PrimitiveValue) int8.getOptionalItem().getValue());
            Assert.assertEquals("Some[123]", int8.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getInt8, actual type: Bool?", wrong::getInt8);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable TinyIntVector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void smallIntVectorTest() {
        try (SmallIntVector vector = new SmallIntVector(Field.nullable("test", new ArrowType.Int(16, true)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 12345);

            ApacheArrowValueReader<?> int16 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int16, true);
            assertIllegalStateException("cannot call getBool, actual type: Int16?", int16::getBool);
            Assert.assertEquals(12345, int16.getInt16());
            Assert.assertEquals(PrimitiveValue.newInt16((short) 12345).makeOptional(), (OptionalValue) int16.getValue());
            Assert.assertNotNull(int16.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInt16((short) 12345), (PrimitiveValue) int16.getOptionalItem().getValue());
            Assert.assertEquals("Some[12345]", int16.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getInt16, actual type: Bool?", wrong::getInt16);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable SmallIntVector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void intVectorTest() {
        try (IntVector vector = new IntVector(Field.nullable("test", new ArrowType.Int(32, true)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 12345);

            ApacheArrowValueReader<?> int32 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int32, true);
            Assert.assertEquals(12345, int32.getInt32());
            assertIllegalStateException("cannot call getDate32, actual type: Int32?", int32::getDate32);
            Assert.assertEquals(PrimitiveValue.newInt32(12345).makeOptional(), (OptionalValue) int32.getValue());
            Assert.assertEquals("Some[12345]", int32.toString());
            Assert.assertNotNull(int32.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInt32(12345), (PrimitiveValue) int32.getOptionalItem().getValue());

            ApacheArrowValueReader<?> date32 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Date32, true);
            assertIllegalStateException("cannot call getInt32, actual type: Date32?", date32::getInt32);
            Assert.assertEquals(LocalDate.ofEpochDay(12345), date32.getDate32());
            Assert.assertEquals(PrimitiveValue.newDate32(12345).makeOptional(), (OptionalValue) date32.getValue());
            Assert.assertEquals("Some[2003-10-20]", date32.toString());
            Assert.assertNotNull(date32.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newDate32(12345), (PrimitiveValue) date32.getOptionalItem().getValue());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int8, true);
            assertIllegalStateException("cannot call getDate32, actual type: Int8?", wrong::getDate32);
            assertIllegalStateException("cannot call getInt32, actual type: Int8?", wrong::getInt32);
            assertIllegalStateException("cannot call getValue, actual type: Int8?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable IntVector[Int8]]", wrong.toString());
        }
    }

    @Test
    public void bigIntVectorTest() {
        try (BigIntVector vector = new BigIntVector(Field.nullable("test", new ArrowType.Int(64, true)), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 0x1234567812345L);

            ApacheArrowValueReader<?> int64 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Int64, true);
            Assert.assertEquals(0x1234567812345L, int64.getInt64());
            assertIllegalStateException("cannot call getDatetime64, actual type: Int64?", int64::getDatetime64);
            assertIllegalStateException("cannot call getTimestamp64, actual type: Int64?", int64::getTimestamp64);
            assertIllegalStateException("cannot call getInterval, actual type: Int64?", int64::getInterval);
            assertIllegalStateException("cannot call getInterval64, actual type: Int64?", int64::getInterval64);
            Assert.assertEquals(PrimitiveValue.newInt64(0x1234567812345L).makeOptional(),
                    (OptionalValue) int64.getValue());
            Assert.assertEquals("Some[320255972942661]", int64.toString());
            Assert.assertNotNull(int64.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInt64(0x1234567812345L),
                    (PrimitiveValue) int64.getOptionalItem().getValue());

            ApacheArrowValueReader<?> dt64 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Datetime64, true);
            assertIllegalStateException("cannot call getInt64, actual type: Datetime64?", dt64::getInt64);
            Assert.assertEquals(LocalDateTime.ofEpochSecond(0x1234567812345L, 0, ZoneOffset.UTC), dt64.getDatetime64());
            assertIllegalStateException("cannot call getTimestamp64, actual type: Datetime64?", dt64::getTimestamp64);
            assertIllegalStateException("cannot call getInterval, actual type: Datetime64?", dt64::getInterval);
            assertIllegalStateException("cannot call getInterval64, actual type: Datetime64?", dt64::getInterval64);
            Assert.assertEquals(PrimitiveValue.newDatetime64(0x1234567812345L).makeOptional(),
                    (OptionalValue) dt64.getValue());
            Assert.assertEquals("Some[+10150477-10-12T12:04:21]", dt64.toString());
            Assert.assertNotNull(dt64.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newDatetime64(0x1234567812345L),
                    (PrimitiveValue) dt64.getOptionalItem().getValue());

            ApacheArrowValueReader<?> tm64 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Timestamp64, true);
            assertIllegalStateException("cannot call getInt64, actual type: Timestamp64?", tm64::getInt64);
            assertIllegalStateException("cannot call getDatetime64, actual type: Timestamp64?", tm64::getDatetime64);
            Assert.assertEquals(Instant.ofEpochSecond(320255972L, 942661000), tm64.getTimestamp64());
            assertIllegalStateException("cannot call getInterval, actual type: Timestamp64?", tm64::getInterval);
            assertIllegalStateException("cannot call getInterval64, actual type: Timestamp64?", tm64::getInterval64);
            Assert.assertEquals(PrimitiveValue.newTimestamp64(0x1234567812345L).makeOptional(),
                    (OptionalValue) tm64.getValue());
            Assert.assertEquals("Some[1980-02-24T15:59:32.942661Z]", tm64.toString());
            Assert.assertNotNull(tm64.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newTimestamp64(0x1234567812345L),
                    (PrimitiveValue) tm64.getOptionalItem().getValue());

            ApacheArrowValueReader<?> inter = ApacheArrowValueReader.createReader(vector, PrimitiveType.Interval, true);
            assertIllegalStateException("cannot call getInt64, actual type: Interval?", inter::getInt64);
            assertIllegalStateException("cannot call getDatetime64, actual type: Interval?", inter::getDatetime64);
            assertIllegalStateException("cannot call getTimestamp64, actual type: Interval?", inter::getTimestamp64);
            Assert.assertEquals(Duration.ofSeconds(320255972, 942661000), inter.getInterval());
            assertIllegalStateException("cannot call getInterval64, actual type: Interval?", inter::getInterval64);
            Assert.assertEquals(PrimitiveValue.newInterval(0x1234567812345L).makeOptional(),
                    (OptionalValue) inter.getValue());
            Assert.assertEquals("Some[PT88959H59M32.942661S]", inter.toString());
            Assert.assertNotNull(inter.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInterval(0x1234567812345L),
                    (PrimitiveValue) inter.getOptionalItem().getValue());

            ApacheArrowValueReader<?> inter64 = ApacheArrowValueReader.createReader(vector, PrimitiveType.Interval64, true);
            assertIllegalStateException("cannot call getInt64, actual type: Interval64?", inter64::getInt64);
            assertIllegalStateException("cannot call getDatetime64, actual type: Interval64?", inter64::getDatetime64);
            assertIllegalStateException("cannot call getTimestamp64, actual type: Interval64?", inter64::getTimestamp64);
            assertIllegalStateException("cannot call getInterval, actual type: Interval64?", inter64::getInterval);
            Assert.assertEquals(Duration.ofSeconds(320255972, 942661000), inter64.getInterval64());
            Assert.assertEquals(PrimitiveValue.newInterval64(0x1234567812345L).makeOptional(),
                    (OptionalValue) inter64.getValue());
            Assert.assertEquals("Some[PT88959H59M32.942661S]", inter64.toString());
            Assert.assertNotNull(inter64.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newInterval64(0x1234567812345L),
                    (PrimitiveValue) inter64.getOptionalItem().getValue());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getInt64, actual type: Bool?", wrong::getInt64);
            assertIllegalStateException("cannot call getDatetime64, actual type: Bool?", wrong::getDatetime64);
            assertIllegalStateException("cannot call getTimestamp64, actual type: Bool?", wrong::getTimestamp64);
            assertIllegalStateException("cannot call getInterval, actual type: Bool?", wrong::getInterval);
            assertIllegalStateException("cannot call getInterval64, actual type: Bool?", wrong::getInterval64);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable BigIntVector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void float4VectorTest() {
        ArrowType.FloatingPoint type = new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE);
        try (Float4Vector vector = new Float4Vector(Field.nullable("test", type), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 0.1234f);

            ApacheArrowValueReader<?> flt = ApacheArrowValueReader.createReader(vector, PrimitiveType.Float, true);
            assertIllegalStateException("cannot call getDouble, actual type: Float?", flt::getDouble);
            Assert.assertEquals(0.1234f, flt.getFloat(), 1e-6f);
            Assert.assertEquals(PrimitiveValue.newFloat(0.1234f).makeOptional(), (OptionalValue) flt.getValue());
            Assert.assertNotNull(flt.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newFloat(0.1234f), (PrimitiveValue) flt.getOptionalItem().getValue());
            Assert.assertEquals("Some[0.1234]", flt.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Double, true);
            assertIllegalStateException("cannot call getFloat, actual type: Double?", wrong::getFloat);
            assertIllegalStateException("cannot call getValue, actual type: Double?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable Float4Vector[Double]]", wrong.toString());
        }
    }

    @Test
    public void float8VectorTest() {
        ArrowType.FloatingPoint type = new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
        try (Float8Vector vector = new Float8Vector(Field.nullable("test", type), allocator)) {
            vector.allocateNew(1);
            vector.set(0, 123456.1234d);

            ApacheArrowValueReader<?> dbl = ApacheArrowValueReader.createReader(vector, PrimitiveType.Double, true);
            assertIllegalStateException("cannot call getFloat, actual type: Double?", dbl::getFloat);
            Assert.assertEquals(123456.1234d, dbl.getDouble(), 1e-6f);
            Assert.assertEquals(PrimitiveValue.newDouble(123456.1234d).makeOptional(), (OptionalValue) dbl.getValue());
            Assert.assertNotNull(dbl.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newDouble(123456.1234d), (PrimitiveValue) dbl.getOptionalItem().getValue());
            Assert.assertEquals("Some[123456.1234]", dbl.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Float, true);
            assertIllegalStateException("cannot call getDouble, actual type: Float?", wrong::getDouble);
            assertIllegalStateException("cannot call getValue, actual type: Float?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable Float8Vector[Float]]", wrong.toString());
        }
    }

    @Test
    public void varCharVectorTest() {
        try (VarCharVector vector = new VarCharVector(Field.nullable("test", new ArrowType.Utf8()), allocator)) {
            vector.allocateNew(1);
            vector.setSafe(0, new Text("[1,2,3]"));

            ApacheArrowValueReader<?> text = ApacheArrowValueReader.createReader(vector, PrimitiveType.Text, true);
            Assert.assertEquals("[1,2,3]", text.getText());
            assertIllegalStateException("cannot call getJson, actual type: Text?", text::getJson);
            assertIllegalStateException("cannot call getJsonDocument, actual type: Text?", text::getJsonDocument);
            Assert.assertEquals(PrimitiveValue.newText("[1,2,3]").makeOptional(), (OptionalValue) text.getValue());
            Assert.assertNotNull(text.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newText("[1,2,3]"), (PrimitiveValue) text.getOptionalItem().getValue());
            Assert.assertEquals("Some[[1,2,3]]", text.toString());

            ApacheArrowValueReader<?> json = ApacheArrowValueReader.createReader(vector, PrimitiveType.Json, true);
            assertIllegalStateException("cannot call getText, actual type: Json?", json::getText);
            Assert.assertEquals("[1,2,3]", json.getJson());
            assertIllegalStateException("cannot call getJsonDocument, actual type: Json?", json::getJsonDocument);
            Assert.assertEquals(PrimitiveValue.newJson("[1,2,3]").makeOptional(), (OptionalValue) json.getValue());
            Assert.assertNotNull(json.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newJson("[1,2,3]"), (PrimitiveValue) json.getOptionalItem().getValue());
            Assert.assertEquals("Some[[1,2,3]]", json.toString());

            ApacheArrowValueReader<?> jsonDoc = ApacheArrowValueReader.createReader(vector, PrimitiveType.JsonDocument, true);
            assertIllegalStateException("cannot call getText, actual type: JsonDocument?", jsonDoc::getText);
            assertIllegalStateException("cannot call getJson, actual type: JsonDocument?", jsonDoc::getJson);
            Assert.assertEquals("[1,2,3]", jsonDoc.getJsonDocument());
            Assert.assertEquals(PrimitiveValue.newJsonDocument("[1,2,3]").makeOptional(), (OptionalValue) jsonDoc.getValue());
            Assert.assertNotNull(jsonDoc.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newJsonDocument("[1,2,3]"), (PrimitiveValue) jsonDoc.getOptionalItem().getValue());
            Assert.assertEquals("Some[[1,2,3]]", jsonDoc.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getText, actual type: Bool?", wrong::getText);
            assertIllegalStateException("cannot call getJson, actual type: Bool?", wrong::getJson);
            assertIllegalStateException("cannot call getJsonDocument, actual type: Bool?", wrong::getJsonDocument);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable VarCharVector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void varBinaryTest() {
        byte[] value = new byte[] { 0x40, 0x41, 0x42 };
        try (VarBinaryVector vector = new VarBinaryVector(Field.nullable("test", new ArrowType.Binary()), allocator)) {
            vector.allocateNew(1);
            vector.setSafe(0, value);

            ApacheArrowValueReader<?> bytes = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bytes, true);
            Assert.assertArrayEquals(value, bytes.getBytes());
            Assert.assertEquals("@AB", bytes.getBytesAsString(StandardCharsets.UTF_8));
            assertIllegalStateException("cannot call getYson, actual type: Bytes?", bytes::getYson);
            Assert.assertEquals(PrimitiveValue.newBytes(value).makeOptional(), (OptionalValue) bytes.getValue());
            Assert.assertNotNull(bytes.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newBytes(value), (PrimitiveValue) bytes.getOptionalItem().getValue());
            Assert.assertEquals("Some[404142]", bytes.toString());

            ApacheArrowValueReader<?> yson = ApacheArrowValueReader.createReader(vector, PrimitiveType.Yson, true);
            assertIllegalStateException("cannot call getBytes, actual type: Yson?", yson::getBytes);
            assertIllegalStateException("cannot call getBytesAsString, actual type: Yson?",
                    () -> yson.getBytesAsString(StandardCharsets.UTF_8));
            Assert.assertArrayEquals(value, yson.getYson());
            Assert.assertEquals(PrimitiveValue.newYson(value).makeOptional(), (OptionalValue) yson.getValue());
            Assert.assertNotNull(yson.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newYson(value), (PrimitiveValue) yson.getOptionalItem().getValue());
            Assert.assertEquals("Some[404142]", yson.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getBytes, actual type: Bool?", wrong::getBytes);
            assertIllegalStateException("cannot call getBytesAsString, actual type: Bool?",
                    () -> wrong.getBytesAsString(StandardCharsets.UTF_8));
            assertIllegalStateException("cannot call getYson, actual type: Bool?", wrong::getYson);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable VarBinaryVector[Bool]]", wrong.toString());
        }
    }

    @Test
    public void fixedBinaryTest() {
        byte[] value = new byte[] {
            (byte) 0x87, 0x65, 0x43, 0x21, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        UUID uuidValue = UUID.fromString("21436587-0000-0000-0000-000000000000");
        ArrowType.FixedSizeBinary type = new ArrowType.FixedSizeBinary(16);
        try (FixedSizeBinaryVector vector = new FixedSizeBinaryVector(Field.nullable("test", type), allocator)) {
            vector.allocateNew(1);
            vector.setSafe(0, value);

            ApacheArrowValueReader<?> uuid = ApacheArrowValueReader.createReader(vector, PrimitiveType.Uuid, true);
            Assert.assertEquals(uuidValue, uuid.getUuid());
            assertIllegalStateException("cannot call getDecimal, actual type: Uuid?", uuid::getDecimal);
            Assert.assertEquals(PrimitiveValue.newUuid(uuidValue).makeOptional(), (OptionalValue) uuid.getValue());
            Assert.assertNotNull(uuid.getOptionalItem());
            Assert.assertEquals(PrimitiveValue.newUuid(uuidValue), (PrimitiveValue) uuid.getOptionalItem().getValue());
            Assert.assertEquals("Some[21436587-0000-0000-0000-000000000000]", uuid.toString());

            ApacheArrowValueReader<?> decimal = ApacheArrowValueReader.createReader(vector, DecimalType.getDefault(), true);
            assertIllegalStateException("cannot call getUuid, actual type: Decimal(22, 9)?", decimal::getUuid);
            Assert.assertEquals(DecimalType.getDefault().newValueUnscaled(0x21436587L), decimal.getDecimal());
            Assert.assertEquals(DecimalType.getDefault().newValueUnscaled(0x21436587L).makeOptional(),
                    (OptionalValue) decimal.getValue());
            Assert.assertNotNull(decimal.getOptionalItem());
            Assert.assertEquals(DecimalType.getDefault().newValueUnscaled(0x21436587L),
                    (DecimalValue) decimal.getOptionalItem().getValue());
            Assert.assertEquals("Some[0.558065031]", decimal.toString());

            ApacheArrowValueReader<?> decimal10 = ApacheArrowValueReader.createReader(vector, DecimalType.of(10), true);
            assertIllegalStateException("cannot call getUuid, actual type: Decimal(10, 0)?", decimal10::getUuid);
            Assert.assertEquals(DecimalType.of(10).newValueUnscaled(0x21436587L), decimal10.getDecimal());
            Assert.assertEquals(DecimalType.of(10).newValueUnscaled(0x21436587L).makeOptional(),
                    (OptionalValue) decimal10.getValue());
            Assert.assertNotNull(decimal10.getOptionalItem());
            Assert.assertEquals(DecimalType.of(10).newValueUnscaled(0x21436587L),
                    (DecimalValue) decimal10.getOptionalItem().getValue());
            Assert.assertEquals("Some[558065031]", decimal10.toString());

            ApacheArrowValueReader<?> wrong = ApacheArrowValueReader.createReader(vector, PrimitiveType.Bool, true);
            assertIllegalStateException("cannot call getUuid, actual type: Bool?", wrong::getUuid);
            assertIllegalStateException("cannot call getDecimal, actual type: Bool?", wrong::getDecimal);
            assertIllegalStateException("cannot call getValue, actual type: Bool?", wrong::getValue);
            Assert.assertEquals("Some[Unreadable FixedSizeBinaryVector[Bool]]", wrong.toString());
        }
    }
}
