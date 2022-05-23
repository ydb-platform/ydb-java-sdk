package tech.ydb.core.grpc;

/**
 * @author Nikolay Perfilov
 */
public class BalancingSettings {
    public final static BalancingPolicy DEFAULT_BALANCING_POLICY = BalancingPolicy.USE_ALL_NODES;

    public final BalancingPolicy policy;
    public final String preferableLocation;

    public BalancingSettings(BalancingPolicy policy, String preferableLocation) {
        this.policy = policy;
        this.preferableLocation = preferableLocation;
    }

    public BalancingSettings() {
        this.policy = DEFAULT_BALANCING_POLICY;
        this.preferableLocation = null;
    }

    public static BalancingSettings fromPolicy(BalancingPolicy balancingPolicy) {
        return new BalancingSettings(balancingPolicy, null);
    }

    public static BalancingSettings fromLocation(String preferableLocation) {
        return new BalancingSettings(BalancingPolicy.USE_PREFERABLE_LOCATION, preferableLocation);
    }

    @Override
    public String toString() {
        return "BalancingSettings{" +
                "policy=" + policy +
                ", preferableLocation='" + preferableLocation + '\'' +
                '}';
    }
}
