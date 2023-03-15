package tech.ydb.table.impl;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.SessionSupplier;
import tech.ydb.table.rpc.TableRpc;
import tech.ydb.table.settings.CreateSessionSettings;
import tech.ydb.table.settings.DeleteSessionSettings;


/**
 * @author Aleksandr Gorshenin
 */
public class SimpleTableClient implements SessionSupplier {
    private final TableRpc tableRpc;
    private final boolean keepQueryText;

    SimpleTableClient(Builder builder) {
        this.tableRpc = builder.tableRpc;
        this.keepQueryText = builder.keepQueryText;
    }

    @Override
    public CompletableFuture<Result<Session>> createSession(Duration duration) {
        CreateSessionSettings settings = new CreateSessionSettings()
                .setTimeout(duration);
        return BaseSession.createSessionId(tableRpc, settings, false)
                .thenApply(response -> response.map(SimpleSession::new));
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return tableRpc.getScheduler();
    }

    public static Builder newClient(TableRpc rpc) {
        return new Builder(rpc);
    }

    public static class Builder {
        private final TableRpc tableRpc;
        private boolean keepQueryText = true;

        public Builder(TableRpc tableRpc) {
            this.tableRpc = tableRpc;
        }

        public Builder keepQueryText(boolean keep) {
            this.keepQueryText = keep;
            return this;
        }

        public SimpleTableClient build() {
            return new SimpleTableClient(this);
        }
    }

    private class SimpleSession extends BaseSession {
        SimpleSession(String id) {
            super(id, tableRpc, keepQueryText);
        }

        @Override
        protected void updateSessionState(Throwable th, StatusCode code, boolean shutdownHint) {
            // Nothing
        }

        @Override
        public void close() {
            delete(new DeleteSessionSettings()).join();
        }
    }
}
