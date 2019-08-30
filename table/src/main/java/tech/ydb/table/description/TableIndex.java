package tech.ydb.table.description;

import java.util.List;

/**
 * @author Sergey Polovko
 */
public class TableIndex {

    public enum Type {
        GLOBAL,
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
     * Index type.
     */
    private final Type type;

    public TableIndex(String name, List<String> columns, Type type) {
        this.name = name;
        this.columns = columns;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public Type getType() {
        return type;
    }
}
