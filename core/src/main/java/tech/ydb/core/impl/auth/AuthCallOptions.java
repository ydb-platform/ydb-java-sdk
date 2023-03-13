package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.impl.BaseGrpcTrasnsport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AuthCallOptions implements AutoCloseable {
    private final AuthIdentity authIdentity;
    private final CallOptions callOptions;

    public AuthCallOptions() {
        this.authIdentity = null;
        this.callOptions = CallOptions.DEFAULT;
    }

    public AuthCallOptions(
            BaseGrpcTrasnsport parent,
            List<EndpointRecord> endpoints,
            ManagedChannelFactory channelFactory,
            AuthRpcProvider<? super GrpcAuthRpc> authProvider,
            long readTimeoutMillis,
            Executor callExecutor) {

        CallOptions options = CallOptions.DEFAULT;

        if (authProvider != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(endpoints, parent, channelFactory);
            authIdentity = authProvider.createAuthIdentity(rpc);
        } else {
            authIdentity = null;
        }

        if (authIdentity != null) {
            options = options.withCallCredentials(new YdbCallCredentials(authIdentity));
        }

        if (readTimeoutMillis > 0) {
            options = options.withDeadlineAfter(readTimeoutMillis, TimeUnit.MILLISECONDS);
        }
        if (callExecutor != null && callExecutor != MoreExecutors.directExecutor()) {
            options = options.withExecutor(callExecutor);
        }

        this.callOptions = options;
    }

    @Override
    public void close() {
        if (authIdentity != null) {
            authIdentity.close();
        }
    }

    public String getToken() {
        if (authIdentity != null) {
            return authIdentity.getToken();
        }
        return null;
    }

    public CallOptions getGrpcCallOptions() {
        return callOptions;
    }
}
