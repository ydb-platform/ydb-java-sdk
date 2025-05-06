package tech.ydb.topic.description;

import tech.ydb.proto.topic.YdbTopic;

import java.util.Objects;

/**
 * @author Nikolay Perfilov
 */
public class MultipleWindowsStat {
    private final long perMinute;
    private final long perHour;
    private final long perDay;

    public MultipleWindowsStat(YdbTopic.MultipleWindowsStat stat) {
        this.perMinute = stat.getPerMinute();
        this.perHour = stat.getPerHour();
        this.perDay = stat.getPerDay();
    }

    public MultipleWindowsStat(long perMinute, long perHour, long perDay) {
        this.perMinute = perMinute;
        this.perHour = perHour;
        this.perDay = perDay;
    }

    public long getPerMinute() {
        return perMinute;
    }

    public long getPerHour() {
        return perHour;
    }

    public long getPerDay() {
        return perDay;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MultipleWindowsStat that = (MultipleWindowsStat) o;
        return perMinute == that.perMinute && perHour == that.perHour && perDay == that.perDay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(perMinute, perHour, perDay);
    }
}
