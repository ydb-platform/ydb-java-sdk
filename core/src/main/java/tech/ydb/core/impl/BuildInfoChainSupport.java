package tech.ydb.core.impl;

import tech.ydb.core.grpc.GrpcTransport;

/**
 * Internal bridge for enabling SDK observability adoption chains on the transport after it has been built.
 * <p>
 * Unlike tracing (configured on the builder before {@code build()}), metrics adoption becomes known only
 * once a higher-level client is wired with a real {@code Meter}, which happens after the transport exists.
 * This class avoids adding a public transport hook by unwrapping the default SDK implementation and calling
 * a package-private extension point on it; non-SDK transports silently no-op.
 */
public final class BuildInfoChainSupport {
    private BuildInfoChainSupport() {
    }

    public static void enableMetricsChain(GrpcTransport transport) {
        if (transport instanceof YdbTransportImpl) {
            ((YdbTransportImpl) transport).enableMetricsChain();
        }
    }
}
