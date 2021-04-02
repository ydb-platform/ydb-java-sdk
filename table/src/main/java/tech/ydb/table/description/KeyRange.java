package tech.ydb.table.description;

import java.util.Optional;

public class KeyRange {
    private final Optional<KeyBound> from;
    private final Optional<KeyBound> to;

    public KeyRange(
            Optional<KeyBound> from,
            Optional<KeyBound> to
    ) {
        this.from = from;
        this.to = to;
    }

    public Optional<KeyBound> getFrom() {
        return from;
    }

    public Optional<KeyBound> getTo() {
        return to;
    }
}
