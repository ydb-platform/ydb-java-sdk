package tech.ydb.table.description;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Sergey Polovko
 * @author Kirill Kurdyukov
 */
public class TableIndex {

    public enum Type {
        GLOBAL,
    }

    public enum ConsistencyType {
        SYNC, ASYNC
    }

    /**
     * Name of the index.
     */
    private final String name;

    /**
     * List of indexed columns.
     */
    private final List<String> columns;

    /**
     * List of columns content to be copied in to index table.
     */
    private final List<String> dataColumns;

    /**
     * Index type.
     */
    private final Type type;

    /**
     * Consistency index type
     */
    private final ConsistencyType consistencyType;

    public TableIndex(
            String name,
            List<String> columns,
            Type type
    ) {
        this(name, columns, emptyList(), type);
    }

    public TableIndex(
            String name,
            List<String> columns,
            List<String> dataColumns,
            Type type
    ) {
        this.name = name;
        this.columns = columns;
        this.dataColumns = dataColumns;
        this.type = type;
        this.consistencyType = ConsistencyType.SYNC;
    }

    public TableIndex(
            String name,
            List<String> columns,
            Type type,
            ConsistencyType consistencyType
    ) {
        this.name = name;
        this.columns = columns;
        this.dataColumns = emptyList();
        this.type = type;
        this.consistencyType = consistencyType;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<String> getDataColumns() {
        return dataColumns;
    }

    public Type getType() {
        return type;
    }

    public ConsistencyType getConsistencyType() {
        return consistencyType;
    }
}
