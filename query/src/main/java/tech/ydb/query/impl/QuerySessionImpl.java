package tech.ydb.query.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import io.grpc.Metadata;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.YdbHeaders;
import tech.ydb.core.impl.call.ProxyReadStream;
import tech.ydb.core.settings.OperationSettings;
import tech.ydb.proto.draft.query.YdbQuery;
import tech.ydb.query.QuerySession;
import tech.ydb.query.settings.AttachSessionSettings;
import tech.ydb.query.settings.CreateSessionSettings;
import tech.ydb.query.settings.DeleteSessionSettings;

/**
 *
 * @author Aleksandr Gorshenin
 */
public abstract class QuerySessionImpl implements QuerySession {
    private static final Metadata SERVER_HINT_DATA = new Metadata();
    static {
        SERVER_HINT_DATA.put(YdbHeaders.YDB_CLIENT_CAPABILITIES, "session-balancer");
    }

    private static final StatusExtract<YdbQuery.CreateSessionResponse> CREATE_SESSION = StatusExtract.of(
            YdbQuery.CreateSessionResponse::getStatus, YdbQuery.CreateSessionResponse::getIssuesList
    );

    private static final StatusExtract<YdbQuery.DeleteSessionResponse> DELETE_SESSION = StatusExtract.of(
            YdbQuery.DeleteSessionResponse::getStatus, YdbQuery.DeleteSessionResponse::getIssuesList
    );

    private final Clock clock;
    private final QueryServiceRpc rpc;
    private final String id;
    private final long nodeID;
    private volatile Instant lastActive;

    QuerySessionImpl(Clock clock, QueryServiceRpc rpc, YdbQuery.CreateSessionResponse response) {
        this.clock = clock;
        this.rpc = rpc;
        this.id = response.getSessionId();
        this.nodeID = response.getNodeId();
        this.lastActive = clock.instant();
    }

    public String getId() {
        return this.id;
    }

    public Instant getLastActive() {
        return this.lastActive;
    }

    GrpcReadStream<Status> attach(AttachSessionSettings settings) {
        YdbQuery.AttachSessionRequest request = YdbQuery.AttachSessionRequest.newBuilder()
                .setSessionId(id)
                .build();
        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return new ProxyReadStream<>(rpc.attachSession(request, grpcSettings), (message, promise, observer) -> {
            observer.onNext(Status.of(
                    StatusCode.fromProto(message.getStatus()),
                    null,
                    Issue.fromPb(message.getIssuesList())
            ));
        });
    }

    private GrpcRequestSettings makeGrpcRequestSettings(OperationSettings settings) {
        return GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withPreferredNodeID((int) nodeID)
//                .withTrailersHandler(shutdownHandler)
                .build();
    }

    public CompletableFuture<Result<YdbQuery.DeleteSessionResponse>> delete(DeleteSessionSettings settings) {
        YdbQuery.DeleteSessionRequest request = YdbQuery.DeleteSessionRequest.newBuilder()
                .setSessionId(id)
                .build();

        GrpcRequestSettings grpcSettings = makeGrpcRequestSettings(settings);
        return rpc.deleteSession(request, grpcSettings).thenApply(DELETE_SESSION);
    }

    static CompletableFuture<Result<YdbQuery.CreateSessionResponse>> createSession(
            QueryServiceRpc rpc,
            CreateSessionSettings settings,
            boolean useServerBalancer) {
        YdbQuery.CreateSessionRequest request = YdbQuery.CreateSessionRequest.newBuilder()
                .build();

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(settings.getRequestTimeout())
                .withExtraHeaders(useServerBalancer ? SERVER_HINT_DATA : null)
                .build();

        return rpc.createSession(request, grpcSettings).thenApply(CREATE_SESSION);
    }
}
