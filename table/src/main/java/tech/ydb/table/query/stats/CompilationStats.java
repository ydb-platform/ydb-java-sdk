package tech.ydb.table.query.stats;

import java.util.Objects;

public final class CompilationStats {
    private final boolean fromCache;
    private final long durationUs;
    private final long cpuTimeUs;

    public CompilationStats(tech.ydb.proto.YdbQueryStats.CompilationStats protoAutoGenCompilationStats) {
        this(
                protoAutoGenCompilationStats.getFromCache(),
                protoAutoGenCompilationStats.getDurationUs(),
                protoAutoGenCompilationStats.getCpuTimeUs()
        );
    }

    public CompilationStats(boolean fromCache, long durationUs, long cpuTimeUs) {
        this.fromCache = fromCache;
        this.durationUs = durationUs;
        this.cpuTimeUs = cpuTimeUs;
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
        return Objects.hash(fromCache, durationUs, cpuTimeUs);
    }

    @Override
    public String toString() {
        return "CompilationStats{" + "fromCache=" + fromCache + ", durationUs=" + durationUs + ", cpuTimeUs=" +
                cpuTimeUs + '}';
    }
}
