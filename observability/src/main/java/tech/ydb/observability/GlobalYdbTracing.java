package tech.ydb.observability;

import tech.ydb.observability.impl.NoopYdbTracing;

final class GlobalYdbTracing {
    static volatile YdbTracing INSTANCE = NoopYdbTracing.INSTANCE;

    private GlobalYdbTracing() {
        // No operations.
    }
}
