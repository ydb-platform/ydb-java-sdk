package tech.ydb.query.settings;

import tech.ydb.core.settings.BaseRequestSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AttachSessionSettings extends BaseRequestSettings {
    private AttachSessionSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends BaseBuilder<Builder> {
        @Override
        public AttachSessionSettings build() {
            return new AttachSessionSettings(this);
        }
    }
}
