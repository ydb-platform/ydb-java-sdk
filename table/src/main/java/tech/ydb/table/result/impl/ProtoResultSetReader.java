package tech.ydb.table.result.impl;

import java.util.Map;

import com.google.common.collect.Maps;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
final class ProtoResultSetReader implements ResultSetReader {

    private final ValueProtos.ResultSet resultSet;
    private final Map<String, Integer> columnIndexes; // TODO: use better data structure
    private final AbstractValueReader[] columnReaders;

    private int rowIndex;
    private ValueProtos.Value currentRow;

    ProtoResultSetReader(ValueProtos.ResultSet resultSet) {
        this.resultSet = resultSet;
        this.columnIndexes = Maps.newHashMapWithExpectedSize(resultSet.getColumnsCount());
        this.columnReaders = new AbstractValueReader[resultSet.getColumnsCount()];

        for (int i = 0; i < resultSet.getColumnsCount(); i++) {
            ValueProtos.Column columnMeta = resultSet.getColumns(i);
            this.columnIndexes.put(columnMeta.getName(), i);
            this.columnReaders[i] = ProtoValueReaders.forTypeImpl(columnMeta.getType());
        }
    }

    @Override
    public boolean isTruncated() {
        return resultSet.getTruncated();
    }

    /**
     * Returns number of columns.
     */
    @Override
    public int getColumnCount() {
        return resultSet.getColumnsCount();
    }

    /**
     * Returns number of rows.
     */
    @Override
    public int getRowCount() {
        return resultSet.getRowsCount();
    }

    @Override
    public void setRowIndex(int index) {
        if (index < 0 || index >= resultSet.getRowsCount()) {
            currentRow = null;
        } else {
            rowIndex = index;
            currentRow = resultSet.getRows(index);
        }
    }

    /**
     * Set iterator to the next table row.
     *
     * On success tryNextRow will reset all column parsers to the values in next row.
     * Column parsers are invalid before the first TryNextRow call.
     */
    @Override
    public boolean next() {
        if (rowIndex >= resultSet.getRowsCount()) {
            currentRow = null;
            return false;
        }
        currentRow = resultSet.getRows(rowIndex++);
        return true;
    }

    @Override
    public String getColumnName(int index) {
        return resultSet.getColumns(index).getName();
    }

    @Override
    public int getColumnIndex(String name) {
        Integer index = columnIndexes.get(name);
        return index == null ? -1 : index;
    }

    @Override
    public ValueReader getColumn(int index) {
        if (currentRow == null) {
            throw new IllegalStateException("empty result set or next() was never called");
        }
        AbstractValueReader reader = columnReaders[index];
        reader.setProtoValue(currentRow.getItems(index));
        return reader;
    }

    @Override
    public ValueReader getColumn(String name) {
        int index = columnIndex(name);
        return getColumn(index);
    }

    @Override
    public Type getColumnType(int index) {
        AbstractValueReader reader = columnReaders[index];
        return reader.getType();
    }

    private int columnIndex(String name) {
        Integer index = columnIndexes.get(name);
        if (index == null) {
            throw new IllegalArgumentException("unknown column '" + name + "'");
        }
        return index;
    }

    ValueProtos.ResultSet getResultSet() {
        return resultSet;
    }
}
