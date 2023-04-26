package tech.ydb.coordination.settings;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

public class ScenarioSettings {

    public static final int START_SESSION_ID = 0;
    public static final int SESSION_KEEP_ALIVE_TIMEOUT_MS = 0;

    private final String coordinationNodeName;

    /**
     * Used for creating semaphore name.
     */
    private final String semaphoreName;

    /**
     * Text description of the session is displayed in the internal interfaces and
     * can be useful when diagnosing problems.
     */
    private final String description;

    public ScenarioSettings(
            Builder builder
    ) {
        this.coordinationNodeName = builder.coordinationNodeName;
        this.semaphoreName = builder.semaphoreName;
        this.description = builder.description;
    }

    public String getCoordinationNodeName() {
        return coordinationNodeName;
    }

    public String getSemaphoreName() {
        return semaphoreName;
    }

    public String getDescription() {
        return description;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String coordinationNodeName = "coordination-node-default";
        private String semaphoreName = "semaphore-default";
        private String description = "";

        public Builder setCoordinationNodeName(@Nonnull String coordinationNodeName) {
            this.coordinationNodeName = Preconditions.checkNotNull(
                    coordinationNodeName,
                    "Coordination node name shouldn’t be null!"
            );

            return this;
        }

        public Builder setSemaphoreName(@Nonnull String semaphoreName) {
            this.semaphoreName = Preconditions.checkNotNull(
                    semaphoreName,
            "Session semaphore name shouldn't be null!"
            );

            return this;
        }

        public Builder setDescription(@Nonnull String description) {
            this.description = Preconditions.checkNotNull(
                    description,
                    "Descriptions shouldn’t be null!"
            );

            return this;
        }

        public ScenarioSettings build() {
            return new ScenarioSettings(this);
        }
    }
}
