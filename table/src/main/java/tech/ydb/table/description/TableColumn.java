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

    private final boolean hasDefaultValue;

    public TableColumn(String name, Type type, String family, boolean hasDefaultValue) {
        this.name = name;
        this.type = type;
        this.family = family;
        this.hasDefaultValue = hasDefaultValue;
    }

    public TableColumn(String name, Type type, String family) {
        this(name, type, family, false);
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

    public boolean hasDefaultValue() {
        return hasDefaultValue;
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
