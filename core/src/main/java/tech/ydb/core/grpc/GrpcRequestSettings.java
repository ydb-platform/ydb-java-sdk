package tech.ydb.core.grpc;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import io.grpc.Metadata;

/**
 * @author Nikolay Perfilov
 */
public class GrpcRequestSettings {
    private final long deadlineAfter;
    private final EndpointInfo preferredEndpoint;
    private final Metadata extraHeaders;
    private final Consumer<Metadata> trailersHandler;

    private GrpcRequestSettings(Builder builder) {
        this.deadlineAfter = builder.getDeadlineAfter();
        this.preferredEndpoint = builder.getPreferredEndpoint();
        this.extraHeaders = builder.getExtraHeaders();
        this.trailersHandler = builder.getTrailersHandler();
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

    public Metadata getExtraHeaders() {
        return extraHeaders;
    }

    public Consumer<Metadata> getTrailersHandler() {
        return trailersHandler;
    }

    @ParametersAreNonnullByDefault
    public static final class Builder {
        private long deadlineAfter = 0;
        private EndpointInfo preferredEndpoint = null;
        private Metadata extraHeaders = null;
        private Consumer<Metadata> trailersHandler = null;

        public Builder withDeadlineAfter(long deadlineAfter) {
            this.deadlineAfter = deadlineAfter;
            return this;
        }

        public Builder withPreferredEndpoint(EndpointInfo preferredEndpoint) {
            this.preferredEndpoint = preferredEndpoint;
            return this;
        }

        public Builder withExtraHeaders(Metadata headers) {
            this.extraHeaders = headers;
            return this;
        }

        public Builder withTrailersHandler(Consumer<Metadata> handler) {
            this.trailersHandler = handler;
            return this;
        }

        public long getDeadlineAfter() {
            return deadlineAfter;
        }

        public EndpointInfo getPreferredEndpoint() {
            return preferredEndpoint;
        }

        public Metadata getExtraHeaders() {
            return extraHeaders;
        }

        public Consumer<Metadata> getTrailersHandler() {
            return trailersHandler;
        }

        public GrpcRequestSettings build() {
            return new GrpcRequestSettings(this);
        }
    }
}
