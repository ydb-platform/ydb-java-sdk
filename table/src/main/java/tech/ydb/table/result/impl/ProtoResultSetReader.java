package tech.ydb.table.result.impl;

import java.util.HashMap;
import java.util.Map;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
final class ProtoResultSetReader implements ResultSetReader {

    private final ValueProtos.ResultSet rs;
    private final Map<String, Integer> columnIndexes;
    private final AbstractValueReader[] readers;

    private int rowIndex = -1; // before first
    private ValueProtos.Value currentRow = null;

    ProtoResultSetReader(ValueProtos.ResultSet resultSet) {
        this.rs = resultSet;
        this.columnIndexes = new HashMap<>();
        this.readers = new AbstractValueReader[resultSet.getColumnsCount()];

        for (int i = 0; i < resultSet.getColumnsCount(); i++) {
            ValueProtos.Column columnMeta = resultSet.getColumns(i);
            this.columnIndexes.put(columnMeta.getName(), i);
            this.readers[i] = ProtoValueReaders.forTypeImpl(columnMeta.getType());
        }
    }

    @Override
    public boolean isTruncated() {
        return rs.getTruncated();
    }

    @Override
    public int getColumnCount() {
        return rs.getColumnsCount();
    }

    @Override
    public int getRowCount() {
        return rs.getRowsCount();
    }

    @Override
    public void setRowIndex(int index) {
        if (index <= -1) {
            rowIndex = -1; // before first
            currentRow = null;
            return;
        }

        if (index >= rs.getRowsCount()) {
            rowIndex = rs.getRowsCount(); // after last
            currentRow = null;
            return;
        }

        rowIndex = index;
        currentRow = rs.getRows(rowIndex);
    }

    @Override
    public boolean next() {
        rowIndex++;

        if (rowIndex >= rs.getRowsCount()) {
            rowIndex = rs.getRowsCount(); // after last
            currentRow = null;
            return false;
        }

        currentRow = rs.getRows(rowIndex);
        return true;
    }

    @Override
    public String getColumnName(int index) {
        if (index < 0 || index >= rs.getColumnsCount()) {
            throw new IllegalArgumentException("Column index: " + index + ", columns count: " + readers.length);
        }
        return rs.getColumns(index).getName();
    }

    @Override
    public int getColumnIndex(String name) {
        Integer index = columnIndexes.get(name);
        return index == null ? -1 : index;
    }

    @Override
    public ValueReader getColumn(int index) {
        if (currentRow == null) {
            throw new IllegalStateException("ResultSetReader not positioned properly, perhaps you need to call next.");
        }
        if (index < 0 || index >= readers.length) {
            throw new IllegalArgumentException("Column index: " + index + ", columns count: " + readers.length);
        }
        AbstractValueReader reader = readers[index];
        reader.setProtoValue(currentRow.getItems(index));
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
        AbstractValueReader reader = readers[index];
        return reader.getType();
    }

    @Deprecated
    ValueProtos.ResultSet getResultSet() {
        return rs;
    }
}
