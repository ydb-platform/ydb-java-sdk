package tech.ydb.coordination.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class DropCoordinationNodeSettings extends OperationSettings {

    private DropCoordinationNodeSettings(OperationBuilder<?> builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public DropCoordinationNodeSettings build() {
            return new DropCoordinationNodeSettings(this);
        }
    }
}
