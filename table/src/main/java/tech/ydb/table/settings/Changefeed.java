package tech.ydb.table.settings;

import java.util.Objects;

import tech.ydb.table.YdbTable;

/**
 * @author Egor Litvinenko
 */
public class Changefeed {

    private final String name;
    private final Mode mode;
    private final Format format;

    public Changefeed(String name, Mode mode, Format format) {
        this.name = Objects.requireNonNull(name);
        this.mode = Objects.requireNonNull(mode);
        this.format = Objects.requireNonNull(format);
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

    public YdbTable.Changefeed toProto() {
        return YdbTable.Changefeed.newBuilder()
                .setName(name)
                .setFormat(format.toProto())
                .setMode(mode.toProto())
                .build();
    }

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

        YdbTable.ChangefeedMode.Mode toProto() {
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

}
