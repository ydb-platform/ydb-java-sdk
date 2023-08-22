package tech.ydb.coordination.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.settings.CoordinationNodeSettings;
import tech.ydb.coordination.settings.DescribeCoordinationNodeSettings;
import tech.ydb.coordination.settings.DropCoordinationNodeSettings;
import tech.ydb.core.Status;

public class CoordinationClientStub implements CoordinationClient {
    List<CoordinationSessionStub> sessions = new ArrayList<>();
    @Override
    public CoordinationSession createSession() {
        sessions.add(new CoordinationSessionStub());
        return sessions.get(sessions.size() - 1);
    }

    @Override
    public CompletableFuture<Status> createNode(String path, CoordinationNodeSettings coordinationNodeSettings) {
        return CompletableFuture.completedFuture(Status.SUCCESS);
    }

    @Override
    public CompletableFuture<Status> alterNode(String path, CoordinationNodeSettings coordinationNodeSettings) {
        return CompletableFuture.completedFuture(Status.SUCCESS);
    }

    @Override
    public CompletableFuture<Status> dropNode(String path, DropCoordinationNodeSettings dropCoordinationNodeSettings) {
        return CompletableFuture.completedFuture(Status.SUCCESS);
    }

    @Override
    public CompletableFuture<Status> describeNode(String path,
                                                  DescribeCoordinationNodeSettings describeCoordinationNodeSettings) {
        return CompletableFuture.completedFuture(Status.SUCCESS);
    }

    @Override
    public String getDatabase() {
        return "database";
    }
}
