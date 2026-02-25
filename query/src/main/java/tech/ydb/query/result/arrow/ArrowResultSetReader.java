package tech.ydb.query.result.arrow;

import java.util.Map;

import com.google.common.collect.Maps;
import io.grpc.ExperimentalApi;
import org.apache.arrow.vector.VectorSchemaRoot;

import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public class ArrowResultSetReader implements ResultSetReader {
    private final boolean isTruncated;
    private final VectorSchemaRoot vsr;
    private final ArrowValueReader<?>[] readers;
    private final Map<String, Integer> columnIndexes;
    private int rowIndex = -1; // before first

    public ArrowResultSetReader(VectorSchemaRoot vsr, ArrowValueReader<?>[] readers, boolean isTruncated) {
        this.vsr = vsr;
        this.readers = readers;
        this.columnIndexes = Maps.newHashMapWithExpectedSize(readers.length);
        for (int idx = 0; idx < readers.length; idx++) {
            columnIndexes.put(readers[idx].getName(), idx);
        }
        this.isTruncated = isTruncated;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    @Override
    public boolean isTruncated() {
        return isTruncated;
    }

    @Override
    public int getRowCount() {
        return vsr.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return readers.length;
    }

    @Override
    public void setRowIndex(int index) {
        if (index <= -1) {
            rowIndex = -1; // before first
            return;
        }

        if (index >= vsr.getRowCount()) {
            rowIndex = vsr.getRowCount(); // after last
            return;
        }

        rowIndex = index;
    }

    @Override
    public boolean next() {
        rowIndex++;

        if (rowIndex >= vsr.getRowCount()) {
            rowIndex = vsr.getRowCount(); // after last
            return false;
        }

        return true;
    }

    @Override
    public String getColumnName(int index) {
        if (index < 0 || index >= readers.length) {
            throw new IllegalArgumentException("Column index: " + index + ", columns count: " + readers.length);
        }
        return readers[index].getName();
    }

    @Override
    public int getColumnIndex(String name) {
        Integer index = columnIndexes.get(name);
        return index == null ? -1 : index;
    }

    @Override
    public ValueReader getColumn(int index) {
        if (rowIndex < 0 || rowIndex >= vsr.getRowCount()) {
            throw new IllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.");
        }
        if (index < 0 || index >= readers.length) {
            throw new IllegalArgumentException("Column index: " + index + ", columns count: " + readers.length);
        }
        ArrowValueReader<?> reader = readers[index];
        reader.setRowIndex(rowIndex);
        return reader;
    }

    @Override
    public ValueReader getColumn(String name) {
        Integer index = columnIndexes.get(name);
        if (index == null) {
            throw new IllegalArgumentException("Unknown column '" + name + "'");
        }
        return getColumn(index);
    }

    @Override
    public Type getColumnType(int index) {
        if (index < 0 || index >= readers.length) {
            throw new IllegalArgumentException("Column index: " + index + ", columns count: " + readers.length);
        }
        return readers[index].getType();
    }
}
