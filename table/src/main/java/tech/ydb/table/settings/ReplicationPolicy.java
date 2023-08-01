package tech.ydb.table.settings;

import javax.annotation.Nullable;

/**
 * @author openminder
 */

public class ReplicationPolicy {

    @Nullable
    private String presetName;
    private int replicasCount;
    private boolean createPerAvailabilityZone;
    private boolean allowPromotion;

    @Nullable
    public String getPresetName() {
        return presetName;
    }

    public ReplicationPolicy setPresetName(@Nullable String presetName) {
        this.presetName = presetName;
        return this;
    }

    public int getReplicasCount() {
        return replicasCount;
    }

    public ReplicationPolicy setReplicasCount(int replicasCount) {
        this.replicasCount = replicasCount;
        return this;
    }

    public boolean isCreatePerAvailabilityZone() {
        return createPerAvailabilityZone;
    }

    public ReplicationPolicy setCreatePerAvailabilityZone(boolean createPerAvailabilityZone) {
        this.createPerAvailabilityZone = createPerAvailabilityZone;
        return this;
    }

    public boolean isAllowPromotion() {
        return allowPromotion;
    }

    public ReplicationPolicy setAllowPromotion(boolean allowPromotion) {
        this.allowPromotion = allowPromotion;
        return this;
    }

}
