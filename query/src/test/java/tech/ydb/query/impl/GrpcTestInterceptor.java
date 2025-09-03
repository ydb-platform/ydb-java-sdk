package tech.ydb.query.impl;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

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

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcTestInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    private volatile Queue<Status> overrideQueue = new ConcurrentLinkedQueue<>();

    public void reset() {
        overrideQueue = new ConcurrentLinkedQueue<>();
    }

    public void addOverrideStatus(Status status) {
        overrideQueue.add(status);
    }

    @Override
    public void accept(ManagedChannelBuilder<?> t) {
        t.intercept(this);
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ProxyClientCall<>(next, overrideQueue.poll(), method, callOptions);
    }

    private static class ProxyClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {
        private final ClientCall<ReqT, RespT> delegate;
        private final Status overrided;

        private ProxyClientCall(Channel channel, Status overrided, MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions) {
            this.delegate = channel.newCall(method, callOptions);
            this.overrided = overrided;
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
                delegate.onClose(overrided != null ? overrided : status, trailers);
            }

            @Override
            public void onReady() {
                delegate.onReady();
            }
        }
    }
}
