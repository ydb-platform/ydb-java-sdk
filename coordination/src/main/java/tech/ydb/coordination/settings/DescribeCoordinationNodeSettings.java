package tech.ydb.coordination.settings;

import tech.ydb.core.settings.OperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class DescribeCoordinationNodeSettings extends OperationSettings {
    private DescribeCoordinationNodeSettings(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationBuilder<Builder> {
        @Override
        public DescribeCoordinationNodeSettings build() {
            return new DescribeCoordinationNodeSettings(this);
        }
    }
}
