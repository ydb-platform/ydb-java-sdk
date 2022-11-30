package tech.ydb.table.description;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TableTtl {
    @Nonnull
    private final TtlMode ttlMode;
    @Nullable
    private final String dateTimeColumn;
    @Nullable
    private final Integer expireAfterSeconds;

    public TableTtl(@Nonnull TtlMode ttlMode, @Nonnull String dateTimeColumn, @Nonnull Integer expireAfterSeconds) {
        this.ttlMode = ttlMode;
        this.dateTimeColumn = dateTimeColumn;
        this.expireAfterSeconds = expireAfterSeconds;
    }

    public TableTtl() {
        this.ttlMode = TtlMode.NOT_SET;
        this.dateTimeColumn = null;
        this.expireAfterSeconds = null;
    }

    @Nonnull
    public TtlMode getTtlMode() {
        return ttlMode;
    }

    @Nullable
    public String getDateTimeColumn() {
        return dateTimeColumn;
    }

    @Nullable
    public Integer getExpireAfterSeconds() {
        return expireAfterSeconds;
    }

    public enum TtlMode {
        DATE_TYPE_COLUMN(1),
        VALUE_SINCE_UNIX_EPOCH(2),
        NOT_SET(0);

        private final int caseMapping;

        TtlMode(int caseMapping) {
            this.caseMapping = caseMapping;
        }

        public static TtlMode forCase(int value) {
            for (TtlMode mode: values()) {
                if (mode.caseMapping == value) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("No TTL mode defined for specified value");
        }
    }
}
