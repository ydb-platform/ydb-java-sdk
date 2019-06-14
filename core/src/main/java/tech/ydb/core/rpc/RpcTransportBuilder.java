package tech.ydb.core.rpc;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.NopAuthProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;


/**
 * @author Sergey Polovko
 */
public abstract class RpcTransportBuilder<T extends RpcTransport, B extends RpcTransportBuilder<T, B>> {

    private Executor callExecutor = MoreExecutors.directExecutor();
    private AuthProvider authProvider = NopAuthProvider.INSTANCE;
    private long readTimeoutMillis = 0;

    public B withAuthProvider(AuthProvider authProvider) {
        this.authProvider = requireNonNull(authProvider);
        return self();
    }

    public B withReadTimeout(Duration timeout) {
        this.readTimeoutMillis = timeout.toMillis();
        checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return self();
    }

    public B withReadTimeout(long timeout, TimeUnit unit) {
        this.readTimeoutMillis = unit.toMillis(timeout);
        checkArgument(readTimeoutMillis > 0, "readTimeoutMillis must be greater than 0");
        return self();
    }

    public B withCallExecutor(Executor executor) {
        this.callExecutor = requireNonNull(executor);
        return self();
    }

    public Executor getCallExecutor() {
        return callExecutor;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public abstract T build();

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }
}
