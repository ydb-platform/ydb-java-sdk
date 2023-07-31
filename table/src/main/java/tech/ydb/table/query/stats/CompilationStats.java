package tech.ydb.table.query.stats;

import java.io.Serializable;
import java.util.Objects;

public final class CompilationStats implements Serializable {
    private static final CompilationStats DEFAULT_INSTANCE = new CompilationStats();
    private boolean fromCache;
    private long durationUs;
    private long cpuTimeUs;
    private int memoizedHashCode;

    private CompilationStats() {
    }

    public CompilationStats(tech.ydb.proto.YdbQueryStats.CompilationStats protoAutoGenCompilationStats) {
        this.fromCache = protoAutoGenCompilationStats.getFromCache();
        this.durationUs = protoAutoGenCompilationStats.getDurationUs();
        this.cpuTimeUs = protoAutoGenCompilationStats.getCpuTimeUs();
    }

    public static CompilationStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public boolean getFromCache() {
        return this.fromCache;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long getCpuTimeUs() {
        return this.cpuTimeUs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof CompilationStats)) {
            return super.equals(obj);
        } else {
            CompilationStats other = (CompilationStats) obj;
            return Objects.equals(getDurationUs(), other.getDurationUs()) &&
                    Objects.equals(getFromCache(), other.getFromCache()) &&
                    Objects.equals(getCpuTimeUs(), other.getCpuTimeUs());
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode == 0) {
            this.memoizedHashCode = Objects.hash(fromCache, durationUs, cpuTimeUs);
        }
        return this.memoizedHashCode;
    }

    @Override
    public String toString() {
        return "CompilationStats{" + "fromCache=" + fromCache + ", durationUs=" + durationUs + ", cpuTimeUs=" +
                cpuTimeUs + '}';
    }
}
