package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.impl.BaseGrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CallOptionsFactory implements AutoCloseable {
    private final AuthIdentity authIdentity;

    public CallOptionsFactory(
            BaseGrpcTransport parent,
            List<EndpointRecord> endpoints,
            ManagedChannelFactory channelFactory,
            AuthRpcProvider<? super GrpcAuthRpc> authProvider) {
        if (authProvider != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(endpoints, parent, channelFactory);
            authIdentity = authProvider.createAuthIdentity(rpc);
        } else {
            authIdentity = null;
        }
    }

    @Override
    public void close() {
        if (authIdentity != null) {
            authIdentity.close();
        }
    }

    public CallOptions createCallOptions(long defaultReadTimeoutMs, Executor executor) {
        CallOptions options = CallOptions.DEFAULT;

        if (authIdentity != null) {
            options = options.withCallCredentials(new YdbCallCredentials(authIdentity));
        }
        if (defaultReadTimeoutMs > 0) {
            options = options.withDeadlineAfter(defaultReadTimeoutMs, TimeUnit.MILLISECONDS);
        }
        if (executor != null && executor != MoreExecutors.directExecutor()) {
            options = options.withExecutor(executor);
        }

        return options;
    }
}
