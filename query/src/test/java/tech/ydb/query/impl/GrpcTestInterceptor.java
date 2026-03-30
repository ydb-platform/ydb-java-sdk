package tech.ydb.query.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import io.grpc.Attributes;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.query.YdbQuery;
import tech.ydb.proto.query.v1.QueryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTestInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    private volatile Queue<Status> overrideQueue = new ConcurrentLinkedQueue<>();
    private volatile Queue<ScriptedResponse> scriptedResponses = new ConcurrentLinkedQueue<>();

    public void reset() {
        overrideQueue = new ConcurrentLinkedQueue<>();
        scriptedResponses = new ConcurrentLinkedQueue<>();
    }

    public void addOverrideStatus(Status status) {
        overrideQueue.add(status);
    }

    public void failCreateSession(StatusCodesProtos.StatusIds.StatusCode statusCode, int attempts) {
        addScriptedResponse(QueryServiceGrpc.getCreateSessionMethod(),
                () -> YdbQuery.CreateSessionResponse.newBuilder().setStatus(statusCode).build(), attempts);
    }

    public void failExecuteQuery(StatusCodesProtos.StatusIds.StatusCode statusCode, int attempts) {
        addScriptedResponse(QueryServiceGrpc.getExecuteQueryMethod(),
                () -> YdbQuery.ExecuteQueryResponsePart.newBuilder().setStatus(statusCode).build(), attempts);
    }

    public void failCommit(StatusCodesProtos.StatusIds.StatusCode statusCode, int attempts) {
        addScriptedResponse(QueryServiceGrpc.getCommitTransactionMethod(),
                () -> YdbQuery.CommitTransactionResponse.newBuilder().setStatus(statusCode).build(), attempts);
    }

    private void addScriptedResponse(MethodDescriptor<?, ?> method, Supplier<Object> messageFactory, int attempts) {
        for (int i = 0; i < attempts; i++) {
            scriptedResponses.add(new ScriptedResponse(method, messageFactory));
        }
    }

    @Override
    public void accept(ManagedChannelBuilder<?> t) {
        t.intercept(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        ScriptedResponse scripted = scriptedResponses.peek();
        if (scripted != null && scripted.method == method) {
            scriptedResponses.poll();
            return new ErrorCall<>((RespT) scripted.messageFactory.get());
        }
        return new ProxyClientCall<>(next, overrideQueue.poll(), method, callOptions);
    }

    private static final class ScriptedResponse {
        private final MethodDescriptor<?, ?> method;
        private final Supplier<Object> messageFactory;

        private ScriptedResponse(MethodDescriptor<?, ?> method, Supplier<Object> messageFactory) {
            this.method = method;
            this.messageFactory = messageFactory;
        }
    }

    private static final class ErrorCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final RespT errorMsg;

        private ErrorCall(RespT errorMsg) {
            this.errorMsg = errorMsg;
        }

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
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

    private static class ProxyClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final ClientCall<ReqT, RespT> delegate;
        private final Status overriden;

        private ProxyClientCall(Channel channel, Status overriden, MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions) {
            this.delegate = channel.newCall(method, callOptions);
            this.overriden = overriden;
        }

        @Override
        public void request(int numMessages) {
            delegate.request(numMessages);
        }

        @Override
        public void cancel(@Nullable String message, @Nullable Throwable cause) {
            delegate.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            delegate.halfClose();
        }

        @Override
        public void setMessageCompression(boolean enabled) {
            delegate.setMessageCompression(enabled);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public Attributes getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public void start(ClientCall.Listener<RespT> listener, Metadata headers) {
            delegate.start(new ProxyListener(listener), headers);
        }

        @Override
        public void sendMessage(ReqT message) {
            delegate.sendMessage(message);
        }

        private class ProxyListener extends ClientCall.Listener<RespT> {
            private final ClientCall.Listener<RespT> delegate;

            public ProxyListener(ClientCall.Listener<RespT> delegate) {
                this.delegate = delegate;
            }


            @Override
            public void onHeaders(Metadata headers) {
                delegate.onHeaders(headers);
            }

            @Override
            public void onMessage(RespT message) {
                delegate.onMessage(message);
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                delegate.onClose(overriden != null ? overriden : status, trailers);
            }

            @Override
            public void onReady() {
                delegate.onReady();
            }
        }
    }
}
