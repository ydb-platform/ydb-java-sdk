package tech.ydb.core.impl.auth;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.CallOptions;

import tech.ydb.auth.AuthIdentity;
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
            String database,
            List<EndpointRecord> endpoints,
            ManagedChannelFactory channelFactory,
            GrpcTransportBuilder transportBuilder) {
        CallOptions options = CallOptions.DEFAULT;

        if (transportBuilder.getAuthProvider() != null) {
            GrpcAuthRpc rpc = new GrpcAuthRpc(endpoints, scheduler, database, channelFactory);
            authIdentity = transportBuilder.getAuthProvider().createAuthIdentity(rpc);
        } else {
            authIdentity = null;
        }

        if (authIdentity != null) {
            options = options.withCallCredentials(new YdbCallCredentials(authIdentity));
        }

        if (transportBuilder.getCallExecutor() != null
                && transportBuilder.getCallExecutor() != MoreExecutors.directExecutor()) {
            options = options.withExecutor(transportBuilder.getCallExecutor());
        }

        if (transportBuilder.getGrpcCompression() != GrpcCompression.NO_COMPRESSION) {
            options = options.withCompression(transportBuilder.getGrpcCompression().compressor());
        }

        this.callOptions = options;
        this.readTimeoutMillis = transportBuilder.getReadTimeoutMillis();
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
