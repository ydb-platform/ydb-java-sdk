package tech.ydb.core.impl.pool;

import java.util.List;

import tech.ydb.core.grpc.BalancingSettings;

/**
 *
 * @author Aleksandr Gorshenin {@literal <alexandr268@ydb.tech>}
 */
@FunctionalInterface
public interface PriorityPicker {
    int getPriority(EndpointRecord endpoint);

    static PriorityPicker from(BalancingSettings cfg, String selfLoc, Piles piles, List<EndpointRecord> endpoints) {
        switch (cfg.getPolicy()) {
            case USE_ALL_NODES:
                return endpoint -> 0;
            case USE_PREFERABLE_PRIMARY_PILE:
                return piles;
            case USE_PREFERABLE_LOCATION:
                return new LocationPicker(cfg.getPreferableLocation(), selfLoc);
            case USE_DETECT_LOCAL_DC:
                return new LocationPicker(endpoints);
            default:
                throw new RuntimeException("Not implemented balancing policy: " + cfg.getPolicy().name());
        }
    }
}
