package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.grpc.GrpcCompression;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.impl.pool.ManagedChannelFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AuthCallOptions implements AutoCloseable {
    private final AuthIdentity authIdentity;
    private final CallOptions callOptions;
    private final long readTimeoutMillis;

    public AuthCallOptions() {
        this.authIdentity = null;
        this.callOptions = CallOptions.DEFAULT;
        this.readTimeoutMillis = 0;
    }
    private AuthCallOptions(Builder builder) {
        CallOptions options = CallOptions.DEFAULT;

        if (builder.authProvider != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(builder.endpoints, builder.scheduler, builder.database,
                    builder.channelFactory);
            authIdentity = builder.authProvider.createAuthIdentity(rpc);
        } else {
            authIdentity = null;
        }

        if (authIdentity != null) {
            options = options.withCallCredentials(new YdbCallCredentials(authIdentity));
        }

        if (builder.callExecutor != null && builder.callExecutor != MoreExecutors.directExecutor()) {
            options = options.withExecutor(builder.callExecutor);
        }
        if (builder.compression != GrpcCompression.NO_COMPRESSION) {
            options = options.withCompression(builder.compression.compressor());
        }

        this.callOptions = options;
        this.readTimeoutMillis = builder.readTimeoutMillis;
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
        if (readTimeoutMillis > 0) {
            return callOptions.withDeadlineAfter(readTimeoutMillis, TimeUnit.MILLISECONDS);
        }
        return callOptions;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * BUILDER
     */
    public static class Builder {
        private ScheduledExecutorService scheduler;
        private String database;
        private List<EndpointRecord> endpoints;
        private ManagedChannelFactory channelFactory;
        private AuthRpcProvider<? super GrpcAuthRpc> authProvider;
        private long readTimeoutMillis;
        private Executor callExecutor;
        private GrpcCompression compression;

        public Builder setScheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder setDatabase(String database) {
            this.database = database;
            return this;
        }

        public Builder setEndpoints(List<EndpointRecord> endpoints) {
            this.endpoints = endpoints;
            return this;
        }

        public Builder setChannelFactory(ManagedChannelFactory channelFactory) {
            this.channelFactory = channelFactory;
            return this;
        }

        public Builder setAuthProvider(AuthRpcProvider<? super GrpcAuthRpc> authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder setReadTimeoutMillis(long readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
            return this;
        }

        public Builder setCallExecutor(Executor callExecutor) {
            this.callExecutor = callExecutor;
            return this;
        }

        public Builder setCompression(GrpcCompression compression) {
            this.compression = compression;
            return this;
        }

        public AuthCallOptions build() {
            return new AuthCallOptions(this);
        }
    }
}
