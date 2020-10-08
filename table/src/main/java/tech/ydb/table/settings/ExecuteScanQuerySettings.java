package tech.ydb.table.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import tech.ydb.table.YdbTable;

import static tech.ydb.table.YdbTable.ExecuteScanQueryRequest.Mode.MODE_UNSPECIFIED;
import static tech.ydb.table.YdbTable.QueryStatsCollection.Mode.STATS_COLLECTION_NONE;

public class ExecuteScanQuerySettings {
    private final YdbTable.ExecuteScanQueryRequest.Mode mode;
    private final long timeoutNanos;
    private final YdbTable.QueryStatsCollection.Mode collectStats;

    public ExecuteScanQuerySettings(Builder b) {
        this.timeoutNanos = b.timeoutNanos;
        this.mode = b.mode;
        this.collectStats = b.collectStats;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private long timeoutNanos = Duration.ofSeconds(60).toNanos();
        private YdbTable.ExecuteScanQueryRequest.Mode mode = MODE_UNSPECIFIED;
        private YdbTable.QueryStatsCollection.Mode collectStats = STATS_COLLECTION_NONE;

        public Builder timeout(long duration, TimeUnit unit) {
            this.timeoutNanos = unit.toNanos(duration);
            return this;
        }

        public Builder timeout(Duration duration) {
            this.timeoutNanos = duration.toNanos();
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


    public long getDeadlineAfter() {
        return System.nanoTime() + timeoutNanos;
    }

    public YdbTable.ExecuteScanQueryRequest.Mode getMode() {
        return mode;
    }

    public YdbTable.QueryStatsCollection.Mode getCollectStats() {
        return collectStats;
    }
}
