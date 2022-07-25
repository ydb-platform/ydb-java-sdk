package tech.ydb.core.grpc;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Nikolay Perfilov
 */
public class GrpcRequestSettings {
    private final long deadlineAfter;
    private final EndpointInfo preferredEndpoint;

    private GrpcRequestSettings(Builder builder) {
        this.deadlineAfter = builder.getDeadlineAfter();
        this.preferredEndpoint = builder.getPreferredEndpoint();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getDeadlineAfter() {
        return deadlineAfter;
    }

    public EndpointInfo getPreferredEndpoint() {
        return preferredEndpoint;
    }

    @ParametersAreNonnullByDefault
    public static final class Builder {
        private long deadlineAfter = 0;
        private EndpointInfo preferredEndpoint = null;

        public Builder withDeadlineAfter(long deadlineAfter) {
            this.deadlineAfter = deadlineAfter;
            return this;
        }

        public Builder withPreferredEndpoint(EndpointInfo preferredEndpoint) {
            this.preferredEndpoint = preferredEndpoint;
            return this;
        }

        public long getDeadlineAfter() {
            return deadlineAfter;
        }

        public EndpointInfo getPreferredEndpoint() {
            return preferredEndpoint;
        }

        public GrpcRequestSettings build() {
            return new GrpcRequestSettings(this);
        }
    }
}
