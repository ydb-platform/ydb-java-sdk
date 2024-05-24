package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.grpc.Metadata;

/**
 * @author Nikolay Perfilov
 */
public class GrpcRequestSettings {
    private final long deadlineAfter;
    private final int preferredNodeID;
    private final String traceId;
    private final List<String> clientCapabilities;
    private final Consumer<Metadata> trailersHandler;

    private GrpcRequestSettings(Builder builder) {
        this.deadlineAfter = builder.deadlineAfter;
        this.preferredNodeID = builder.preferredNodeID;
        this.traceId = builder.traceId;
        this.clientCapabilities = builder.clientCapabilities;
        this.trailersHandler = builder.trailersHandler;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getDeadlineAfter() {
        return deadlineAfter;
    }

    public int getPreferredNodeID() {
        return preferredNodeID;
    }

    public String getTraceId() {
        return traceId;
    }

    public List<String> getClientCapabilities() {
        return clientCapabilities;
    }

    public Consumer<Metadata> getTrailersHandler() {
        return trailersHandler;
    }

    public static final class Builder {
        private long deadlineAfter = 0L;
        private int preferredNodeID = 0;
        private String traceId = null;
        private List<String> clientCapabilities = null;
        private Consumer<Metadata> trailersHandler = null;

        /**
         * Returns a new {@code Builder} with a deadline, based on the running Java Virtual Machine's
         * high-resolution time source {@link System#nanoTime() }
         * If the value is null or negative, then the default
         * {@link GrpcTransportBuilder#withReadTimeout(java.time.Duration)} will be used.
         *
         * @param deadlineAfter the value of the JVM time source, when request will be cancelled, in nanoseconds
         * @return {@code Builder} with a deadline
         */
        public Builder withDeadlineAfter(long deadlineAfter) {
            this.deadlineAfter = deadlineAfter;
            return this;
        }

        /**
         * Returns a new {@code Builder} with a deadline. Specified duration will be converted to the value of JVM
         * high-resolution time source
         *
         * @param duration the deadline duration
         * @return {@code Builder} with a deadline
         */
        public Builder withDeadline(Duration duration) {
            if (duration != null && !duration.isZero()) {
                this.deadlineAfter = System.nanoTime() + duration.toNanos();
            } else {
                this.deadlineAfter = 0L;
            }
            return this;
        }

        public Builder withPreferredNodeID(Integer preferredNodeID) {
            this.preferredNodeID = preferredNodeID;
            return this;
        }

        public Builder withTraceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder withClientCapabilities(List<String> clientCapabilities) {
            this.clientCapabilities = clientCapabilities;
            return this;
        }

        public Builder addClientCapability(String clientCapability) {
            if (this.clientCapabilities == null) {
                this.clientCapabilities = new ArrayList<>();
            }
            this.clientCapabilities.add(clientCapability);
            return this;
        }

        public Builder withTrailersHandler(Consumer<Metadata> handler) {
            this.trailersHandler = handler;
            return this;
        }

        public GrpcRequestSettings build() {
            return new GrpcRequestSettings(this);
        }
    }
}
