package tech.ydb.core.grpc;

/**
 * @author Nikolay Perfilov
 */
public class BalancingSettings {
    public enum Policy {
        /**  Use all available cluster nodes regardless datacenter locality
         */
        USE_ALL_NODES,
        /** Use preferable location (data center),
         * preferableLocation is a name of location (VLA, MYT, SAS, MAN).
         * If preferableLocation is not set local datacenter is used (not recommended)
         */
        USE_PREFERABLE_LOCATION
    }

    private final Policy policy;
    private final String preferableLocation;

    private BalancingSettings(Policy policy, String preferableLocation) {
        this.policy = policy;
        this.preferableLocation = preferableLocation;
    }

    public Policy getPolicy() {
        return policy;
    }

    public String getPreferableLocation() {
        return preferableLocation;
    }

    public static BalancingSettings defaultInstance() {
        return new BalancingSettings(Policy.USE_ALL_NODES, null);
    }

    public static BalancingSettings fromPolicy(Policy balancingPolicy) {
        return new BalancingSettings(balancingPolicy, null);
    }

    public static BalancingSettings fromLocation(String preferableLocation) {
        return new BalancingSettings(Policy.USE_PREFERABLE_LOCATION, preferableLocation);
    }

    @Override
    public String toString() {
        return "BalancingSettings{" +
                "policy=" + policy +
                ", preferableLocation='" + preferableLocation + '\'' +
                '}';
    }
}
