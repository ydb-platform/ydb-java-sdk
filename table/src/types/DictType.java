package ru.yandex.ydb.table.types;

/**
 * @author Sergey Polovko
 */
public final class DictType implements Type {

    private final Type keyType;
    private final Type valueType;

    private DictType(Type keyType, Type valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public static DictType of(Type keyType, Type valueType) {
        return new DictType(keyType, valueType);
    }

    @Override
    public Kind getKind() {
        return Kind.DICT;
    }

    public Type getKeyType() {
        return keyType;
    }

    public Type getValueType() {
        return valueType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || o.getClass() != DictType.class) return false;

        DictType dictType = (DictType) o;
        return keyType.equals(dictType.getKeyType()) && valueType.equals(dictType.getKeyType());
    }

    @Override
    public int hashCode() {
        int result = Kind.DICT.hashCode();
        result = 31 * result + keyType.hashCode();
        result = 31 * result + valueType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Dict<" + keyType + ", " + valueType + '>';
    }
}
