package tech.ydb.coordination.settings;

import tech.ydb.core.settings.OperationSettings;

public class DescribeCoordinationNodeSettings extends OperationSettings {
    private DescribeCoordinationNodeSettings(OperationBuilder<?> builder) {
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
