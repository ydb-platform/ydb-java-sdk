package tech.ydb.coordination.recipes.util;

import tech.ydb.coordination.CoordinationSession;

/**
 * Provides access to a Listenable for session state changes.
 */
public interface SessionListenableProvider {
    /**
     * Gets the Listenable for session state changes.
     *
     * @return the Listenable instance for session state changes, never null
     */
    Listenable<CoordinationSession.State> getSessionListenable();
}
