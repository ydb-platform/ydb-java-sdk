package tech.ydb.coordination.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
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

    protected abstract boolean handleResponse(SessionResponse response);
    protected abstract boolean handleError(Status status);

    protected Status incorrectTypeStatus(SessionResponse response, String exptected) {
        String msg = "Incorrect type of response " + TextFormat.shortDebugString(response) + ", expected " + exptected;
        return Status.of(StatusCode.CLIENT_INTERNAL_ERROR, null, Issue.of(msg, Issue.Severity.ERROR));
    }

    public static StreamMsg<Status> newCreateSemaphoreMsg(String name, long limit, byte[] data) {
        return new CreateSemaphoreMsg(name, limit, data);
    }

    public static StreamMsg<Status> newUpdateSemaphoreMsg(String name, byte[] data) {
        return new UpdateSemaphoreMsg(name, data);
    }

    public static StreamMsg<Status> newDeleteSemaphoreMsg(String name, boolean force) {
        return new DeleteSemaphoreMsg(name, force);
    }

    public static StreamMsg<Result<Boolean>> newAcquireSemaphoreMsg(
            String name, long count, byte[] data, boolean ephemeral, long timeoutMillis
    ) {
        return new AcquireSemaphoreMsg(name, count, timeoutMillis, ephemeral, data);
    }

    public static StreamMsg<Result<Boolean>> newReleaseSemaphoreMsg(String name) {
        return new ReleaseSemaphoreMsg(name);
    }

    private abstract static class BaseStatusMsg extends StreamMsg<Status> {
        @Override
        public boolean handleError(Status status) {
            return future.complete(status);
        }

        protected boolean handleResult(
                StatusCodesProtos.StatusIds.StatusCode code, List<YdbIssueMessage.IssueMessage> issues
        ) {
            return future.complete(Status.of(StatusCode.fromProto(code), null, Issue.fromPb(issues)));
        }
    }

    private abstract static class BaseResultMsg<V> extends StreamMsg<Result<V>> {
        @Override
        public boolean handleError(Status status) {
            return future.complete(Result.fail(status));
        }

        protected boolean handleResult(
                V value, StatusCodesProtos.StatusIds.StatusCode code, List<YdbIssueMessage.IssueMessage> issues
        ) {
            Status status = Status.of(StatusCode.fromProto(code), null, Issue.fromPb(issues));
            return future.complete(status.isSuccess() ? Result.success(value, status) : Result.fail(status));
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
                return handleError(incorrectTypeStatus(response, "create_semaphore_result"));
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
                return handleError(incorrectTypeStatus(response, "update_semaphore_result"));
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
                return handleError(incorrectTypeStatus(response, "delete_semaphore_result"));
            }
            SessionResponse.DeleteSemaphoreResult result = response.getDeleteSemaphoreResult();
            return handleResult(result.getStatus(), result.getIssuesList());
        }
    }

    private static class AcquireSemaphoreMsg extends BaseResultMsg<Boolean> {
        private final String name;
        private final long count;
        private final long timeoutMillis;
        private final boolean ephemeral;
        private final ByteString data;

        AcquireSemaphoreMsg(String name, long count, long timeoutMillis, boolean ephemeral, byte[] data) {
            this.name = name;
            this.count = count;
            this.timeoutMillis = timeoutMillis;
            this.ephemeral = ephemeral;
            this.data = data == null ? ByteString.EMPTY : ByteString.copyFrom(data);
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(name)
                        .setCount(count)
                        .setTimeoutMillis(timeoutMillis)
                        .setEphemeral(ephemeral)
                        .setData(data)
                        .setReqId(reqId)
                        .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasAcquireSemaphoreResult()) {
                return handleError(incorrectTypeStatus(response, "acquire_semaphore_result"));
            }
            SessionResponse.AcquireSemaphoreResult result = response.getAcquireSemaphoreResult();
            return handleResult(result.getAcquired(), result.getStatus(), result.getIssuesList());
        }
    }

    private static class ReleaseSemaphoreMsg extends BaseResultMsg<Boolean> {
        private final String name;

        ReleaseSemaphoreMsg(String name) {
            this.name = name;
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setReleaseSemaphore(
                SessionRequest.ReleaseSemaphore.newBuilder()
                        .setName(name)
                        .setReqId(reqId)
                        .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasReleaseSemaphoreResult()) {
                return handleError(incorrectTypeStatus(response, "release_semaphore_result"));
            }
            SessionResponse.ReleaseSemaphoreResult result = response.getReleaseSemaphoreResult();
            return handleResult(result.getReleased(), result.getStatus(), result.getIssuesList());
        }
    }
}
