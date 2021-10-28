package tech.ydb.core.grpc;

public enum DiscoveryMode {
    /** Block in GrpcTransport constructor until we get list of endpoints
     * and establish connection to at least some of it.
     * Note: Even in Sync mode SDK will perform lazy async update of endpoints list
     */
    SYNC,
    /** Do not block in GrpcTransport constructor until we get list of endpoints and establish any connection.
     * Instead, standard grpc lazy initialization will be launched on first rpc call.
     * This call and all subsequent ones will be waiting for connection initialization
     * which takes couple seconds on average.
     * This will lead to much greater latencies for first requests.
     * More importantly, all of these waiting requests are more likely to be sent on the first connected channel
     * which will lead to session disbalance due to all sessions being created on that first connected node.
     */
    ASYNC
}
