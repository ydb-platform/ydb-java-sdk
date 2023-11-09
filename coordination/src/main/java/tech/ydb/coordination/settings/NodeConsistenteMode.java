package tech.ydb.coordination.settings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public enum NodeConsistenteMode {
    /** The default or current value */
    UNSET,
    /** Strict mode makes sure operations may only complete on current leader */
    STRICT,
    /** Relaxed mode allows operations to complete on stale masters */
    RELAXED
}
