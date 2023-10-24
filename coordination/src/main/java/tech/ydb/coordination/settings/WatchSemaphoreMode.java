package tech.ydb.coordination.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum WatchSemaphoreMode {
    /**
     * Watch for changes in semaphore data
     */
    WATCH_DATA(true, false),
    /**
     * Watch for changes in semaphore owners
     */
    WATCH_OWNERS(false, true),
    /**
     * Watch for changes in semaphore data or owners
     */
    WATCH_DATA_AND_OWNERS(true, true);

    private final boolean watchData;
    private final boolean watchOwners;

    WatchSemaphoreMode(boolean watchData, boolean watchOwners) {
        this.watchData = watchData;
        this.watchOwners = watchOwners;
    }

    public boolean watchData() {
        return watchData;
    }

    public boolean watchOwners() {
        return watchOwners;
    }
}
