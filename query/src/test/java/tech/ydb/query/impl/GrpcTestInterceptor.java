package tech.ydb.query.impl;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import tech.ydb.core.StatusCode;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;

public class GrpcTestInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    private static final Queue<StatusCode> CREATE_SESSION = new ConcurrentLinkedQueue<>();
    private static final Queue<StatusCode> EXECUTE_QUERY = new ConcurrentLinkedQueue<>();
    private static final Queue<StatusCode> COMMIT_TX = new ConcurrentLinkedQueue<>();
    private static final Queue<Status> GRPC_CALLS = new ConcurrentLinkedQueue<>();

    public static void reset() {
        CREATE_SESSION.clear();
        EXECUTE_QUERY.clear();
        COMMIT_TX.clear();
        GRPC_CALLS.clear();
    }

    public static void nextGrpcCall(Status status) {
        GRPC_CALLS.add(status);
    }

    public static void nextCreateSession(StatusCode... codes) {
        CREATE_SESSION.addAll(Arrays.asList(codes));
    }

    public static void nextExecuteQuery(StatusCode... codes) {
        EXECUTE_QUERY.addAll(Arrays.asList(codes));
    }

    public static void nextCommitTx(StatusCode... codes) {
        COMMIT_TX.addAll(Arrays.asList(codes));
    }

    private static StatusCodesProtos.StatusIds.StatusCode toPb(StatusCode code) {
        switch (code) {
            case ABORTED: return StatusCodesProtos.StatusIds.StatusCode.ABORTED;
            case BAD_SESSION: return StatusCodesProtos.StatusIds.StatusCode.BAD_SESSION;
            case BAD_REQUEST: return StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST;
            case UNDETERMINED: return StatusCodesProtos.StatusIds.StatusCode.UNDETERMINED;
            default:
                throw new IllegalArgumentException("Cannot map code " + code);
        }
    }

    @Override
    public void accept(ManagedChannelBuilder<?> t) {
        t.intercept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel next) {
        Status grpc = GRPC_CALLS.poll();
        if (grpc != null) {
            return new FailCall<>(grpc);
        }

        if (method == QueryServiceGrpc.getCreateSessionMethod()) {
            StatusCode status = CREATE_SESSION.poll();
            if (status != null && status != StatusCode.SUCCESS) {
                RespT resp = (RespT) YdbQuery.CreateSessionResponse.newBuilder().setStatus(toPb(status)).build();
                return new ErrorCall<>(resp);
            }
        }

        if (method == QueryServiceGrpc.getExecuteQueryMethod()) {
            StatusCode status = EXECUTE_QUERY.poll();
            if (status != null && status != StatusCode.SUCCESS) {
                RespT resp = (RespT) YdbQuery.ExecuteQueryResponsePart.newBuilder().setStatus(toPb(status)).build();
                return new ErrorCall<>(resp);
            }
        }

        if (method == QueryServiceGrpc.getCommitTransactionMethod()) {
            StatusCode status = COMMIT_TX.poll();
            if (status != null && status != StatusCode.SUCCESS) {
                RespT resp = (RespT) YdbQuery.CommitTransactionResponse.newBuilder().setStatus(toPb(status)).build();
                return new ErrorCall<>(resp);
            }
        }

        return next.newCall(method, callOptions);
    }

    private static class ErrorCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final RespT errorMsg;

        public ErrorCall(RespT errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public void start(ClientCall.Listener<RespT> listener, Metadata headers) {
            ForkJoinPool.commonPool().execute(() -> {
                listener.onMessage(errorMsg);
                listener.onClose(Status.OK, new Metadata());
            });
        }

        @Override
        public void request(int numMessages) { }

        @Override
        public void cancel(String message, Throwable cause) { }

        @Override
        public void halfClose() { }

        @Override
        public void sendMessage(ReqT message) { }
    }

    private static class FailCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final Status code;

        public FailCall(Status code) {
            this.code = code;
        }

        @Override
        public void start(ClientCall.Listener<RespT> listener, Metadata headers) {
            ForkJoinPool.commonPool().execute(() -> {
                listener.onClose(code, new Metadata());
            });
        }

        @Override
        public void request(int numMessages) { }

        @Override
        public void cancel(String message, Throwable cause) { }

        @Override
        public void halfClose() { }

        @Override
        public void sendMessage(ReqT message) { }
    }
}
