package tech.ydb.coordination.settings;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

public class SessionSettings {

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
        this.coordinationNodeName = builder.coordinationName;
        this.sessionNum = builder.sessionNum;
        this.description = builder.description;
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

        private String coordinationName = "coordination-node-default";

        private int sessionNum = 1;
        private String description = "";

        public void setCoordinationName(@Nonnull String coordinationName) {
            this.coordinationName = Preconditions.checkNotNull(
                    coordinationName,
                    "Coordination node name  shouldn’t be null!"
            );
        }

        public void setSessionNum(int sessionNum) {
            this.sessionNum = sessionNum;
        }

        public void setDescription(@Nonnull String description) {
            this.description = Preconditions.checkNotNull(
                    description,
                    "Descriptions shouldn’t be null!"
            );
        }

        public SessionSettings build() {
            return new SessionSettings(this);
        }
    }
}
