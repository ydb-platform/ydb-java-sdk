package tech.ydb.core.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationSettings extends BaseRequestSettings {
    private final Duration operationTimeout;
    private final Duration cancelTimeout;
    private final Boolean reportCostInfo;

    protected OperationSettings(OperationBuilder<?> builder) {
        super(builder);
        this.operationTimeout = builder.operationTimeout;
        this.cancelTimeout = builder.cancelTimeout;
        this.reportCostInfo = builder.reportCostInfo;
    }

    public Duration getOperationTimeout() {
        return operationTimeout;
    }

    public Duration getCancelTimeout() {
        return cancelTimeout;
    }

    public Boolean getReportCostInfo() {
        return reportCostInfo;
    }

    public static class OperationBuilder<Self extends OperationBuilder<?>> extends BaseBuilder<Self> {
        private Duration operationTimeout = null;
        private Duration cancelTimeout = null;
        private Boolean reportCostInfo = null;

        public Self withOperationTimeout(Duration duration) {
            this.operationTimeout = duration;
            return self();
        }

        public Self withOperationTimeout(long duration, TimeUnit unit) {
            this.operationTimeout = Duration.ofNanos(unit.toNanos(duration));
            return self();
        }

        public Self withCancelTimeout(Duration duration) {
            this.cancelTimeout = duration;
            return self();
        }

        public Self withCancelTimeout(long duration, TimeUnit unit) {
            this.cancelTimeout = Duration.ofNanos(unit.toNanos(duration));
            return self();
        }

        public Self withReportCostInfo(Boolean report) {
            this.reportCostInfo = report;
            return self();
        }

        @Override
        public OperationSettings build() {
            return new OperationSettings(this);
        }
    }
}
