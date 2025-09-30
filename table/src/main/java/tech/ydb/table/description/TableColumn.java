package tech.ydb.table.description;

import javax.annotation.Nullable;

import tech.ydb.table.values.PrimitiveValue;
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
    @Nullable
    private final PrimitiveValue literalDefaultValue;
    @Nullable
    private final SequenceDescription sequenceDescription;

    public TableColumn(String name, Type type, String family, boolean hasDefaultValue) {
        this.name = name;
        this.type = type;
        this.family = family;
        this.hasDefaultValue = hasDefaultValue;
        this.literalDefaultValue = null;
        this.sequenceDescription = null;
    }

    public TableColumn(String name, Type type, @Nullable String family, PrimitiveValue literalDefaultValue) {
        this.name = name;
        this.type = type;
        this.family = family;
        this.hasDefaultValue = true;
        this.literalDefaultValue = literalDefaultValue;
        this.sequenceDescription = null;
    }

    public TableColumn(String name, Type type, @Nullable String family, SequenceDescription sequenceDescription) {
        this.name = name;
        this.type = type;
        this.family = family;
        this.hasDefaultValue = true;
        this.literalDefaultValue = null;
        this.sequenceDescription = sequenceDescription;
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

    @Nullable
    public PrimitiveValue getLiteralDefaultValue() {
        return literalDefaultValue;
    }

    @Nullable
    public SequenceDescription getSequenceDescription() {
        return sequenceDescription;
    }
}
