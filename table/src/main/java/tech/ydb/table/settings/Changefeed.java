package tech.ydb.table.settings;

import java.time.Duration;
import java.util.Objects;

import tech.ydb.table.description.ChangefeedDescription;
import tech.ydb.table.impl.BaseSession;


/**
 * @author Egor Litvinenko
 */
public class Changefeed {
    public enum Mode {
        KEYS_ONLY,
        UPDATES,
        NEW_IMAGE,
        OLD_IMAGE,
        NEW_AND_OLD_IMAGES;

        @Deprecated
        public tech.ydb.proto.table.YdbTable.ChangefeedMode.Mode toPb() {
            return BaseSession.buildChangefeedMode(this);
        }
    }

    public enum Format {
        /** Change record in JSON format for common (row oriented) tables */
        JSON,
        /** Change record in JSON format for document (DynamoDB-compatible) tables */
        DYNAMODB_STREAMS_JSON,
        /** Debezium-like change record JSON format for common (row oriented) tables */
        DEBEZIUM_JSON;

        @Deprecated
        public tech.ydb.proto.table.YdbTable.ChangefeedFormat.Format toProto() {
            return BaseSession.buildChangefeedFormat(this);
        }
    }

    private final String name;
    private final Mode mode;
    private final Format format;
    private final boolean virtualTimestamps;
    private final Duration retentionPeriod;
    private final boolean initialScan;
    private final Duration resolvedTimestampsInterval;

    private Changefeed(Builder builder) {
        this.name = builder.name;
        this.mode = builder.mode;
        this.format = builder.format;
        this.virtualTimestamps = builder.virtualTimestamps;
        this.retentionPeriod = builder.retentionPeriod;
        this.initialScan = builder.initialScan;
        this.resolvedTimestampsInterval = builder.resolvedTimestampsInterval;
    }

    public String getName() {
        return name;
    }

    public Mode getMode() {
        return mode;
    }

    public Format getFormat() {
        return format;
    }

    public boolean hasVirtualTimestamps() {
        return virtualTimestamps;
    }

    public boolean hasInitialScan() {
        return initialScan;
    }

    public Duration getRetentionPeriod() {
        return retentionPeriod;
    }

    public Duration getResolvedTimestampsInterval() {
        return resolvedTimestampsInterval;
    }

    @Deprecated
    public tech.ydb.proto.table.YdbTable.Changefeed toProto() {
        return BaseSession.buildChangefeed(this);
    }

    public static Builder fromDescription(ChangefeedDescription description) {
        return new Builder(description);
    }

    public static Builder newBuilder(String changefeedName) {
        return new Builder(changefeedName);
    }

    public static class Builder {
        private final String name;
        private Mode mode = Mode.KEYS_ONLY;
        private Format format = Format.JSON;
        private boolean virtualTimestamps = false;
        private Duration retentionPeriod = null;
        private boolean initialScan = false;
        private Duration resolvedTimestampsInterval = null;

        private Builder(ChangefeedDescription description) {
            this.name = description.getName();
            this.mode = description.getMode();
            this.format = description.getFormat();
            this.virtualTimestamps = description.hasVirtualTimestamps();
            this.resolvedTimestampsInterval = description.getResolvedTimestampsInterval();
        }

        private Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public Builder withMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder withFormat(Format format) {
            this.format = format;
            return this;
        }

        public Builder withVirtualTimestamps(boolean value) {
            this.virtualTimestamps = value;
            return this;
        }

        public Builder withInitialScan(boolean value) {
            this.initialScan = value;
            return this;
        }

        public Builder withRetentionPeriod(Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        public Builder withResolvedTimestampsInterval(Duration resolvedTimestampsInterval) {
            this.resolvedTimestampsInterval = resolvedTimestampsInterval;
            return this;
        }

        public Changefeed build() {
            return new Changefeed(this);
        }
    }
}
