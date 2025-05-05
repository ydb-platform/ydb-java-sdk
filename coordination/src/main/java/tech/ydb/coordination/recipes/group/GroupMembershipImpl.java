package tech.ydb.coordination.recipes.group;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.recipes.locks.LockInternals;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableContainer;
import tech.ydb.coordination.recipes.util.SemaphoreObserver;
import tech.ydb.coordination.settings.CoordinationSessionSettings;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;

public class GroupMembershipImpl implements GroupMembership {
    private static final long MAX_GROUP_SIZE = Long.MAX_VALUE;

    private final CoordinationClient coordinationClient;
    private final String coordinationNodePath;
    private final String groupName;
    private final RetryPolicy retryPolicy;

    private final CoordinationSession session;
    private final LockInternals lockInternals;
    private final SemaphoreObserver semaphoreObserver;
    private final ListenableContainer<CoordinationSession.State> sessionStateListenable;
    private final ListenableContainer<List<GroupMember>> groupMembersListenable;

    public GroupMembershipImpl(
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

    public GroupMembershipImpl(
            CoordinationClient coordinationClient,
            String coordinationNodePath,
            String groupName,
            GroupMembershipSettings settings
    ) {
        this.coordinationClient = coordinationClient;
        this.coordinationNodePath = coordinationNodePath;
        this.groupName = groupName;
        this.retryPolicy = settings.getRetryPolicy();

        this.session = coordinationClient.createSession(
                coordinationNodePath,
                CoordinationSessionSettings.newBuilder()
                        .withRetryPolicy(retryPolicy)
                        .build()
        );
        this.sessionStateListenable = new ListenableContainer<>();
        session.addStateListener(sessionStateListenable::notifyListeners);

        this.lockInternals = new LockInternals(
                session,
                groupName,
                MAX_GROUP_SIZE
        );

        this.semaphoreObserver = new SemaphoreObserver(
                session,
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

    @Override
    public void start() {
        // TODO: correctly handle failed connection, failed semaphore and failed starts
        session.connect().thenAccept(sessionStatus -> {
            sessionStatus.expectSuccess("Unable to establish session");
            session.createSemaphore(groupName, MAX_GROUP_SIZE).thenAccept(semaphoreStatus -> {
                // TODO: start acquiring task
                semaphoreObserver.start();
            });
        });
    }

    @Override
    public @Nullable List<GroupMember> getCurrentMembers() {
        SemaphoreDescription cachedDescription = semaphoreObserver.getCachedData();
        return mapSemaphoreDescriptionToMembersList(cachedDescription);
    }

    private static @Nullable List<GroupMember> mapSemaphoreDescriptionToMembersList(SemaphoreDescription description) {
        if (description == null) {
            return null;
        }

        List<SemaphoreDescription.Session> ownersList = description.getOwnersList();
        return ownersList.stream().map(GroupMembershipImpl::mapSessionToGroupMember).collect(Collectors.toList());
    }

    private static GroupMember mapSessionToGroupMember(SemaphoreDescription.Session session) {
        return new GroupMember(
                session.getId(),
                session.getData()
        );
    }

    @Override
    public Listenable<CoordinationSession.State> getSessionListenable() {
        return sessionStateListenable;
    }

    @Override
    public Listenable<List<GroupMember>> getMembersListenable() {
        return groupMembersListenable;
    }

    @Override
    public void close() throws IOException {
        session.close();
        lockInternals.close();
        semaphoreObserver.close();
    }
}
