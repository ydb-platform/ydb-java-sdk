package tech.ydb.coordination.recipes.leader_election;

import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class LeaderWatcher implements AutoCloseable {
    public interface Listener {
        void newLeader(byte[] leaderInfo);
        void lostLeader();
    }

    private final CoordinationClient client;
    private final String nodePath;
    private final String semaphoreName;

    private final AtomicReference<CoordinationSession> session = new AtomicReference<>();
    private volatile boolean isStopped = false;

    public LeaderWatcher(CoordinationClient client, String nodePath, String semaphoreName) {
        this.client = client;
        this.nodePath = nodePath;
        this.semaphoreName = semaphoreName;
    }

    @Override
    public void close() {
        isStopped = true;

        CoordinationSession old = session.getAndSet(null);
        if (old != null) {
            old.close();
        }
    }

    public void start(Listener listener) {
        if (isStopped) {
            throw new IllegalStateException("LeaderElection is already closed");
        }

        CoordinationSession newSession = client.createSession(nodePath);
        CoordinationSession old = session.getAndSet(newSession);
        if (old != null) {
            old.close();
        }

        newSession.addStateListener(state -> {
            if (state == CoordinationSession.State.LOST) {
                // restart lost session
                start(listener);
                return;
            }

            if (!state.isConnected()) {
                listener.lostLeader();
            } else {
                watchLeader(newSession, listener);
            }
        });

        newSession.connect();
    }

    private void watchLeader(CoordinationSession session, Listener listener) {
        if (!session.getState().isActive()) {
            return;
        }

        session.watchSemaphore(semaphoreName,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
        ).whenComplete((result, th) -> {
            if (result == null || !result.isSuccess()) {
                // retry
                watchLeader(session, listener);
                return;
            }

            SemaphoreWatcher watcher = result.getValue();

            SemaphoreDescription description = watcher.getDescription();
            if (description.getOwnersList().isEmpty()) {
                listener.lostLeader();
            } else {
                listener.newLeader(description.getOwnersList().get(0).getData());
            }

            watcher.getChangedFuture().thenRun(() -> watchLeader(session, listener));
        });
    }
}
