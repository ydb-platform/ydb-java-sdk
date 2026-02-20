package tech.ydb.core.grpc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import io.grpc.Metadata;

import tech.ydb.core.impl.call.GrpcFlows;
import tech.ydb.core.tracing.NoopTracer;
import tech.ydb.core.tracing.Span;

/**
 * @author Nikolay Perfilov
 */
public class GrpcRequestSettings {
    private final long deadlineAfter;
    private final Integer preferredNodeID;
    private final boolean directMode;
    private final boolean prefferReadyChannel;
    private final boolean deadlineDisabled;
    private final String traceId;
    private final List<String> clientCapabilities;
    private final Consumer<Metadata> trailersHandler;
    private final BooleanSupplier pessimizationHook;
    private final GrpcFlowControl flowControl;
    private final Span span;

    private GrpcRequestSettings(Builder builder) {
        this.deadlineAfter = builder.deadlineAfter;
        this.preferredNodeID = builder.preferredNodeID;
        this.directMode = builder.directMode;
        this.prefferReadyChannel = builder.preferReadyChannel;
        this.deadlineDisabled = builder.deadlineDisabled;
        this.traceId = builder.traceId;
        this.clientCapabilities = builder.clientCapabilities;
        this.trailersHandler = builder.trailersHandler;
        this.pessimizationHook = builder.pessimizationHook;
        this.flowControl = builder.flowControl;
        this.span = builder.span;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public long getDeadlineAfter() {
        return deadlineAfter;
    }

    public boolean isDeadlineDisabled() {
        return deadlineDisabled;
    }

    public Integer getPreferredNodeID() {
        return preferredNodeID;
    }

    public boolean isDirectMode() {
        return directMode;
    }

    public boolean isPreferReadyChannel() {
        return prefferReadyChannel;
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

    public BooleanSupplier getPessimizationHook() {
        return pessimizationHook;
    }

    public GrpcFlowControl getFlowControl() {
        return flowControl;
    }

    public Span getSpan() {
        return span;
    }

    public static final class Builder {
        private long deadlineAfter = 0L;
        private boolean preferReadyChannel = false;
        private boolean deadlineDisabled = false;
        private Integer preferredNodeID = null;
        private boolean directMode = false;
        private String traceId = null;
        private List<String> clientCapabilities = null;
        private Consumer<Metadata> trailersHandler = null;
        private BooleanSupplier pessimizationHook = null;
        private GrpcFlowControl flowControl = GrpcFlows.SIMPLE_FLOW;
        private Span span = NoopTracer;

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
            this.deadlineDisabled = false;
            return this;
        }

        /**
         * Returns a new {@code Builder} with a deadline. Specified duration will be converted to the value of JVM
         * high-resolution time source
         * @param duration the deadline duration
         * @return {@code Builder} with a deadline
         */
        public Builder withDeadline(Duration duration) {
            if (duration != null && !duration.isZero()) {
                this.deadlineAfter = System.nanoTime() + duration.toNanos();
            } else {
                this.deadlineAfter = 0L;
            }
            this.deadlineDisabled = false;
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

        public Builder withFlowControl(GrpcFlowControl flowCtrl) {
            this.flowControl = flowCtrl;
            return this;
        }

        public Builder withDirectMode(boolean directMode) {
            this.directMode = directMode;
            return this;
        }

        public Builder withPreferReadyChannel(boolean preferReady) {
            this.preferReadyChannel = preferReady;
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

        public Builder withPessimizationHook(BooleanSupplier pessimizationHook) {
            this.pessimizationHook = pessimizationHook;
            return this;
        }

        public Builder withSpan(Span span) {
            this.span = span;
            return this;
        }

        public Builder disableDeadline() {
            this.deadlineDisabled = true;
            return this;
        }

        public GrpcRequestSettings build() {
            return new GrpcRequestSettings(this);
        }
    }
}
