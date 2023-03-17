package tech.ydb.topic.description;

/**
 * @author Nikolay Perfilov
 */
public class MultipleWindowsStat {
    private final long perMinute;
    private final long perHour;
    private final long perDay;

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
}
