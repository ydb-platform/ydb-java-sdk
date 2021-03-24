package tech.ydb.table.description;

import javax.annotation.Nullable;

import tech.ydb.table.values.Type;


/**
 * @author Sergey Polovko
 */
public class TableColumn {

    private final String name;
    private final Type type;
    @Nullable
    private final String family;

    public TableColumn(String name, Type type, String family) {
        this.name = name;
        this.type = type;
        this.family = family;
    }

    public TableColumn(String name, Type type) {
        this(name, type, null);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public String getFamily() {
        return family;
    }

    @Override
    public String toString() {
        return name + ' ' + type;
    }
}
