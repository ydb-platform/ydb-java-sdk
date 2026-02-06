package tech.ydb.table.result;

import tech.ydb.table.values.Type;

/**
 * @author Sergey Polovko
 */
public interface ResultSetReader {

    /**
     * Gets whether or not this result set is complete.
     *
     * @return {@code true} if the result was truncated, {@code false} otherwise.
     */
    boolean isTruncated();

    /**
     * Gets number of this result set columns
     *
     * @return the result set columns count
     */
    int getColumnCount();

    /**
     * Gets number of this result set rows
     *
     * @return the result set rows count
     */
    int getRowCount();

    /**
     * Explicitly sets the reader on a specific row position.
     *
     * @param index the row index, zero is the first row
     */
    void setRowIndex(int index);

    /**
     * Set reader to the next table row.
     * <p>
     * On success {@link #next()} will reset all column parsers to the values in next row. Column parsers
     * are invalid before the first {@link #next()} call.
     *
     * @return returns {@code true} if the next row is available, {@code false} otherwise.
     */
    boolean next();

    /**
     * Gets column name by index.
     *
     * @param index the column index, zero is the first column
     * @return the column name
     * @throws IllegalArgumentException if the index is out of range
     *         (<code>index &lt; 0 || index &gt;= getColumnCount()</code>)
     */
    String getColumnName(int index);

    /**
     * Gets column type by index.
     *
     * @param index the column index, zero is the first column
     * @return the column type
     * @throws IllegalArgumentException if the index is out of range
     *         (<code>index &lt; 0 || index &gt;= getColumnCount()</code>)
     */
    Type getColumnType(int index);

    /**
     * Gets column index by name or {@code -1} if column with given name is not present.
     *
     * @param name the column name
     * @return the column index
     */
    int getColumnIndex(String name);

    /**
     * Gets the current row value reader by the column index
     *
     * @param index the column index, zero is the first column
     * @return the value reader
     * @throws IllegalArgumentException if the column index is out of range
     *         (<code>index &lt; 0 || index &gt;= getColumnCount()</code>)
     * @throws IllegalStateException if the result set reading was not started or was already finished
     */
    ValueReader getColumn(int index);

    /**
     * Gets the current row value reader by the column name
     *
     * @param name the column name
     * @return the value reader
     * @throws IllegalArgumentException if column with given name is not present
     * @throws IllegalStateException if the result set reading was not started or was already finished
     */
    ValueReader getColumn(String name);
}
