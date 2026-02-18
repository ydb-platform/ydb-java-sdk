package tech.ydb.table.query;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.ipc.ReadChannel;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.table.values.DecimalType;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.ListType;
import tech.ydb.table.values.PrimitiveType;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowWriterTest {

    private void assertIllegalArgument(String message, ThrowingRunnable runnable) {
         IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class, runnable);
         Assert.assertEquals(message, ex.getMessage());
    }

    private void assertIllegalState(String message, ThrowingRunnable runnable) {
         IllegalStateException ex = Assert.assertThrows(IllegalStateException.class, runnable);
         Assert.assertEquals(message, ex.getMessage());
    }

    private static RootAllocator allocator;

    @BeforeClass
    public static void initAllocator() {
        allocator = new RootAllocator();
    }

    @AfterClass
    public static void cleanAllocator() {
        allocator.close();
    }

    @Test
    public void unsupportedTypesTest() {
        // tz date is not supported
        assertIllegalArgument("Type TzDate is not supported in ArrowWriter", () -> ApacheArrowWriter.newSchema()
                .addColumn("col", PrimitiveType.TzDate)
                .createWriter(allocator));

        // complex types are not supported (except Optional<T>)
        assertIllegalArgument("Type Int32? is not supported in ArrowWriter", () -> ApacheArrowWriter.newSchema()
                .addNullableColumn("col", PrimitiveType.Int32.makeOptional())
                .createWriter(allocator));

        // Non primitive types are not supported (except Decimal)
        assertIllegalArgument("Type List<Int32> is not supported in ArrowWriter", () -> ApacheArrowWriter.newSchema()
                .addColumn("col", ListType.of(PrimitiveType.Int32))
                .createWriter(allocator));
    }

    @Test
    public void invalidColumnTest() {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("col", PrimitiveType.Uuid)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();
            assertIllegalArgument("Column 'col2' not found", () -> row.writeUuid("col2", UUID.randomUUID()));
        }
    }

    @Test
    public void nullableTypeTest() {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("col1", PrimitiveType.Uuid)
                .addColumn("col2", PrimitiveType.Uuid.makeOptional())
                .addNullableColumn("col3", PrimitiveType.Uuid)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();
            assertIllegalState("cannot call writeNull, actual type: Uuid", () -> row.writeNull("col1"));
            row.writeNull("col2"); // success
            row.writeNull("col3"); // success
        }
    }

    @Test
    public void baseTypeValidationTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("col1", PrimitiveType.Uuid)
                .addColumn("col2", PrimitiveType.Int32)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeBool, actual type: Uuid", () -> row.writeBool("col1", true));

            assertIllegalState("cannot call writeInt8, actual type: Uuid", () -> row.writeInt8("col1", (byte) 0));
            assertIllegalState("cannot call writeInt16, actual type: Uuid", () -> row.writeInt16("col1", (short) 0));
            assertIllegalState("cannot call writeInt32, actual type: Uuid", () -> row.writeInt32("col1", 0));
            assertIllegalState("cannot call writeInt64, actual type: Uuid", () -> row.writeInt64("col1", 0));

            assertIllegalState("cannot call writeUint8, actual type: Uuid", () -> row.writeUint8("col1", 0));
            assertIllegalState("cannot call writeUint16, actual type: Uuid", () -> row.writeUint16("col1", 0));
            assertIllegalState("cannot call writeUint32, actual type: Uuid", () -> row.writeUint32("col1", 0));
            assertIllegalState("cannot call writeUint64, actual type: Uuid", () -> row.writeUint64("col1", 0));

            assertIllegalState("cannot call writeFloat, actual type: Uuid", () -> row.writeFloat("col1", 0));
            assertIllegalState("cannot call writeDouble, actual type: Uuid", () -> row.writeDouble("col1", 0));

            assertIllegalState("cannot call writeText, actual type: Uuid", () -> row.writeText("col1", ""));
            assertIllegalState("cannot call writeJson, actual type: Uuid", () -> row.writeJson("col1", ""));
            assertIllegalState("cannot call writeJsonDocument, actual type: Uuid",
                    () -> row.writeJsonDocument("col1", ""));

            assertIllegalState("cannot call writeBytes, actual type: Uuid",
                    () -> row.writeBytes("col1", new byte[0]));
            assertIllegalState("cannot call writeYson, actual type: Uuid",
                    () -> row.writeYson("col1", new byte[0]));

            assertIllegalState("cannot call writeDate, actual type: Uuid",
                    () -> row.writeDate("col1", LocalDate.ofEpochDay(0)));
            assertIllegalState("cannot call writeDatetime, actual type: Uuid",
                    () -> row.writeDatetime("col1", LocalDateTime.now()));
            assertIllegalState("cannot call writeTimestamp, actual type: Uuid",
                    () -> row.writeTimestamp("col1", Instant.ofEpochSecond(0)));
            assertIllegalState("cannot call writeInterval, actual type: Uuid",
                    () -> row.writeInterval("col1", Duration.ZERO));

            assertIllegalState("cannot call writeDate32, actual type: Uuid",
                    () -> row.writeDate32("col1", LocalDate.ofEpochDay(0)));
            assertIllegalState("cannot call writeDatetime64, actual type: Uuid",
                    () -> row.writeDatetime64("col1", LocalDateTime.now()));
            assertIllegalState("cannot call writeTimestamp64, actual type: Uuid",
                    () -> row.writeTimestamp64("col1", Instant.ofEpochSecond(0)));
            assertIllegalState("cannot call writeInterval64, actual type: Uuid",
                    () -> row.writeInterval64("col1", Duration.ZERO));

            // second column
            assertIllegalState("cannot call writeDecimal, actual type: Int32",
                    () -> row.writeDecimal("col2", DecimalType.getDefault().newValue(0)));
            assertIllegalState("cannot call writeUuid, actual type: Int32",
                    () -> row.writeUuid("col2", UUID.randomUUID()));
        }
    }

    @Test
    public void tinyIntVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Bool)
                .addNullableColumn("c2", PrimitiveType.Int8)
                .addNullableColumn("c3", PrimitiveType.Uint8)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeInt8, actual type: Bool", () -> row.writeInt8("c1", (byte) 0));
            assertIllegalState("cannot call writeUint8, actual type: Int8", () -> row.writeUint8("c2", 0));
            assertIllegalState("cannot call writeBool, actual type: Uint8", () -> row.writeBool("c3", false));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Int(8, false) not null, c2: Int(8, true), c3: Int(8, false)>",
                    schema.toString());
        }
    }

    @Test
    public void smallIntVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Date)
                .addNullableColumn("c2", PrimitiveType.Int16)
                .addNullableColumn("c3", PrimitiveType.Uint16)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeUint16, actual type: Date", () -> row.writeUint16("c1", 0));
            assertIllegalState("cannot call writeDate, actual type: Int16",
                    () -> row.writeDate("c2", LocalDate.ofEpochDay(0)));
            assertIllegalState("cannot call writeInt16, actual type: Uint16", () -> row.writeInt16("c3", (short) 0));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Int(16, false) not null, c2: Int(16, true), c3: Int(16, false)>",
                    schema.toString());
        }
    }

    @Test
    public void intVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Int32)
                .addColumn("c2", PrimitiveType.Uint32)
                .addNullableColumn("c3", PrimitiveType.Date32)
                .addNullableColumn("c4", PrimitiveType.Datetime)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeUint32, actual type: Int32", () -> row.writeUint32("c1", 0));
            assertIllegalState("cannot call writeDate32, actual type: Uint32",
                    () -> row.writeDate32("c2", LocalDate.ofEpochDay(0)));
            assertIllegalState("cannot call writeDatetime, actual type: Date32",
                    () -> row.writeDatetime("c3", LocalDateTime.now()));
            assertIllegalState("cannot call writeInt32, actual type: Datetime", () -> row.writeInt32("c4", 0));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Int(32, true) not null, c2: Int(32, false) not null, c3: Int(32, true), "
                    + "c4: Int(32, false)>", schema.toString());
        }
    }

    @Test
    public void bigIntVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Int64)
                .addColumn("c2", PrimitiveType.Uint64)
                .addNullableColumn("c3", PrimitiveType.Datetime64)
                .addNullableColumn("c4", PrimitiveType.Timestamp)
                .addNullableColumn("c5", PrimitiveType.Timestamp64)
                .addNullableColumn("c6", PrimitiveType.Interval)
                .addNullableColumn("c7", PrimitiveType.Interval64)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeUint64, actual type: Int64", () -> row.writeUint64("c1", 0));
            assertIllegalState("cannot call writeDatetime64, actual type: Uint64",
                    () -> row.writeDatetime64("c2", LocalDateTime.now()));
            assertIllegalState("cannot call writeTimestamp, actual type: Datetime64",
                    () -> row.writeTimestamp("c3", Instant.now()));
            assertIllegalState("cannot call writeTimestamp64, actual type: Timestamp",
                    () -> row.writeTimestamp64("c4", Instant.now()));
            assertIllegalState("cannot call writeInterval, actual type: Timestamp64",
                    () -> row.writeInterval("c5", Duration.ZERO));
            assertIllegalState("cannot call writeInterval64, actual type: Interval",
                    () -> row.writeInterval64("c6", Duration.ZERO));
            assertIllegalState("cannot call writeInt64, actual type: Interval64",
                    () -> row.writeInt64("c7", 0));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Int(64, true) not null, c2: Int(64, false) not null, c3: Int(64, true), "
                    + "c4: Int(64, true), c5: Int(64, true), c6: Duration(MILLISECOND), c7: Int(64, true)>",
                    schema.toString());
        }
    }

    @Test
    public void varCharVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Text)
                .addNullableColumn("c2", PrimitiveType.Json)
                .addColumn("c3", PrimitiveType.JsonDocument)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeJson, actual type: Text", () -> row.writeJson("c1", ""));
            assertIllegalState("cannot call writeJsonDocument, actual type: Json", () -> row.writeJsonDocument("c2", ""));
            assertIllegalState("cannot call writeText, actual type: JsonDocument", () -> row.writeText("c3", ""));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Utf8 not null, c2: Utf8, c3: Utf8 not null>", schema.toString());
        }
    }

    @Test
    public void varBinaryVectorTest() throws IOException {
        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Bytes)
                .addNullableColumn("c2", PrimitiveType.Yson)
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeYson, actual type: Bytes", () -> row.writeYson("c1", new byte[0]));
            assertIllegalState("cannot call writeBytes, actual type: Yson", () -> row.writeBytes("c2", new byte[0]));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: Binary not null, c2: Binary>", schema.toString());
        }
    }

    @Test
    public void fixedSizeBinaryVectorTest() throws IOException {
        DecimalValue dv = DecimalType.getDefault().newValue(0);
        UUID uv = UUID.randomUUID();

        try (ApacheArrowWriter writer = ApacheArrowWriter.newSchema()
                .addColumn("c1", PrimitiveType.Uuid)
                .addNullableColumn("c2", DecimalType.getDefault())
                .createWriter(allocator)) {

            ApacheArrowWriter.Batch batch = writer.createNewBatch(0);
            ApacheArrowWriter.Row row = batch.writeNextRow();

            assertIllegalState("cannot call writeDecimal, actual type: Uuid", () -> row.writeDecimal("c1", dv));
            assertIllegalState("cannot call writeUuid, actual type: Decimal(22, 9)", () -> row.writeUuid("c2", uv));

            BulkUpsertArrowData data = batch.buildBatch();

            Schema schema = readApacheArrowSchema(data.getSchema());
            Assert.assertEquals("Schema<c1: FixedSizeBinary(16) not null, c2: FixedSizeBinary(16)>", schema.toString());
        }
    }

    private static Schema readApacheArrowSchema(ByteString bytes) {
        try (InputStream is = bytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
              return MessageSerializer.deserializeSchema(channel);
            }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
    }
}
