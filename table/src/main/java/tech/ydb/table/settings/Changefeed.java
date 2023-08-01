package tech.ydb.table.settings;

import java.time.Duration;
import java.util.Objects;

import tech.ydb.proto.table.YdbTable;

/**
 * @author Egor Litvinenko
 */
public class Changefeed {
    public enum Mode {
        KEYS_ONLY(YdbTable.ChangefeedMode.Mode.MODE_KEYS_ONLY),
        UPDATES(YdbTable.ChangefeedMode.Mode.MODE_UPDATES),
        NEW_IMAGE(YdbTable.ChangefeedMode.Mode.MODE_NEW_IMAGE),
        OLD_IMAGE(YdbTable.ChangefeedMode.Mode.MODE_OLD_IMAGE),
        NEW_AND_OLD_IMAGES(YdbTable.ChangefeedMode.Mode.MODE_NEW_AND_OLD_IMAGES);

        private final YdbTable.ChangefeedMode.Mode proto;

        Mode(YdbTable.ChangefeedMode.Mode proto) {
            this.proto = proto;
        }

        YdbTable.ChangefeedMode.Mode toPb() {
            return proto;
        }
    }

    public enum Format {
        JSON(YdbTable.ChangefeedFormat.Format.FORMAT_JSON);

        private final YdbTable.ChangefeedFormat.Format proto;

        Format(YdbTable.ChangefeedFormat.Format proto) {
            this.proto = proto;
        }

        YdbTable.ChangefeedFormat.Format toProto() {
            return proto;
        }
    }

    private final String name;
    private final Mode mode;
    private final Format format;
    private final boolean virtualTimestamps;
    private final Duration retentionPeriod;
    private final boolean initialScan;

    private Changefeed(Builder builder) {
        this.name = builder.name;
        this.mode = builder.mode;
        this.format = builder.format;
        this.virtualTimestamps = builder.virtualTimestamps;
        this.retentionPeriod = builder.retentionPeriod;
        this.initialScan = builder.initialScan;
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

    public YdbTable.Changefeed toProto() {
        YdbTable.Changefeed.Builder builder = YdbTable.Changefeed.newBuilder()
                .setName(name)
                .setFormat(format.toProto())
                .setVirtualTimestamps(virtualTimestamps)
                .setInitialScan(initialScan)
                .setMode(mode.toPb());

        if (retentionPeriod != null) {
            builder.setRetentionPeriod(com.google.protobuf.Duration.newBuilder()
                    .setSeconds(retentionPeriod.getSeconds())
                    .setNanos(retentionPeriod.getNano())
                    .build());
        }

        return builder.build();
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

        public Changefeed build() {
            return new Changefeed(this);
        }
    }
}
