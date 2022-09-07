package tech.ydb.core.grpc.impl;

import java.util.concurrent.Executor;

import tech.ydb.core.auth.AuthIdentity;
import tech.ydb.core.auth.AuthProvider;
import tech.ydb.core.auth.AuthRpc;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.grpc.YdbCallCredentials;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AuthCallOptions {
    private final EndpointRecord endpoint;
    private final BaseGrpcTrasnsport parent;
    private final ChannelFactory channelFactory;
    private final CallOptions callOptions;

    public AuthCallOptions(
            BaseGrpcTrasnsport parent,
            EndpointRecord endpoint,
            ChannelFactory channelFactory,
            AuthProvider authProvider,
            Executor executor) {
        this.endpoint = endpoint;
        this.parent = parent;
        this.channelFactory = channelFactory;

        CallOptions options = CallOptions.DEFAULT;
        if (authProvider != null) {
            AuthIdentity identity = authProvider.createAuthIdentity(new YdbAuthRpc());
            if (identity != null) {
                options = options.withCallCredentials(new YdbCallCredentials(identity));
            }
        }
        if (executor != null && executor != MoreExecutors.directExecutor()) {
            options = options.withExecutor(executor);
        }
        
        this.callOptions = options;
    }

    public CallOptions getCallOptions() {
        return callOptions;
    }

    private class YdbAuthRpc implements AuthRpc {
        @Override
        public String getEndpoint() {
            return endpoint.toString();
        }

        @Override
        public String getDatabase() {
            return parent.getDatabase();
        }

        @Override
        public GrpcTransport createTransport() {
            // For auth provider we use transport without auth (with default CallOptions)
            return new SingleChannelTransport(
                    CallOptions.DEFAULT,
                    parent.getDefaultReadTimeoutMillis(),
                    parent.getDatabase(),
                    endpoint,
                    channelFactory
            );
        }
    }
}
