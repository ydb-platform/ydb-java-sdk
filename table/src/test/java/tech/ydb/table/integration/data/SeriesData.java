package tech.ydb.table.integration.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tech.ydb.table.utils.ExtendedRandom;
import tech.ydb.table.utils.Pair;

public final class SeriesData {
    private static final ExtendedRandom RANDOM = new ExtendedRandom();
    public static final List<Series> SERIES = Stream.generate(() -> new Series(
            RANDOM.nextLong(),
            RANDOM.nextString('A', 'z', 8),
            RANDOM.nextString('A', 'z', 20)
    )).limit(10).collect(Collectors.toList());
    public static final List<Season> SEASONS = Stream.generate(() -> new Season(
            SERIES.get(RANDOM.nextInt(SERIES.size())).seriesID,
            RANDOM.nextLong(),
            RANDOM.nextString('A', 'z', 8)
    )).limit(20).collect(Collectors.toList());

    private SeriesData() {
    }

    public enum TablesData {
        SERIES(Arrays.asList("series_id", "title", "series_info"), "series"),
        SEASON(Arrays.asList("series_id", "season_id", "title"), "seasons");
        private final List<String> columns;
        private final String tableName;

        TablesData(List<String> columns, String tableName) {
            this.columns = columns;
            this.tableName = tableName;
        }

        public List<String> getColumns() {
            return columns;
        }

        public String getName() {
            return tableName;
        }

        public List<? extends ExtractablePrimaryKey<Long>> getSampleData() {
            switch (this) {
                case SEASON:
                    return SeriesData.SEASONS;
                case SERIES:
                    return SeriesData.SERIES;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public static class Season implements ExtractablePrimaryKey<Long> {
        private final long seriesID;
        private final long seasonID;
        private final String title;

        public Season(long seriesID, long seasonID, String title) {
            this.seriesID = seriesID;
            this.seasonID = seasonID;
            this.title = title;
        }

        public long seriesID() {
            return this.seriesID;
        }

        public long seasonID() {
            return this.seasonID;
        }

        public String title() {
            return this.title;
        }

        @Override
        public List<Pair<String, Long>> getPrimaryKey() {
            return Arrays.asList(new Pair<>("series_id", seriesID), new Pair<>("season_id", seasonID));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Season)) {
                return false;
            }
            Season season = (Season) o;
            return seriesID == season.seriesID && seasonID == season.seasonID && Objects.equals(title, season.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seriesID, seasonID, title);
        }
    }

    public static class Series implements ExtractablePrimaryKey<Long> {
        private final long seriesID;
        private final String title;
        private final String seriesInfo;

        public Series(long seriesID, String title, String seriesInfo) {
            this.seriesID = seriesID;
            this.title = title;
            this.seriesInfo = seriesInfo;
        }

        public long seriesID() {
            return seriesID;
        }

        public String title() {
            return title;
        }

        public String seriesInfo() {
            return seriesInfo;
        }

        @Override
        public List<Pair<String, Long>> getPrimaryKey() {
            return Collections.singletonList(new Pair<>("series_id", seriesID));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Series)) {
                return false;
            }
            Series series = (Series) o;
            return seriesID == series.seriesID && Objects.equals(title, series.title) &&
                    Objects.equals(seriesInfo, series.seriesInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(seriesID, title, seriesInfo);
        }
    }
}
