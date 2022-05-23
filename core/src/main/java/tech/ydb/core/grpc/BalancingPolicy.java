package tech.ydb.core.grpc;

/**
 * @author Nikolay Perfilov
 */
public enum BalancingPolicy {
    /**  Use all available cluster nodes regardless datacenter locality
     */
    USE_ALL_NODES,
    /** Use preferable location (data center),
     * preferableLocation is a name of location (VLA, MYT, SAS, MAN).
     * If preferableLocation is not set local datacenter is used (not recommended)
     */
    USE_PREFERABLE_LOCATION
}
