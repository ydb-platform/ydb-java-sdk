package tech.ydb.table.result;

/**
 * @author Sergey Polovko
 */
public interface ResultSetReader {

    /**
     * Returns {@code true} if the result was truncated, {@code false} otherwise.
     */
    boolean isTruncated();

    /**
     * Returns number of columns.
     */
    int getColumnCount();

    /**
     * Returns number of rows.
     */
    int getRowCount();

    /**
     * Explicitly switch to a specific row.
     */
    void setRowIndex(int index);

    /**
     * Set iterator to the next table row.
     *
     * On success tryNextRow will reset all column parsers to the values in next row.
     * Column parsers are invalid before the first TryNextRow call.
     */
    boolean next();

    /**
     * Returns column name by index.
     */
    String getColumnName(int index);

    /**
     * Returns value reader for column by index.
     */
    ValueReader getColumn(int index);

    /**
     * Returns value reader for column by name.
     */
    ValueReader getColumn(String name);
}