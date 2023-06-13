package tech.ydb.coordination.scenario.leader_election;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.scenario.WorkingScenario;
import tech.ydb.core.Status;
import tech.ydb.proto.coordination.SemaphoreDescription;
import tech.ydb.proto.coordination.SemaphoreSession;
import tech.ydb.proto.coordination.SessionRequest;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElection extends WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElection.class);
    private static final int LIMIT_TOKENS_SEMAPHORE = 1;
    private static final int COUNT_TOKENS = 1;

    private final AtomicLong epochLeader = new AtomicLong();

    private LeaderElection(CoordinationClient client, Settings settings) {
        super(client, settings, LIMIT_TOKENS_SEMAPHORE);
    }

    public static Builder newBuilder(
            CoordinationClient client,
            String ticket,
            Observer observer
    ) {
        return new Builder(client, ticket, observer);
    }

    public long epochLeader() {
        return epochLeader.get();
    }

    private void describeSemaphore() {
        currentCoordinationSession.get().sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(settings.getSemaphoreName())
                        .setWatchOwners(true)
                        .setIncludeOwners(true)
                        .build()
        );
    }

    public interface Observer {

        void onNext(String ticket);
    }

    public static class Builder extends WorkingScenario.Builder<LeaderElection> {

        private final String ticket;
        private final Observer observer;

        public Builder(
                CoordinationClient client,
                String ticket,
                Observer observer
        ) {
            super(client);

            this.ticket = ticket;
            this.observer = observer;
        }

        @Override
        protected LeaderElection buildScenario(Settings settings) {
            LeaderElection leaderElection = new LeaderElection(client, settings);

            leaderElection.start(
                    new CoordinationSession.Observer() {
                        @Override
                        public void onAcquireSemaphoreResult(boolean acquired, Status status) {
                            if (acquired) {
                                leaderElection.describeSemaphore();
                            }
                        }

                        @Override
                        public void onAcquireSemaphorePending() {
                            leaderElection.describeSemaphore();
                        }

                        @Override
                        public void onDescribeSemaphoreResult(
                                SemaphoreDescription semaphoreDescription,
                                Status status
                        ) {
                            if (status.isSuccess()) {
                                SemaphoreSession semaphoreSessionLeader = semaphoreDescription.getOwnersList().get(0);

                                if (semaphoreSessionLeader.getOrderId() > leaderElection.epochLeader()) {
                                    leaderElection.epochLeader.set(semaphoreSessionLeader.getOrderId());

                                    observer.onNext(
                                            semaphoreSessionLeader.getData()
                                                    .toString(StandardCharsets.UTF_8)
                                    );
                                }
                            } else {
                                logger.error("Error describer result from leader election session, " +
                                        "status: {}", status);
                            }
                        }

                        @Override
                        public void onDescribeSemaphoreChanged(boolean dataChanged, boolean ownersChanged) {
                            if (ownersChanged) {
                                leaderElection.describeSemaphore();
                            }
                        }

                        @Override
                        public void onFailure(Status status) {
                            logger.error("Fail from leader election session: {}", status);
                        }

                        @Override
                        public void onSessionStarted() {
                            logger.info("Starting leader election session, sessionId: {}",
                                    leaderElection.currentCoordinationSession.get().getSessionId());
                        }

                        @Override
                        public void onCreateSemaphoreResult(Status status) {
                            logger.info("Creating semaphore {}, with status: {}", settings.getSemaphoreName(), status);

                            leaderElection.currentCoordinationSession.get().sendAcquireSemaphore(
                                    SessionRequest.AcquireSemaphore.newBuilder()
                                            .setName(settings.getSemaphoreName())
                                            .setCount(COUNT_TOKENS)
                                            .setTimeoutMillis(-1)
                                            .setData(ByteString.copyFrom(ticket.getBytes(StandardCharsets.UTF_8)))
                                            .build()
                            );
                        }
                    }
            );

            return leaderElection;
        }
    }
}
