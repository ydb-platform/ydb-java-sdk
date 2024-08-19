package tech.ydb.query;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

final class TestExampleData {
    public static class Series {
        private final long seriesID;
        private final String title;
        private final LocalDate releaseDate;
        private final String seriesInfo;

        public Series(long seriesID, String title, LocalDate releaseDate, String seriesInfo) {
            this.seriesID = seriesID;
            this.title = title;
            this.releaseDate = releaseDate;
            this.seriesInfo = seriesInfo;
        }

        public long seriesID() {
            return seriesID;
        }

        public String title() {
            return title;
        }

        public LocalDate releaseDate() {
            return releaseDate;
        }

        public String seriesInfo() {
            return seriesInfo;
        }
    }
    public static class Season {
        private final long seriesID;
        private final long seasonID;
        private final String title;
        private final LocalDate firstAired;
        private final LocalDate lastAired;

        public Season(long seriesID, long seasonID, String title, LocalDate firstAired, LocalDate lastAired) {
            this.seriesID = seriesID;
            this.seasonID = seasonID;
            this.title = title;
            this.firstAired = firstAired;
            this.lastAired = lastAired;
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

        public LocalDate firstAired() {
            return this.firstAired;
        }

        public LocalDate lastAired() {
            return this.lastAired;
        }
    }

    public static class Episode {
        private final long seriesID;
        private final long seasonID;
        private final long episodeID;
        private final String title;
        private final LocalDate airDate;

        public Episode(long seriesID, long seasonID, long episodeID, String title, LocalDate airDate) {
            this.seriesID = seriesID;
            this.seasonID = seasonID;
            this.episodeID = episodeID;
            this.title = title;
            this.airDate = airDate;
        }

        public long seriesID() {
            return seriesID;
        }

        public long seasonID() {
            return seasonID;
        }

        public long episodeID() {
            return episodeID;
        }

        public String title() {
            return title;
        }

        public LocalDate airDate() {
            return airDate;
        }
    }

    public static final List<Series> SERIES = Arrays.asList(
        new Series(
            1, "IT Crowd", LocalDate.of(2006, 2, 3),
            "The IT Crowd is a British sitcom produced by Channel 4, written by Graham Linehan, produced by " +
            "Ash Atalla and starring Chris O'Dowd, Richard Ayoade, Katherine Parkinson, and Matt Berry."),
        new Series(
            2, "Silicon Valley", LocalDate.of(2014, 4, 6),
            "Silicon Valley is an American comedy television series created by Mike Judge, John Altschuler and " +
            "Dave Krinsky. The series focuses on five young men who founded a startup company in Silicon Valley.")
    );

    public static final List<Season> SEASONS = Arrays.asList(
        new Season(1, 1, "Season 1", LocalDate.of(2006,  2,  3), LocalDate.of(2006,  3,  3)),
        new Season(1, 2, "Season 2", LocalDate.of(2007,  8, 24), LocalDate.of(2007,  9, 28)),
        new Season(1, 3, "Season 3", LocalDate.of(2008, 11, 21), LocalDate.of(2008, 12, 26)),
        new Season(1, 4, "Season 4", LocalDate.of(2010,  6, 25), LocalDate.of(2010,  7, 30)),
        new Season(2, 1, "Season 1", LocalDate.of(2014,  4,  6), LocalDate.of(2014,  6,  1)),
        new Season(2, 2, "Season 2", LocalDate.of(2015,  4, 12), LocalDate.of(2015,  6, 14)),
        new Season(2, 3, "Season 3", LocalDate.of(2016,  4, 24), LocalDate.of(2016,  6, 26)),
        new Season(2, 4, "Season 4", LocalDate.of(2017,  4, 23), LocalDate.of(2017,  6, 25)),
        new Season(2, 5, "Season 5", LocalDate.of(2018,  3, 25), LocalDate.of(2018,  5, 13))
    );

    public static final List<Episode> EPISODES = Arrays.asList(
        new Episode(1, 1, 1, "Yesterday's Jam",                   LocalDate.of(2006,  2,  3)),
        new Episode(1, 1, 2, "Calamity Jen",                      LocalDate.of(2006,  2,  3)),
        new Episode(1, 1, 3, "Fifty, Fifty",                      LocalDate.of(2006,  2, 10)),
        new Episode(1, 1, 4, "The Red Door",                      LocalDate.of(2006,  2, 17)),
        new Episode(1, 1, 5, "The Haunting of Bill Crouse",       LocalDate.of(2006,  2, 24)),
        new Episode(1, 1, 6, "Aunt Irma Visits",                  LocalDate.of(2006,  3,  3)),
        new Episode(1, 2, 1, "The Work Outing",                   LocalDate.of(2006,  8, 24)),
        new Episode(1, 2, 2, "Return of the Golden Child",        LocalDate.of(2007,  8, 31)),
        new Episode(1, 2, 3, "Moss and the German",               LocalDate.of(2007,  9,  7)),
        new Episode(1, 2, 4, "The Dinner Party",                  LocalDate.of(2007,  9, 14)),
        new Episode(1, 2, 5, "Smoke and Mirrors",                 LocalDate.of(2007,  9, 21)),
        new Episode(1, 2, 6, "Men Without Women",                 LocalDate.of(2007,  9, 28)),
        new Episode(1, 3, 1, "From Hell",                         LocalDate.of(2008, 11, 21)),
        new Episode(1, 3, 2, "Are We Not Men?",                   LocalDate.of(2008, 11, 28)),
        new Episode(1, 3, 3, "Tramps Like Us",                    LocalDate.of(2008, 12,  5)),
        new Episode(1, 3, 4, "The Speech",                        LocalDate.of(2008, 12, 12)),
        new Episode(1, 3, 5, "Friendface",                        LocalDate.of(2008, 12, 19)),
        new Episode(1, 3, 6, "Calendar Geeks",                    LocalDate.of(2008, 12, 26)),
        new Episode(1, 4, 1, "Jen The Fredo",                     LocalDate.of(2010,  6, 25)),
        new Episode(1, 4, 2, "The Final Countdown",               LocalDate.of(2010,  7,  2)),
        new Episode(1, 4, 3, "Something Happened",                LocalDate.of(2010,  7,  9)),
        new Episode(1, 4, 4, "Italian For Beginners",             LocalDate.of(2010,  7, 16)),
        new Episode(1, 4, 5, "Bad Boys",                          LocalDate.of(2010,  7, 23)),
        new Episode(1, 4, 6, "Reynholm vs Reynholm",              LocalDate.of(2010,  7, 30)),
        new Episode(2, 1, 1, "Minimum Viable Product",            LocalDate.of(2014,  4,  6)),
        new Episode(2, 1, 2, "The Cap Table",                     LocalDate.of(2014,  4, 13)),
        new Episode(2, 1, 3, "Articles of Incorporation",         LocalDate.of(2014,  4, 20)),
        new Episode(2, 1, 4, "Fiduciary Duties",                  LocalDate.of(2014,  4, 27)),
        new Episode(2, 1, 5, "Signaling Risk",                    LocalDate.of(2014,  5,  4)),
        new Episode(2, 1, 6, "Third Party Insourcing",            LocalDate.of(2014,  5, 11)),
        new Episode(2, 1, 7, "Proof of Concept",                  LocalDate.of(2014,  5, 18)),
        new Episode(2, 1, 8, "Optimal Tip, to, Tip Efficiency",   LocalDate.of(2014,  6,  1)),
        new Episode(2, 2, 1, "Sand Hill Shuffle",                 LocalDate.of(2015,  4, 12)),
        new Episode(2, 2, 2, "Runaway Devaluation",               LocalDate.of(2015,  4, 19)),
        new Episode(2, 2, 3, "Bad Money",                         LocalDate.of(2015,  4, 26)),
        new Episode(2, 2, 4, "The Lady",                          LocalDate.of(2015,  5, 03)),
        new Episode(2, 2, 5, "Server Space",                      LocalDate.of(2015,  5, 10)),
        new Episode(2, 2, 6, "Homicide",                          LocalDate.of(2015,  5, 17)),
        new Episode(2, 2, 7, "Adult Content",                     LocalDate.of(2015,  5, 24)),
        new Episode(2, 2, 8, "White Hat/Black Hat",               LocalDate.of(2015,  5, 31)),
        new Episode(2, 2, 9, "Binding Arbitration",               LocalDate.of(2015,  6,  7)),
        new Episode(2, 2, 10, "Two Days of the Condor",           LocalDate.of(2015,  6, 14)),
        new Episode(2, 3, 1, "Founder Friendly",                  LocalDate.of(2016,  4, 24)),
        new Episode(2, 3, 2, "Two in the Box",                    LocalDate.of(2016,  5,  1)),
        new Episode(2, 3, 3, "Meinertzhagen's Haversack",         LocalDate.of(2016,  5,  8)),
        new Episode(2, 3, 4, "Maleant Data Systems Solutions",    LocalDate.of(2016,  5, 15)),
        new Episode(2, 3, 5, "The Empty Chair",                   LocalDate.of(2016,  5, 22)),
        new Episode(2, 3, 6, "Bachmanity Insanity",               LocalDate.of(2016,  5, 29)),
        new Episode(2, 3, 7, "To Build a Better Beta",            LocalDate.of(2016,  6,  5)),
        new Episode(2, 3, 8, "Bachman's Earnings Over, Ride",     LocalDate.of(2016,  6, 12)),
        new Episode(2, 3, 9, "Daily Active Users",                LocalDate.of(2016,  6, 19)),
        new Episode(2, 3, 10, "The Uptick",                       LocalDate.of(2016,  6, 26)),
        new Episode(2, 4, 1, "Success Failure",                   LocalDate.of(2017,  4, 23)),
        new Episode(2, 4, 2, "Terms of Service",                  LocalDate.of(2017,  4, 30)),
        new Episode(2, 4, 3, "Intellectual Property",             LocalDate.of(2017,  5,  7)),
        new Episode(2, 4, 4, "Teambuilding Exercise",             LocalDate.of(2017,  5, 14)),
        new Episode(2, 4, 5, "The Blood Boy",                     LocalDate.of(2017,  5, 21)),
        new Episode(2, 4, 6, "Customer Service",                  LocalDate.of(2017,  5, 28)),
        new Episode(2, 4, 7, "The Patent Troll",                  LocalDate.of(2017,  6,  4)),
        new Episode(2, 4, 8, "The Keenan Vortex",                 LocalDate.of(2017,  6, 11)),
        new Episode(2, 4, 9, "Hooli, Con",                        LocalDate.of(2017,  6, 18)),
        new Episode(2, 4, 10, "Server Error",                     LocalDate.of(2017,  6, 25)),
        new Episode(2, 5, 1, "Grow Fast or Die Slow",             LocalDate.of(2018,  3, 25)),
        new Episode(2, 5, 2, "Reorientation",                     LocalDate.of(2018,  4,  1)),
        new Episode(2, 5, 3, "Chief Operating Officer",           LocalDate.of(2018,  4,  8)),
        new Episode(2, 5, 4, "Tech Evangelist",                   LocalDate.of(2018,  4, 15)),
        new Episode(2, 5, 5, "Facial Recognition",                LocalDate.of(2018,  4, 22)),
        new Episode(2, 5, 6, "Artificial Emotional Intelligence", LocalDate.of(2018,  4, 29)),
        new Episode(2, 5, 7, "Initial Coin Offering",             LocalDate.of(2018,  5,  6)),
        new Episode(2, 5, 8, "Fifty, One Percent",                LocalDate.of(2018,  5, 13))
    );

    private TestExampleData() { }
}
