package tech.ydb.coordination.settings;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.exceptions.CreateSessionException;
import tech.ydb.coordination.scenario.WorkingScenario;

public class ScenarioSettings {

    public static final int START_SESSION_ID = 0;
    public static final int SESSION_KEEP_ALIVE_TIMEOUT_MS = 0;

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

    private ScenarioSettings(
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

        protected abstract T buildScenario(ScenarioSettings settings);

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
                            return buildScenario(new ScenarioSettings(this));
                        } else {
                            throw new CreateSessionException(status);
                        }
                    }
            );
        }
    }
}
