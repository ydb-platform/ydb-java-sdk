package tech.ydb.coordination.recipes.group;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;
import tech.ydb.coordination.recipes.util.RetryableTask;
import tech.ydb.coordination.recipes.util.SemaphoreObserver;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * A distributed group membership implementation that uses coordination service
 * to manage membership and track group members.
 *
 * <p>This class provides mechanisms to:
 * <ul>
 *   <li>Join a named group</li>
 *   <li>Track current group members</li>
 *   <li>Receive notifications about membership changes</li>
 * </ul>
 *
 * <p>The implementation uses a semaphore with watch capabilities to track membership.
 */
public class GroupMembership implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(GroupMembership.class);
    private static final long MAX_GROUP_SIZE = Long.MAX_VALUE;
    private static final Duration ACQUIRE_TIMEOUT = Duration.ofSeconds(30);

    private final String groupName;
    private final RetryPolicy retryPolicy;
    private final ScheduledExecutorService scheduledExecutor;

    private final CoordinationSession coordinationSession;
    private final SemaphoreObserver semaphoreObserver;
    private final ListenableContainer<CoordinationSession.State> sessionListenable;
    private final ListenableContainer<List<GroupMember>> groupMembersListenable;

    private final AtomicReference<State> state = new AtomicReference<>(State.INITIAL);
    private final AtomicReference<Future<Status>> initializingTask = new AtomicReference<>(null);
    private Future<Status> acquireTask = null;

    /**
     * Internal state
      */
    private enum State {
        /** Initial state before starting */
        INITIAL,
        /** When start() has been called but initialization isn't complete */
        STARTING,
        /** Fully operational state */
        STARTED,
        /** Failed terminated state */
        FAILED,
        /** Closed terminated state */
        CLOSED
    }

    /**
     * Creates a new GroupMembership with default settings.
     *
     * @param coordinationClient the coordination service client
     * @param coordinationNodePath path to the coordination node
     * @param groupName name of the group to join
     * @throws IllegalArgumentException if any argument is invalid
     */
    public GroupMembership(
            CoordinationClient coordinationClient,
            String coordinationNodePath,
            String groupName
    ) {
        this(
                coordinationClient,
                coordinationNodePath,
                groupName,
                GroupMembershipSettings.newBuilder()
                        .build()
        );
    }

    /**
     * Creates a new GroupMembership with custom settings.
     *
     * @param coordinationClient the coordination service client
     * @param coordinationNodePath path to the coordination node
     * @param groupName name of the group to join
     * @param settings configuration settings
     * @throws IllegalArgumentException if any argument is invalid
     * @throws NullPointerException if any required argument is null
     */
    public GroupMembership(
            CoordinationClient coordinationClient,
            String coordinationNodePath,
            String groupName,
            GroupMembershipSettings settings
    ) {
        validateConstructorArgs(coordinationClient, coordinationNodePath, groupName, settings);

        this.groupName = groupName;
        this.retryPolicy = settings.getRetryPolicy();
        this.scheduledExecutor = settings.getScheduledExecutor();

        this.coordinationSession = coordinationClient.createSession(
                coordinationNodePath,
                CoordinationSessionSettings.newBuilder()
                        .withRetryPolicy(retryPolicy)
                        .build()
        );
        this.sessionListenable = new ListenableContainer<>();
        coordinationSession.addStateListener(sessionState -> {
            if (!state.get().equals(State.CLOSED) && (sessionState == CoordinationSession.State.LOST ||
                    sessionState == CoordinationSession.State.CLOSED)) {
                logger.error("Coordination session unexpectedly changed to {} state, group membership went FAILED",
                        sessionState);
                stopInternal(State.FAILED);
            }
            if (sessionState == CoordinationSession.State.RECONNECTED) {
                reconnect();
            }
            sessionListenable.notifyListeners(sessionState);
        });

        this.semaphoreObserver = new SemaphoreObserver(
                coordinationSession,
                groupName,
                WatchSemaphoreMode.WATCH_OWNERS,
                DescribeSemaphoreMode.WITH_OWNERS,
                settings.getRetryPolicy(),
                settings.getScheduledExecutor()
        );
        this.groupMembersListenable = new ListenableContainer<>();
        semaphoreObserver.getWatchDataListenable().addListener(description -> {
            List<GroupMember> groupMembers = mapSemaphoreDescriptionToMembersList(description);
            groupMembersListenable.notifyListeners(groupMembers);
        });
    }


    private void validateConstructorArgs(
            CoordinationClient coordinationClient,
            String coordinationNodePath,
            String groupName,
            GroupMembershipSettings settings
    ) {
        Objects.requireNonNull(coordinationClient, "CoordinationClient cannot be null");
        Objects.requireNonNull(coordinationNodePath, "Coordination node path cannot be null");
        Objects.requireNonNull(groupName, "Group name cannot be null");
        Objects.requireNonNull(settings, "Settings cannot be null");
        Objects.requireNonNull(settings.getRetryPolicy(), "Retry policy cannot be null");
        Objects.requireNonNull(settings.getScheduledExecutor(), "Scheduled executor cannot be null");

        if (groupName.isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be empty");
        }
        if (coordinationNodePath.isEmpty()) {
            throw new IllegalArgumentException("Coordination node path cannot be empty");
        }
    }

    private void reconnect() {
        logger.info("Attempting to reconnect group membership for group '{}'", groupName);
        tryEnqueueAcquire();
    }

    /**
     * Starts the group membership service.
     *
     * <p>This begins the process of joining the group and starts tracking membership.
     *
     * @throws IllegalStateException if already started
     */
    public void start() {
        Preconditions.checkState(
                state.compareAndSet(State.INITIAL, State.STARTING),
                "Group membership may be started only once"
        );

        logger.info("Starting group membership initialization for group '{}'", groupName);

        CompletableFuture<Status> sessionConnectTask = getSessionConnectRetryableTask();
        CompletableFuture<Status> semaphoreCreateTask = getSemaphoreCreateTask();

        CompletableFuture<Status> initializingRetriedTask = sessionConnectTask
                .thenCompose(connectionStatus -> {
                    connectionStatus.expectSuccess("Unable to establish session");
                    logger.debug("Successfully connected session for group '{}'", groupName);
                    return semaphoreCreateTask;
                })
                .thenApply(semaphoreStatus -> {
                    if (semaphoreStatus.isSuccess()) {
                        logger.info("Successfully initialized semaphore for group '{}'", groupName);
                        onInitializeSuccess();
                    } else {
                        logger.error("Failed to create semaphore for group '{}': {}", groupName, semaphoreStatus);
                    }
                    semaphoreStatus.expectSuccess("Unable to create semaphore");
                    return semaphoreStatus;
                }).exceptionally(ex -> {
                    logger.error("Group membership initialization failed for group '{}'", groupName, ex);
                    stopInternal(State.FAILED);
                    return Status.of(StatusCode.CLIENT_INTERNAL_ERROR);
                });

        initializingTask.set(initializingRetriedTask);
    }

    private CompletableFuture<Status> getSessionConnectRetryableTask() {
        return new RetryableTask(
                "groupMembership-sessionConnect-" + groupName,
                coordinationSession::connect,
                scheduledExecutor,
                retryPolicy
        ).execute();
    }

    private CompletableFuture<Status> getSemaphoreCreateTask() {
        Supplier<CompletableFuture<Status>> semaphoreCreateTaskSupplier = () ->
                coordinationSession.createSemaphore(groupName, MAX_GROUP_SIZE)
                        .thenCompose(status -> {
                            if (status.getCode() == StatusCode.ALREADY_EXISTS) {
                                return CompletableFuture.completedFuture(Status.SUCCESS);
                            }
                            return CompletableFuture.completedFuture(status);
                        });
        return new RetryableTask(
                "groupMembership-semaphoreCreate-" + groupName,
                semaphoreCreateTaskSupplier,
                scheduledExecutor,
                retryPolicy
        ).execute();
    }

    private void onInitializeSuccess() {
        logger.info("Group membership initialization completed successfully for group '{}'", groupName);
        state.set(State.STARTED);
        semaphoreObserver.start();
        tryEnqueueAcquire();
    }

    /**
     * Enqueues task if no current working task
     */
    private synchronized boolean tryEnqueueAcquire() {
        if (acquireTask != null) {
            logger.warn("Acquire task already in progress for group '{}', skipping", groupName);
            return false;
        }

        logger.debug("Enqueuing new acquire task for group '{}'", groupName);
        CompletableFuture<Status> acquireRetryableTask = new RetryableTask(
                "groupMembership-acquireSemaphoreTask-" + groupName,
                () -> coordinationSession.acquireSemaphore(groupName, 1, ACQUIRE_TIMEOUT)
                        .thenApply(Result::getStatus),
                scheduledExecutor,
                retryPolicy
        ).execute();

        acquireTask = acquireRetryableTask.whenComplete(this::finishAcquireTask);
        return true;
    }

    private synchronized void finishAcquireTask(Status status, @Nullable Throwable throwable) {
        acquireTask = null;

        if (throwable != null) {
            logger.error("Acquire task failed with exception for group '{}'", groupName, throwable);
            tryEnqueueAcquire();
            return;
        }

        if (status.isSuccess()) {
            logger.info("Successfully acquired semaphore for group '{}'", groupName);
            return;
        }

        logger.warn("Failed to acquire semaphore for group '{}' with status: '{}'", groupName, status);
        tryEnqueueAcquire();
    }

    /**
     * Gets the current list of group members.
     *
     * @return list of current group members, or null if not available
     */
    public @Nullable List<GroupMember> getCurrentMembers() {
        SemaphoreDescription cachedDescription = semaphoreObserver.getCachedData();
        return mapSemaphoreDescriptionToMembersList(cachedDescription);
    }

    private static @Nullable List<GroupMember> mapSemaphoreDescriptionToMembersList(SemaphoreDescription description) {
        if (description == null) {
            return null;
        }

        List<SemaphoreDescription.Session> ownersList = description.getOwnersList();
        return ownersList.stream().map(GroupMembership::mapSessionToGroupMember).collect(Collectors.toList());
    }

    private static GroupMember mapSessionToGroupMember(SemaphoreDescription.Session session) {
        return new GroupMember(
                session.getId(),
                session.getData()
        );
    }

    /**
     * Gets a listenable for session state changes.
     *
     * @return observable for coordination session state changes
     */
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return sessionListenable;
    }

    /**
     * Gets a listenable for group membership changes.
     *
     * @return observable for group membership changes
     */
    public Listenable<List<GroupMember>> getMembersListenable() {
        return groupMembersListenable;
    }

    /**
     * Closes the group membership and releases all resources.
     *
     * <p>After closing, the instance cannot be reused.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        logger.info("Closing group membership for group '{}'", groupName);
        stopInternal(State.CLOSED);
    }

    /**
     * Stops the service and transitions to specified termination state.
     *
     * @param terminationState either FAILED or CLOSED
     * @return true if state was changed, false if already terminated
     */
    private synchronized boolean stopInternal(State terminationState) {
        State localState = state.get();
        if (localState == State.FAILED || localState == State.CLOSED) {
            logger.warn("Attempted to stop already stopped group membership '{}' (current state: {})",
                    groupName, localState);
            return false;
        }
        logger.info("Stopping group membership '{}' (current state: {}, target state: {})",
                groupName, localState, terminationState);

        // change state
        state.set(terminationState);

        // stop tasks
        Future<Status> localInitializingTask = initializingTask.get();
        if (localInitializingTask != null) {
            localInitializingTask.cancel(true);
            initializingTask.set(null);
        }
        Future<Status> localAcquireTask = acquireTask;
        if (localAcquireTask != null) {
            localAcquireTask.cancel(true);
            acquireTask = null;
        }

        // Clean up resources
        try {
            semaphoreObserver.close();
        } catch (Exception e) {
            logger.warn("Error closing semaphore observer for {}: {}", groupName, e.getMessage());
        }
        try {
            coordinationSession.close();
        } catch (Exception e) {
            logger.warn("Error closing session for {}: {}", groupName, e.getMessage());
        }

        return true;
    }
}
