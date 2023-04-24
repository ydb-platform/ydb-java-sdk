package tech.ydb.coordination.settings;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

public class SessionSettings {
    public static final int START_SESSION_ID = 0;

    private final int coordinationNodeNum;
    private final String coordinationNodeName;

    /**
     * Used for creating coordination node name.
     * <p>
     * Example: leader-election-$sessionNum -> leader-election-1, sessionNum = 1.
     */
    private final int sessionNum;

    /**
     * Text description of the session is displayed in the internal interfaces and
     * can be useful when diagnosing problems.
     */
    private final String description;

    public SessionSettings(
            Builder builder
    ) {
        this.coordinationNodeNum = builder.coordinationNodeNum;
        this.coordinationNodeName = builder.coordinationNodeName;
        this.sessionNum = builder.sessionNum;
        this.description = builder.description;
    }

    public int getCoordinationNodeNum() {
        return coordinationNodeNum;
    }

    public String getCoordinationNodeName() {
        return coordinationNodeName;
    }

    public int getSessionNum() {
        return sessionNum;
    }

    public String getDescription() {
        return description;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private int coordinationNodeNum = 1;
        private String coordinationNodeName = "coordination-node-default";
        private int sessionNum = 1;
        private String description = "";

        public Builder setCoordinationNum(int coordinationNodeNum) {
            this.coordinationNodeNum = coordinationNodeNum;

            return this;
        }

        public Builder setCoordinationNodeName(@Nonnull String coordinationNodeName) {
            this.coordinationNodeName = Preconditions.checkNotNull(
                    coordinationNodeName,
                    "Coordination node name  shouldn’t be null!"
            );

            return this;
        }

        public Builder setSessionNum(int sessionNum) {
            this.sessionNum = sessionNum;

            return this;
        }

        public Builder setDescription(@Nonnull String description) {
            this.description = Preconditions.checkNotNull(
                    description,
                    "Descriptions shouldn’t be null!"
            );

            return this;
        }

        public SessionSettings build() {
            return new SessionSettings(this);
        }
    }
}
