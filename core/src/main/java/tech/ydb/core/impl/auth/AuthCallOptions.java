package tech.ydb.core.impl.auth;

import java.util.concurrent.Executor;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.grpc.YdbCallCredentials;
import tech.ydb.core.impl.BaseGrpcTrasnsport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AuthCallOptions implements AutoCloseable {
    private final CallOptions callOptions;
    private final AuthIdentity authIdentity;

    public AuthCallOptions(
            BaseGrpcTrasnsport parent,
            EndpointRecord endpoint,
            ManagedChannelFactory channelFactory,
            AuthRpcProvider<? super GrpcAuthRpc> authProvider,
            Executor executor,
            long defaul) {
        CallOptions options = CallOptions.DEFAULT;
        if (authProvider != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(endpoint, parent, channelFactory);
            authIdentity = authProvider.createAuthIdentity(rpc);
            if (authIdentity != null) {
                options = options.withCallCredentials(new YdbCallCredentials(authIdentity));
            }
        } else {
            authIdentity = null;
        }
        if (executor != null && executor != MoreExecutors.directExecutor()) {
            options = options.withExecutor(executor);
        }

        this.callOptions = options;
    }

    @Override
    public void close() {
        if (authIdentity != null) {
            authIdentity.close();
        }
    }

    public CallOptions getCallOptions() {
        return callOptions;
    }
}
