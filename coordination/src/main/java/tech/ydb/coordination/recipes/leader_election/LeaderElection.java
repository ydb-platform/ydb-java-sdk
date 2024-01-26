package tech.ydb.coordination.recipes.leader_election;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;

import static tech.ydb.coordination.CoordinationSession.State.LOST;

public class LeaderElection implements AutoCloseable {
    public interface Listener {
        void takeLeadership();
        void suspendLeadership();
        void resumeLeadership();
        void lostLeadership();
    }

    private final CoordinationClient client;
    private final String nodePath;
    private final String semaphoreName;
    private final byte[] leaderData;

    private final AtomicReference<CoordinationSession> session = new AtomicReference<>();
    private volatile boolean isStopped = false;

    public LeaderElection(CoordinationClient client, String nodePath, String semaphoreName, byte[] info) {
        this.client = client;
        this.nodePath = nodePath;
        this.semaphoreName = semaphoreName;
        this.leaderData = info;
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
            switch (state) {
                case UNSTARTED:
                case CONNECTING:
                default:
                    // do nothing
                    break;
                case CONNECTED:
                    tryTakeLeadership(newSession, listener);
                    break;
                case RECONNECTING:
                    listener.suspendLeadership();
                    break;
                case RECONNECTED:
                    listener.resumeLeadership();
                    break;
                case LOST:
                    listener.lostLeadership();
                    // restart lost session
                    start(listener);
                    break;
                case CLOSED:
                    listener.lostLeadership();
                    break;
            }
        });

        newSession.connect().join();
    }

    private void tryTakeLeadership(CoordinationSession session, Listener listener) {
        if (!session.getState().isActive()) {
            return;
        }

        session.acquireSemaphore(semaphoreName, 1, leaderData, Duration.ofSeconds(5)).whenComplete((result, th) -> {
            if (result != null && result.isSuccess()) {
                listener.takeLeadership();
                return;
            }

            // retry
            tryTakeLeadership(session, listener);
        });
    }
}
