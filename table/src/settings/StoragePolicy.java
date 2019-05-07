package ru.yandex.ydb.table.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * @author Sergey Polovko
 */
public class StoragePolicy {

    @Nullable
    private String presetName;
    @Nullable
    private String sysLog;
    @Nullable
    private String log;
    @Nullable
    private String data;
    @Nullable
    private String external;

    @Nullable
    public String getPresetName() {
        return presetName;
    }

    public StoragePolicy setPresetName(@Nonnull String presetName) {
        this.presetName = presetName;
        return this;
    }

    @Nullable
    public String getSysLog() {
        return sysLog;
    }

    public StoragePolicy setSysLog(@Nonnull String sysLog) {
        this.sysLog = sysLog;
        return this;
    }

    @Nullable
    public String getLog() {
        return log;
    }

    public StoragePolicy setLog(@Nonnull String log) {
        this.log = log;
        return this;
    }

    @Nullable
    public String getData() {
        return data;
    }

    public StoragePolicy setData(@Nonnull String data) {
        this.data = data;
        return this;
    }

    @Nullable
    public String getExternal() {
        return external;
    }

    public StoragePolicy setExternal(@Nonnull String external) {
        this.external = external;
        return this;
    }
}
