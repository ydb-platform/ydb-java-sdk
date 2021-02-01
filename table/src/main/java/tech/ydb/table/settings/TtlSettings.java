package tech.ydb.table.settings;

import javax.annotation.Nonnull;

public final class TtlSettings {
    private final String dateTimeColumn;
    private final int expireAfterSeconds;

    public TtlSettings(@Nonnull String dateTimeColumn, int expireAfterSeconds) {
        this.dateTimeColumn = dateTimeColumn;
        this.expireAfterSeconds = expireAfterSeconds;
    }

    public String getDateTimeColumn() {
        return dateTimeColumn;
    }

    public int getExpireAfterSeconds() {
        return expireAfterSeconds;
    }
}
