package tech.ydb.coordination.recipes.locks;

public class ReadWriteInterProcessLockSettings {
    private final boolean waitConnection;

    public ReadWriteInterProcessLockSettings(Builder builder) {
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

        public ReadWriteInterProcessLockSettings build() {
            return new ReadWriteInterProcessLockSettings(this);
        }
    }
}

