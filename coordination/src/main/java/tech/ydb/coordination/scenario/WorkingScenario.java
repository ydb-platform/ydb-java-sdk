package tech.ydb.coordination.scenario;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.core.Status;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.proto.coordination.SessionRequest;

/**
 * @author Kirill Kurdyukov
 */
public abstract class WorkingScenario {

    private static final Logger logger = LoggerFactory.getLogger(WorkingScenario.class);

    protected final AtomicReference<CoordinationSession> currentCoordinationSession;
    protected final AtomicBoolean isWorking = new AtomicBoolean(true);
    protected final Settings settings;

    private final CoordinationClient client;
    private final long semaphoreLimit;

    public WorkingScenario(
            CoordinationClient client,
            Settings settings,
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

                        try {
                            tryStart(completableStatus, observer);
                        } catch (RuntimeException e) {
                            logger.error("Failed trying start session", e);
                        }
                    }
            );

            currentCoordinationSession.set(coordinationSession);

            byte[] protectionKey = new byte[16];
            ThreadLocalRandom.current().nextBytes(protectionKey);

            coordinationSession.sendStartSession(
                    SessionRequest.SessionStart.newBuilder()
                            .setSessionId(Settings.START_SESSION_ID)
                            .setPath(settings.getCoordinationNodePath())
                            .setDescription(settings.getDescription())
                            .setTimeoutMillis(Settings.SESSION_KEEP_ALIVE_TIMEOUT_MS)
                            .setProtectionKey(ByteString.copyFrom(protectionKey))
                            .build()
            );

            coordinationSession.sendCreateSemaphore(
                    SessionRequest.CreateSemaphore.newBuilder()
                            .setName(settings.getSemaphoreName())
                            .setLimit(semaphoreLimit)
                            .build()
            );
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

    protected static class Settings {
        public static final int START_SESSION_ID = 0;
        public static final int SESSION_KEEP_ALIVE_TIMEOUT_MS = 5000;

        private final String coordinationNodePath;

        /**
         * Used for creating semaphore name.
         */
        private final String semaphoreName;

        /**
         * Text description of the session is displayed in the internal interfaces and
         * can be useful when diagnosing problems.
         */
        private final String description;

        Settings(
                Builder<?> builder
        ) {
            this.coordinationNodePath = builder.coordinationNodeName;
            this.semaphoreName = builder.semaphoreName;
            this.description = builder.description;
        }

        public String getCoordinationNodePath() {
            return coordinationNodePath;
        }

        public String getSemaphoreName() {
            return semaphoreName;
        }

        public String getDescription() {
            return description;
        }
    }

    public abstract static class Builder<T extends WorkingScenario> {

        protected final CoordinationClient client;

        private String coordinationNodeName = "coordination-node-default";
        private String semaphoreName = "semaphore-default";
        private String description = "";

        public Builder(CoordinationClient client) {
            this.client = client;
        }

        public Builder<T> setCoordinationNodeName(@Nonnull String coordinationNodeName) {
            this.coordinationNodeName = Preconditions.checkNotNull(
                    coordinationNodeName,
                    "Coordination node name shouldn’t be null!"
            );

            return this;
        }

        public Builder<T> setSemaphoreName(@Nonnull String semaphoreName) {
            this.semaphoreName = Preconditions.checkNotNull(
                    semaphoreName,
                    "Session semaphore name shouldn't be null!"
            );

            return this;
        }

        public Builder<T> setDescription(@Nonnull String description) {
            this.description = Preconditions.checkNotNull(
                    description,
                    "Descriptions shouldn’t be null!"
            );

            return this;
        }

        protected abstract T buildScenario(WorkingScenario.Settings settings);

        public CompletableFuture<T> start() {
            if (!coordinationNodeName.startsWith(client.getDatabase())) {
                setCoordinationNodeName(client.getDatabase() + "/" + coordinationNodeName);
            }

            return client.createNode(
                    coordinationNodeName,
                    CoordinationNodeSettings.newBuilder().build()
            ).thenApply(
                    status -> {
                        if (status.isSuccess()) {
                            return buildScenario(new WorkingScenario.Settings(this));
                        } else {
                            throw new UnexpectedResultException("Fail creating scenario", status);
                        }
                    }
            );
        }
    }
}
