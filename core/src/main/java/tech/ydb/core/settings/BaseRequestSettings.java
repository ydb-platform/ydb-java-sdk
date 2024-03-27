package tech.ydb.core.settings;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 * @author Aleksandr Gorshenin
 */
public class BaseRequestSettings {
    private final String traceId;
    private final Duration requestTimeout;

    protected BaseRequestSettings(BaseBuilder<?> builder) {
        this.traceId = builder.traceId;
        this.requestTimeout = builder.requestTimeout;
    }

    public String getTraceId() {
        return traceId;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public static class BaseBuilder<Self extends BaseBuilder<?>> {
        private String traceId = null;
        private Duration requestTimeout = null;

        @SuppressWarnings("unchecked")
        protected Self self() {
            return (Self) this;
        }

        public Self withRequestTimeout(Duration duration) {
            this.requestTimeout = duration;
            return self();
        }

        public Self withRequestTimeout(long duration, TimeUnit unit) {
            this.requestTimeout = Duration.ofNanos(unit.toNanos(duration));
            return self();
        }

        /**
         * Set request trace id. Used for debug purposes.
         * If not set explicitly, random UUID will be generated
         *
         * @param traceId request trace id
         * @return Builder
         */
        public Self withTraceId(String traceId) {
            this.traceId = traceId;
            return self();
        }

        public BaseRequestSettings build() {
            return new BaseRequestSettings(this);
        }
    }
}
