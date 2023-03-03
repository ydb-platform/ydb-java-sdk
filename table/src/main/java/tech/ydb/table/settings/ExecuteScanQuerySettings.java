package tech.ydb.table.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import tech.ydb.table.YdbTable;

import static tech.ydb.table.YdbTable.ExecuteScanQueryRequest.Mode.MODE_EXEC;
import static tech.ydb.table.YdbTable.QueryStatsCollection.Mode.STATS_COLLECTION_NONE;

public class ExecuteScanQuerySettings {
    private final YdbTable.ExecuteScanQueryRequest.Mode mode;
    private final Duration timeout;
    private final YdbTable.QueryStatsCollection.Mode collectStats;

    public ExecuteScanQuerySettings(Builder b) {
        this.timeout = b.timeout;
        this.mode = b.mode;
        this.collectStats = b.collectStats;
    }

    public Duration getTimeoutDuration() {
        return timeout;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Duration timeout = Duration.ofSeconds(60);
        private YdbTable.ExecuteScanQueryRequest.Mode mode = MODE_EXEC;
        private YdbTable.QueryStatsCollection.Mode collectStats = STATS_COLLECTION_NONE;

        public Builder timeout(long duration, TimeUnit unit) {
            if (duration > 0) {
                this.timeout = Duration.ofNanos(unit.toNanos(duration));
            }
            return this;
        }

        public Builder timeout(Duration duration) {
            if (duration != null && !duration.isNegative()) {
                this.timeout = duration;
            }
            return this;
        }

        public Builder mode(YdbTable.ExecuteScanQueryRequest.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder collectStats(YdbTable.QueryStatsCollection.Mode collectStats) {
            this.collectStats = collectStats;
            return this;
        }

        public ExecuteScanQuerySettings build() {
            return new ExecuteScanQuerySettings(this);
        }
    }


    public YdbTable.ExecuteScanQueryRequest.Mode getMode() {
        return mode;
    }

    public YdbTable.QueryStatsCollection.Mode getCollectStats() {
        return collectStats;
    }
}
