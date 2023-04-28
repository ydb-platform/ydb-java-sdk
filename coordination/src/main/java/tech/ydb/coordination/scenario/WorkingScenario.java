package tech.ydb.coordination.scenario;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.settings.ScenarioSettings;
import tech.ydb.core.Status;

/**
 * @author Kirill Kurdyukov
 */
public abstract class WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(WorkingScenario.class);

    protected final AtomicReference<CoordinationSession> currentCoordinationSession;
    protected final AtomicBoolean isWorking = new AtomicBoolean(true);
    protected final ScenarioSettings settings;

    private final CoordinationClient client;
    private final long semaphoreLimit;

    public WorkingScenario(
         CoordinationClient client,
         ScenarioSettings settings,
         long semaphoreLimit
    ) {
        this.client = client;
        this.settings = settings;
        this.semaphoreLimit = semaphoreLimit;
        this.currentCoordinationSession = new AtomicReference<>(client.createSession());
    }

    protected void start(CoordinationSession.Observer observer) {
        tryStart(null, observer);
    }

    protected void tryStart(
            Status status,
            CoordinationSession.Observer observer
    ) {
        if (status != null) {
            logger.info("Stopped session with status: {}", status);
        }

        if (isWorking.get()) {
            logger.info("Starting session...");

            CoordinationSession coordinationSession = client.createSession();

            coordinationSession.start(observer).whenComplete(
                    (completableStatus, throwable) -> {
                        if (throwable != null) {
                            logger.error("Failed coordination session", throwable);
                        }

                        tryStart(completableStatus, observer);
                    }
            );

            coordinationSession.sendStartSession(
                    SessionRequest.SessionStart.newBuilder()
                            .setSessionId(ScenarioSettings.START_SESSION_ID)
                            .setPath(settings.getCoordinationNodePath())
                            .setDescription(settings.getDescription())
                            .setTimeoutMillis(ScenarioSettings.SESSION_KEEP_ALIVE_TIMEOUT_MS)
                            .build()
            );

            coordinationSession.sendCreateSemaphore(
                    SessionRequest.CreateSemaphore.newBuilder()
                            .setName(settings.getSemaphoreName())
                            .setLimit(semaphoreLimit)
                            .build()
            );

            currentCoordinationSession.set(coordinationSession);
        }
    }

    public void stop() {
        if (isWorking.compareAndSet(true, false)) {
            logger.info("Stopping session...");

            while (true) {
                CoordinationSession coordinationSession = currentCoordinationSession.get();

                if (coordinationSession != null) {
                    coordinationSession.stop();
                }

                if (currentCoordinationSession.compareAndSet(coordinationSession, coordinationSession)) {
                    break;
                }
            }
        }
    }
}
