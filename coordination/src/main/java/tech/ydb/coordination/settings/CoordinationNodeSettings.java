package tech.ydb.coordination.settings;

import tech.ydb.coordination.description.NodeConfig;
import tech.ydb.core.settings.OperationSettings;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationNodeSettings extends OperationSettings {
    private final NodeConfig config;

    private CoordinationNodeSettings(Builder builder) {
        super(builder);
        this.config = builder.config;
    }

    public NodeConfig getConfig() {
        return this.config;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OperationSettings.OperationBuilder<Builder> {
        private NodeConfig config = NodeConfig.create();

        public Builder withNodeConfig(NodeConfig config) {
            this.config = config;
            return this;
        }

        @Override
        public CoordinationNodeSettings build() {
            return new CoordinationNodeSettings(this);
        }
    }
}
