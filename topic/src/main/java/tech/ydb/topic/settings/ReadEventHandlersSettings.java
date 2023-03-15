package tech.ydb.topic.settings;

import java.util.concurrent.Executor;

import tech.ydb.topic.read.events.ReadEventHandler;

/**
 * @author Nikolay Perfilov
 */
public class ReadEventHandlersSettings {
    private final Executor executor;
    private final ReadEventHandler eventHandler;

    private ReadEventHandlersSettings(Builder builder) {
        this.executor = builder.executor;
        this.eventHandler = builder.eventHandler;
    }

    public Executor getExecutor() {
        return executor;
    }

    public ReadEventHandler getEventHandler() {
        return eventHandler;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private Executor executor;
        private ReadEventHandler eventHandler;

        public Builder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setEventHandler(ReadEventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public ReadEventHandlersSettings build() {
            return new ReadEventHandlersSettings(this);
        }

    }
}
