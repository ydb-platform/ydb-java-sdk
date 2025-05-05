package tech.ydb.coordination.recipes.util;

import tech.ydb.coordination.CoordinationSession;

public interface SessionListenableProvider {
    Listenable<CoordinationSession.State> getSessionListenable();
}
