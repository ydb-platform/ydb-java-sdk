package tech.ydb.core.grpc;

import tech.ydb.core.grpc.impl.ydb.EndpointPool;
import tech.ydb.core.grpc.impl.ydb.GrpcChannelPool;

/**
 * @author Nikolay Perfilov
 */
public enum TransportImplType {
    /** Use native grpc implementation.
     * io.grpc.ManagedChannel is responsible for endpoint balancing.
     * We can specify LoadBalancer (such as RandomChoiceLoadBalancer),
     * but we can't choose where to send requests directly.
     */
    GRPC_TRANSPORT_IMPL,
    /** Use YDB transport implementation over grpc (In development!).
     * {@link EndpointPool} is responsible for endpoint balancing, pessimization e.t.c.
     * We can send a request to any endpoint if it is present in {@link EndpointPool}.
     * {@link GrpcChannelPool} maintains relevant grpc connections.
     */
    YDB_TRANSPORT_IMPL
}
