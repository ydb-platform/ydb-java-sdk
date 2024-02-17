package tech.ydb.coordination.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.YdbIssueMessage;
import tech.ydb.proto.coordination.SessionRequest;
import tech.ydb.proto.coordination.SessionResponse;

/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> Type of result
 */
abstract class StreamMsg<R> {
    protected final CompletableFuture<R> future = new CompletableFuture<>();

    public CompletableFuture<R> getResult() {
        return future;
    }

    public abstract SessionRequest makeRequest(long reqId);
    public abstract boolean handleResponse(SessionResponse response);
    public abstract boolean handleError(Status status);

    public static StreamMsg<Status> createSemaphoreMsg(String name, long limit, byte[] data) {
        return new CreateSemaphoreMsg(name, limit, data);
    }

    public static StreamMsg<Status> updateSemaphoreMsg(String name, byte[] data) {
        return new UpdateSemaphoreMsg(name, data);
    }

    public static StreamMsg<Status> deleteSemaphoreMsg(String name, boolean force) {
        return new DeleteSemaphoreMsg(name, force);
    }

    private abstract static class BaseStatusMsg extends StreamMsg<Status> {
        @Override
        public boolean handleError(Status status) {
            return future.complete(status);
        }

        protected boolean handleTypeMismatch(SessionResponse response) {
            Status error = Status.of(StatusCode.CLIENT_INTERNAL_ERROR)
                    .withIssues(Issue.of("Incorrect type of sessin response " + response, Issue.Severity.ERROR));
            return future.complete(error);
        }

        protected boolean handleResult(
                StatusCodesProtos.StatusIds.StatusCode code,
                List<YdbIssueMessage.IssueMessage> issues) {
            return future.complete(Status.of(StatusCode.fromProto(code), null, Issue.fromPb(issues)));
        }
    }

    private static class CreateSemaphoreMsg extends BaseStatusMsg {
        private final String name;
        private final long limit;
        private final ByteString data;

        CreateSemaphoreMsg(String name, long limit, byte[] data) {
            this.name = name;
            this.limit = limit;
            this.data = data == null ? ByteString.EMPTY : ByteString.copyFrom(data);
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setCreateSemaphore(
                    SessionRequest.CreateSemaphore.newBuilder()
                            .setName(name)
                            .setLimit(limit)
                            .setData(data)
                            .setReqId(reqId)
                            .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasCreateSemaphoreResult()) {
                return handleTypeMismatch(response);
            }
            SessionResponse.CreateSemaphoreResult result = response.getCreateSemaphoreResult();
            return handleResult(result.getStatus(), result.getIssuesList());
        }
    }

    private static class UpdateSemaphoreMsg extends BaseStatusMsg {
        private final String name;
        private final ByteString data;

        UpdateSemaphoreMsg(String name, byte[] data) {
            this.name = name;
            this.data = data == null ? ByteString.EMPTY : ByteString.copyFrom(data);
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setUpdateSemaphore(
                    SessionRequest.UpdateSemaphore.newBuilder()
                            .setName(name)
                            .setData(data)
                            .setReqId(reqId)
                            .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasUpdateSemaphoreResult()) {
                return handleTypeMismatch(response);
            }
            SessionResponse.UpdateSemaphoreResult result = response.getUpdateSemaphoreResult();
            return handleResult(result.getStatus(), result.getIssuesList());
        }
    }

    private static class DeleteSemaphoreMsg extends BaseStatusMsg {
        private final String name;
        private final boolean force;

        DeleteSemaphoreMsg(String name, boolean force) {
            this.name = name;
            this.force = force;
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setDeleteSemaphore(
                    SessionRequest.DeleteSemaphore.newBuilder()
                            .setName(name)
                            .setForce(force)
                            .setReqId(reqId)
                            .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasDeleteSemaphoreResult()) {
                return handleTypeMismatch(response);
            }
            SessionResponse.DeleteSemaphoreResult result = response.getDeleteSemaphoreResult();
            return handleResult(result.getStatus(), result.getIssuesList());
        }
    }
}
