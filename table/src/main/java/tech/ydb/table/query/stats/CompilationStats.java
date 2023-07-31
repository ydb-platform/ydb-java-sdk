package tech.ydb.table.query.stats;

import java.io.Serializable;

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

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public void setDurationUs(long durationUs) {
        this.durationUs = durationUs;
    }

    public long getCpuTimeUs() {
        return this.cpuTimeUs;
    }

    public void setCpuTimeUs(long cpuTimeUs) {
        this.cpuTimeUs = cpuTimeUs;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof CompilationStats)) {
            return super.equals(obj);
        } else {
            CompilationStats other = (CompilationStats) obj;
            if (this.getFromCache() != other.getFromCache()) {
                return false;
            } else if (this.getDurationUs() != other.getDurationUs()) {
                return false;
            } else {
                return this.getCpuTimeUs() == other.getCpuTimeUs();
            }
        }
    }

    @Override
    public int hashCode() {
        if (this.memoizedHashCode != 0) {
            return this.memoizedHashCode;
        } else {
            int hash = 41;
            hash = 37 * hash + 1;
            hash = 53 * hash + Boolean.hashCode(getFromCache());
            hash = 37 * hash + 2;
            hash = 53 * hash + Long.hashCode(getDurationUs());
            hash = 37 * hash + 3;
            hash = 53 * hash + Long.hashCode(getCpuTimeUs());
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    @Override
    public String toString() {
        return "CompilationStats{" + "fromCache=" + fromCache + ", durationUs=" + durationUs + ", cpuTimeUs=" +
                cpuTimeUs + '}';
    }
}
