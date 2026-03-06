package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
import tech.ydb.auth.AuthRpcProvider;
import tech.ydb.core.grpc.GrpcCompression;
import tech.ydb.core.grpc.GrpcTransportBuilder;
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

    public AuthCallOptions(
            ScheduledExecutorService scheduler,
            List<EndpointRecord> endpoints,
            ManagedChannelFactory channelFactory,
            GrpcTransportBuilder builder) {
        CallOptions options = CallOptions.DEFAULT;

        AuthRpcProvider<? super GrpcAuthRpc> authProvider = builder.getAuthProvider();
        if (authProvider != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(endpoints, scheduler, builder.getDatabase(), channelFactory);
            authIdentity = builder.getAuthProvider().createAuthIdentity(rpc);
        } else {
            authIdentity = null;
        }

        if (builder.getCallExecutor() != null && builder.getCallExecutor() != MoreExecutors.directExecutor()) {
            options = options.withExecutor(builder.getCallExecutor());
        }

        if (builder.getGrpcCompression() != GrpcCompression.NO_COMPRESSION) {
            options = options.withCompression(builder.getGrpcCompression().compressor());
        }

        this.callOptions = options;
        this.readTimeoutMillis = builder.getReadTimeoutMillis();
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
}
