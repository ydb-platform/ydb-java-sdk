package tech.ydb.coordination.recipes.locks;

// TODO: More settings
public class InterProcessMutexSettings {
    private final boolean waitConnection;

    public InterProcessMutexSettings(Builder builder) {
        this.waitConnection = builder.waitConnection;
    }

    public boolean isWaitConnection() {
        return waitConnection;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private boolean waitConnection = false;

        public Builder withWaitConnection(boolean waitConnection) {
            this.waitConnection = waitConnection;
            return this;
        }

        public InterProcessMutexSettings build() {
            return new InterProcessMutexSettings(this);
        }
    }
}
