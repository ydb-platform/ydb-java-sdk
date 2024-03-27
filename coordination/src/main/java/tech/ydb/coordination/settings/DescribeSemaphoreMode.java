package tech.ydb.coordination.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum DescribeSemaphoreMode {
    /**
     * Describe only semaphore's data (name, user-defined data and others)
     */
    DATA_ONLY(false, false),
    /**
     * Include owners list to describe result
     */
    WITH_OWNERS(true, false),
    /**
     * Include waiters list to describe result
     */
    WITH_WAITERS(false, true),
    /**
     * Include waiters and owners lists to describe result
     */
    WITH_OWNERS_AND_WAITERS(true, true);

    private final boolean includeOwners;
    private final boolean includeWaiters;

    DescribeSemaphoreMode(boolean includeOwners, boolean includeWaiters) {
        this.includeOwners = includeOwners;
        this.includeWaiters = includeWaiters;
    }

    public boolean includeOwners() {
        return includeOwners;
    }

    public boolean includeWaiters() {
        return includeWaiters;
    }
}
