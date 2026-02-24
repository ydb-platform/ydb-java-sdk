package tech.ydb.query.result.array;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.table.values.PrimitiveType;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ArrayResultSetReaderTest {
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

    private void assertIllegalArgumentException(String msg, ThrowingRunnable runnable) {
        IllegalArgumentException ex = Assert.assertThrows(IllegalArgumentException.class, runnable);
        Assert.assertEquals(msg, ex.getMessage());
    }

    @Test
    public void columnValidationTest() {
        IntVector col1 = new IntVector("col1", FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        BigIntVector col2 = new BigIntVector("col2", FieldType.nullable(new ArrowType.Int(64, true)), allocator);

        try (VectorSchemaRoot vsr = VectorSchemaRoot.of(col1, col2)) {
            ArrayValueReader<?> r1 = ArrayValueReader.createReader(col1, PrimitiveType.Int32, true);
            ArrayValueReader<?> r2 = ArrayValueReader.createReader(col2, PrimitiveType.Int64, true);
            ArrayResultSetReader rs = new ArrayResultSetReader(vsr, new ArrayValueReader<?>[] { r2, r1 }, true);

            Assert.assertTrue(rs.isTruncated());
            Assert.assertEquals(2, rs.getColumnCount());
            Assert.assertEquals(0, rs.getRowCount());

            Assert.assertEquals("col2", rs.getColumnName(0));
            Assert.assertEquals("col1", rs.getColumnName(1));
            assertIllegalArgumentException("Column index: 2, columns count: 2", () -> rs.getColumnName(2));
            assertIllegalArgumentException("Column index: -1, columns count: 2", () -> rs.getColumnName(-1));

            Assert.assertEquals(PrimitiveType.Int64.makeOptional(), rs.getColumnType(0));
            Assert.assertEquals(PrimitiveType.Int32.makeOptional(), rs.getColumnType(1));
            assertIllegalArgumentException("Column index: 2, columns count: 2", () -> rs.getColumnType(2));
            assertIllegalArgumentException("Column index: -1, columns count: 2", () -> rs.getColumnType(-1));

            Assert.assertEquals(1, rs.getColumnIndex("col1"));
            Assert.assertEquals(0, rs.getColumnIndex("col2"));
            Assert.assertEquals(-1, rs.getColumnIndex("col3"));

            assertIllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.",
                    () -> rs.getColumn(0));
            assertIllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.",
                    () -> rs.getColumn("col1"));

            Assert.assertFalse(rs.next());
        }
    }

    @Test
    public void iterateTest() {
        IntVector col1 = new IntVector("col1", FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        col1.allocateNew(3);
        col1.set(0, 1234567);
        col1.setNull(1);
        col1.set(2, 7654321);

        BigIntVector col2 = new BigIntVector("col2", FieldType.nullable(new ArrowType.Int(64, true)), allocator);
        col2.allocateNew(3);
        col2.set(0, 1234567890123456789L);
        col2.set(1, 3456789012345678L);
        col2.setNull(2);

        try (VectorSchemaRoot vsr = VectorSchemaRoot.of(col1, col2)) {
            vsr.setRowCount(3);

            ArrayValueReader<?> r1 = ArrayValueReader.createReader(col1, PrimitiveType.Int32, true);
            ArrayValueReader<?> r2 = ArrayValueReader.createReader(col2, PrimitiveType.Int64, true);
            ArrayResultSetReader rs = new ArrayResultSetReader(vsr, new ArrayValueReader<?>[] { r1, r2 }, false);

            Assert.assertFalse(rs.isTruncated());
            Assert.assertEquals(3, rs.getRowCount());

            Assert.assertEquals(-1, rs.getRowIndex()); // before first
            Assert.assertTrue(rs.next());
            Assert.assertEquals(0, rs.getRowIndex()); // first

            Assert.assertEquals(1234567, rs.getColumn(0).getInt32());
            Assert.assertEquals(1234567, rs.getColumn("col1").getInt32());
            Assert.assertEquals(1234567890123456789L, rs.getColumn(1).getInt64());
            Assert.assertEquals(1234567890123456789L, rs.getColumn("col2").getInt64());

            assertIllegalArgumentException("Column index: 2, columns count: 2", () -> rs.getColumn(2));
            assertIllegalArgumentException("Column index: -1, columns count: 2", () -> rs.getColumn(-1));
            assertIllegalArgumentException("Unknown column 'col3'", () -> rs.getColumn("col3"));

            Assert.assertTrue(rs.next());
            Assert.assertEquals(1, rs.getRowIndex()); // second

            Assert.assertFalse(rs.getColumn(0).isOptionalItemPresent());
            Assert.assertFalse(rs.getColumn("col1").isOptionalItemPresent());
            Assert.assertEquals(3456789012345678L, rs.getColumn(1).getInt64());
            Assert.assertEquals(3456789012345678L, rs.getColumn("col2").getInt64());

            Assert.assertTrue(rs.next());
            Assert.assertEquals(2, rs.getRowIndex()); // last

            Assert.assertEquals(7654321, rs.getColumn(0).getInt32());
            Assert.assertEquals(7654321, rs.getColumn("col1").getInt32());
            Assert.assertFalse(rs.getColumn(1).isOptionalItemPresent());
            Assert.assertFalse(rs.getColumn("col2").isOptionalItemPresent());

            Assert.assertFalse(rs.next());
            Assert.assertEquals(3, rs.getRowIndex()); // after last
        }
    }

    @Test
    public void randomAccessTest() {
        IntVector col1 = new IntVector("col1", FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        col1.allocateNew(3);
        col1.set(0, 1234567);
        col1.setNull(1);
        col1.set(2, 7654321);

        BigIntVector col2 = new BigIntVector("col2", FieldType.nullable(new ArrowType.Int(64, true)), allocator);
        col2.allocateNew(3);
        col2.set(0, 1234567890123456789L);
        col2.set(1, 3456789012345678L);
        col2.setNull(2);

        try (VectorSchemaRoot vsr = VectorSchemaRoot.of(col1, col2)) {
            vsr.setRowCount(3);

            ArrayValueReader<?> r1 = ArrayValueReader.createReader(col1, PrimitiveType.Int32, true);
            ArrayValueReader<?> r2 = ArrayValueReader.createReader(col2, PrimitiveType.Int64, true);
            ArrayResultSetReader rs = new ArrayResultSetReader(vsr, new ArrayValueReader<?>[] { r1, r2 }, false);

            Assert.assertFalse(rs.isTruncated());
            Assert.assertEquals(3, rs.getRowCount());

            Assert.assertEquals(-1, rs.getRowIndex()); // before first
            rs.setRowIndex(0);
            Assert.assertEquals(0, rs.getRowIndex()); // first

            Assert.assertEquals(1234567, rs.getColumn(0).getInt32());
            Assert.assertEquals(1234567, rs.getColumn("col1").getInt32());
            Assert.assertEquals(1234567890123456789L, rs.getColumn(1).getInt64());
            Assert.assertEquals(1234567890123456789L, rs.getColumn("col2").getInt64());

            rs.setRowIndex(-100); // before first
            Assert.assertEquals(-1, rs.getRowIndex()); // before first

            rs.setRowIndex(2); // last
            Assert.assertEquals(2, rs.getRowIndex()); // before first

            Assert.assertEquals(7654321, rs.getColumn(0).getInt32());
            Assert.assertEquals(7654321, rs.getColumn("col1").getInt32());
            Assert.assertFalse(rs.getColumn(1).isOptionalItemPresent());
            Assert.assertFalse(rs.getColumn("col2").isOptionalItemPresent());

            rs.setRowIndex(3); // after last
            Assert.assertEquals(3, rs.getRowIndex()); // after last

            assertIllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.",
                    () -> rs.getColumn(0));
            assertIllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.",
                    () -> rs.getColumn("col1"));

            Assert.assertFalse(rs.next());
            Assert.assertEquals(3, rs.getRowIndex()); // after last

            rs.setRowIndex(100); // after last
            Assert.assertEquals(3, rs.getRowIndex()); // after last
        }
    }
}
