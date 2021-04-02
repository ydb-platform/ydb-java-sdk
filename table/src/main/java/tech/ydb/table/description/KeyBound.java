package tech.ydb.table.description;

import tech.ydb.table.values.Value;

public class KeyBound {
    private final Value value;
    private final boolean inclusive;

    private KeyBound(
            Value value,
            boolean inclusive
    ) {
        this.value = value;
        this.inclusive = inclusive;
    }

    public Value getValue() {
        return value;
    }

    public boolean isInclusive() {
        return inclusive;
    }

    public static KeyBound inclusive(Value value) {
        return new KeyBound(value, true);
    }

    public static KeyBound exclusive(Value value) {
        return new KeyBound(value, false);
    }
}
