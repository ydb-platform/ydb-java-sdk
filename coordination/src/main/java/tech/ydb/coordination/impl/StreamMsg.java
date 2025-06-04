package tech.ydb.coordination.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;

import tech.ydb.coordination.description.SemaphoreChangedEvent;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.description.SemaphoreWatcher;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
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

    public StreamMsg<?> nextMsg() {
        return null;
    }

    public boolean isIdempotent() {
        return false;
    }

    public abstract SessionRequest makeRequest(long reqId);

    protected abstract boolean handleResponse(SessionResponse response);
    protected abstract boolean handleError(Status status);

    protected Status incorrectTypeStatus(SessionResponse response, String expected) {
        String msg = "Incorrect type of response " + TextFormat.shortDebugString(response) + ", expected " + expected;
        return Status.of(StatusCode.CLIENT_INTERNAL_ERROR, Issue.of(msg, Issue.Severity.ERROR));
    }

    public static StreamMsg<Status> createSemaphore(String name, long limit, byte[] data) {
        return new CreateSemaphoreMsg(name, limit, data);
    }

    public static StreamMsg<Status> updateSemaphore(String name, byte[] data) {
        return new UpdateSemaphoreMsg(name, data);
    }

    public static StreamMsg<Status> deleteSemaphore(String name, boolean force) {
        return new DeleteSemaphoreMsg(name, force);
    }

    public static StreamMsg<Result<Boolean>> acquireSemaphore(
            String name, long count, byte[] data, boolean ephemeral, long timeoutMillis
    ) {
        return new AcquireSemaphoreMsg(name, count, timeoutMillis, ephemeral, data);
    }

    public static StreamMsg<Result<Boolean>> releaseSemaphore(String name) {
        return new ReleaseSemaphoreMsg(name);
    }

    public static StreamMsg<Result<SemaphoreDescription>> describeSemaphore(String name, DescribeSemaphoreMode mode) {
        return new DescribeSemaphoreMsg(name, mode);
    }

    public static StreamMsg<Result<SemaphoreWatcher>> watchSemaphore(
            String name, DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode) {
        return new WatchSemaphoreMsg(name, describeMode, watchMode);
    }

    private abstract static class BaseStatusMsg extends StreamMsg<Status> {
        @Override
        public boolean handleError(Status status) {
            return future.complete(status);
        }

        protected boolean handleResult(
                StatusCodesProtos.StatusIds.StatusCode code, List<YdbIssueMessage.IssueMessage> issues
        ) {
            return future.complete(Status.of(StatusCode.fromProto(code), Issue.fromPb(issues)));
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
            Status status = Status.of(StatusCode.fromProto(code), Issue.fromPb(issues));
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
        public boolean isIdempotent() {
            return true;
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
        public boolean isIdempotent() {
            return true;
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

    private static class DescribeSemaphoreMsg extends BaseResultMsg<SemaphoreDescription> {
        private final String name;
        private final DescribeSemaphoreMode describeMode;

        DescribeSemaphoreMsg(String name, DescribeSemaphoreMode describeMode) {
            this.name = name;
            this.describeMode = describeMode;
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setDescribeSemaphore(
                    SessionRequest.DescribeSemaphore.newBuilder()
                            .setName(name)
                            .setIncludeOwners(describeMode.includeOwners())
                            .setIncludeWaiters(describeMode.includeWaiters())
                            .setWatchData(false)
                            .setWatchOwners(false)
                            .setReqId(reqId)
                            .build()
            ).build();
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasDescribeSemaphoreResult()) {
                return handleError(incorrectTypeStatus(response, "describe_semaphore_result"));
            }
            SessionResponse.DescribeSemaphoreResult result = response.getDescribeSemaphoreResult();
            SemaphoreDescription desc = new SemaphoreDescription(result.getSemaphoreDescription());
            return handleResult(desc, result.getStatus(), result.getIssuesList());
        }
    }

    private static class WatchSemaphoreMsg extends BaseResultMsg<SemaphoreWatcher> {
        private final String name;
        private final DescribeSemaphoreMode describeMode;
        private final WatchSemaphoreMode watchMode;
        private final ChangedMsg changedMsg = new ChangedMsg();

        WatchSemaphoreMsg(String name, DescribeSemaphoreMode describeMode, WatchSemaphoreMode watchMode) {
            this.name = name;
            this.describeMode = describeMode;
            this.watchMode = watchMode;
        }

        @Override
        public SessionRequest makeRequest(long reqId) {
            return SessionRequest.newBuilder().setDescribeSemaphore(
                    SessionRequest.DescribeSemaphore.newBuilder()
                            .setName(name)
                            .setIncludeOwners(describeMode.includeOwners())
                            .setIncludeWaiters(describeMode.includeWaiters())
                            .setWatchData(watchMode.watchData())
                            .setWatchOwners(watchMode.watchOwners())
                            .setReqId(reqId)
                            .build()
            ).build();
        }

        @Override
        public StreamMsg<?> nextMsg() {
            return changedMsg;
        }

        @Override
        public boolean handleResponse(SessionResponse response) {
            if (!response.hasDescribeSemaphoreResult()) {
                return handleError(incorrectTypeStatus(response, "describe_semaphore_result"));
            }
            SessionResponse.DescribeSemaphoreResult result = response.getDescribeSemaphoreResult();
            SemaphoreDescription desc = new SemaphoreDescription(result.getSemaphoreDescription());
            SemaphoreWatcher watcher = new SemaphoreWatcher(desc, changedMsg.getResult());
            return handleResult(watcher, result.getStatus(), result.getIssuesList());
        }

        private class ChangedMsg extends BaseResultMsg<SemaphoreChangedEvent> {
            @Override
            public SessionRequest makeRequest(long reqId) {
                return WatchSemaphoreMsg.this.makeRequest(reqId);
            }

            @Override
            public boolean handleResponse(SessionResponse response) {
                if (!response.hasDescribeSemaphoreChanged()) {
                    return handleError(incorrectTypeStatus(response, "describe_semaphore_changed"));
                }
                SemaphoreChangedEvent event = new SemaphoreChangedEvent(response.getDescribeSemaphoreChanged());
                return handleResult(event, StatusCodesProtos.StatusIds.StatusCode.SUCCESS, Collections.emptyList());
            }
        }
    }
}
