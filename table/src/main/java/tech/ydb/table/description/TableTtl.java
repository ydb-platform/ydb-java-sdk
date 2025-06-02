package tech.ydb.table.description;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TableTtl {
    private static final TableTtl NOT_SET = new TableTtl(TtlMode.NOT_SET, TtlUnit.UNSPECIFIED, "", 0, null);

    @Nonnull
    private final TtlMode ttlMode;
    @Nonnull
    private final TtlUnit ttlUnit;
    @Nonnull
    private final String dateTimeColumn;
    @Nonnull
    private final Integer expireAfterSeconds;
    @Nonnull
    private final Integer runIntervalSeconds;

    @Deprecated
    public TableTtl(@Nonnull TtlMode ttlMode, @Nonnull String dateTimeColumn, @Nonnull Integer expireAfterSeconds) {
        this.ttlMode = ttlMode;
        this.dateTimeColumn = dateTimeColumn;
        this.expireAfterSeconds = expireAfterSeconds;
        this.ttlUnit = TtlUnit.UNSPECIFIED;
        this.runIntervalSeconds = null;
    }

    @Deprecated
    public TableTtl() {
        this.ttlMode = TtlMode.NOT_SET;
        this.dateTimeColumn = "";
        this.expireAfterSeconds = 0;
        this.ttlUnit = TtlUnit.UNSPECIFIED;
        this.runIntervalSeconds = null;
    }

    private TableTtl(
            @Nonnull TtlMode mode,
            @Nonnull TtlUnit unit,
            @Nonnull String columnName,
            int expireAfterSeconds,
            Integer runIntervalSeconds
    ) {
        this.ttlMode = mode;
        this.dateTimeColumn = columnName;
        this.expireAfterSeconds = expireAfterSeconds;
        this.ttlUnit = unit;
        this.runIntervalSeconds = runIntervalSeconds;
    }

    @Nonnull
    public TtlMode getTtlMode() {
        return ttlMode;
    }

    @Nonnull
    public TtlUnit getTtlUnit() {
        return ttlUnit;
    }

    @Nonnull
    public String getDateTimeColumn() {
        return dateTimeColumn;
    }

    @Nonnull
    public Integer getExpireAfterSeconds() {
        return expireAfterSeconds;
    }

    /**
     * @deprecated use {{@link #getRunIntervalSeconds()}} instead
     */
    @Deprecated
    public Integer getRunIntervaelSeconds() {
        return getRunIntervalSeconds();
    }

    @Nullable
    public Integer getRunIntervalSeconds() {
        return runIntervalSeconds;
    }

    public TableTtl withRunIntervalSeconds(int seconds) {
        return new TableTtl(ttlMode, ttlUnit, dateTimeColumn, expireAfterSeconds, seconds);
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

    public enum TtlUnit {
        UNSPECIFIED,
        SECONDS,
        MILLISECONDS,
        MICROSECONDS,
        NANOSECONDS;
    }

    /**
     * Construct an empty TTL configuration
     *
     * @return instance of TTL configuration
     */
    public static TableTtl notSet() {
        return NOT_SET;
    }

    /**
     * The row will be considered as expired at the moment of time, when the value stored in <i>columnName</i> is less
     * than or equal to the current time (in epoch time format), and <i>expireAfterSeconds</i> has passed since that
     * moment; i.e. the expiration threshold is the value of <i>columnName</i>plus <i>expireAfterSeconds</i>.
     *
     * @param columnName name of column with type Date, Datetime or Timestamp
     * @param expireAfterSeconds number of seconds to add to the time in the column
     * @return instance of TTL configuration
     */
    public static TableTtl dateTimeColumn(@Nonnull String columnName, int expireAfterSeconds) {
        return new TableTtl(TtlMode.DATE_TYPE_COLUMN, TtlUnit.UNSPECIFIED, columnName, expireAfterSeconds, null);
    }

    /**
     * The row will be considered as expired at the moment of time, when the value stored in <i>columnName</i> is less
     * than or equal to the current time (in epoch time format), and <i>expireAfterSeconds</i> has passed since that
     * moment; i.e. the expiration threshold is the value of <i>columnName</i>plus <i>expireAfterSeconds</i>.
     *
     * @param columnName name of column with type UInt32, UInt64 or DyNumber
     * @param unit time unit  of column
     * @param expireAfterSeconds number of seconds to add to the time in the column
     * @return instance of TTL configuration
     */
    public static TableTtl valueSinceUnixEpoch(@Nonnull String columnName, TtlUnit unit, int expireAfterSeconds) {
        return new TableTtl(TtlMode.VALUE_SINCE_UNIX_EPOCH, unit, columnName, expireAfterSeconds, null);
    }
}
