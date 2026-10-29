package tech.ydb.topic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;

/** Real protocol faults: a lost request, a received-but-hidden write ACK, or a lost commit response. */
final class MultiWriterFaultInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    enum Fault { NONE, BEFORE_STAGING, LOST_ACK, LOST_COMMIT }

    private volatile Fault fault = Fault.NONE;
    private final AtomicBoolean consumed = new AtomicBoolean();
    private final AtomicReference<Runnable> disconnect = new AtomicReference<>();
    private volatile CountDownLatch reached = new CountDownLatch(1);
    private final List<String> events = Collections.synchronizedList(new ArrayList<>());

    void reset(Fault next) {
        release();
        fault = next;
        consumed.set(false);
        reached = new CountDownLatch(1);
        events.clear();
    }

    CountDownLatch reached() {
        return reached;
    }

    List<String> events() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    void release() {
        Runnable action = disconnect.getAndSet(null);
        if (action != null) {
            action.run();
        }
    }

    @Override
    public void accept(ManagedChannelBuilder<?> builder) {
        builder.intercept(this);
    }

    @Override
    public <W, R> ClientCall<W, R> interceptCall(MethodDescriptor<W, R> method,
            CallOptions options, Channel next) {
        return new FaultCall<>(next.newCall(method, options), method.getFullMethodName());
    }

    private final class FaultCall<W, R> extends ClientCall<W, R> {
        private final ClientCall<W, R> real;
        private final String method;
        private final Object callbacks = new Object();
        private Listener<R> listener;
        private volatile String producer = "";
        private boolean closed;
        private boolean hiddenCommit;

        FaultCall(ClientCall<W, R> real, String method) {
            this.real = real;
            this.method = method;
        }

        private void fail() {
            synchronized (callbacks) {
                if (closed) {
                    return;
                }
                closed = true;
                events.add("STREAM_UNAVAILABLE producer=" + producer);
                listener.onClose(Status.UNAVAILABLE.withDescription("multiwriter parity fault"), new Metadata());
            }
            real.cancel("multiwriter parity fault", null);
        }

        @Override
        public void start(Listener<R> downstream, Metadata headers) {
            listener = downstream;
            real.start(new Listener<R>() {
                @Override
                public void onHeaders(Metadata value) {
                    synchronized (callbacks) {
                        if (!closed) {
                            listener.onHeaders(value);
                        }
                    }
                }

                @Override
                public void onMessage(R value) {
                    synchronized (callbacks) {
                        if (closed) {
                            return;
                        }
                        if (method.endsWith("/CommitTransaction") && fault == Fault.LOST_COMMIT
                                && consumed.compareAndSet(false, true)) {
                            hiddenCommit = true;
                            events.add("COMMIT_RESPONSE_LOST real=" + value);
                            reached.countDown();
                            return;
                        }
                        if (value instanceof FromServer && producer.startsWith("observed-")) {
                            FromServer message = (FromServer) value;
                            if (message.hasInitResponse()) {
                                events.add("INIT producer=" + producer + " response=" + message.getInitResponse());
                            }
                            if (message.hasWriteResponse()) {
                                events.add("REAL_ACK producer=" + producer + " response=" + message.getWriteResponse());
                                if ("observed-0".equals(producer) && fault == Fault.LOST_ACK
                                        && consumed.compareAndSet(false, true)) {
                                    disconnect.set(FaultCall.this::fail);
                                    events.add("STAGING_OR_WRITE_ACK_WITHHELD");
                                    reached.countDown();
                                    return;
                                }
                            }
                        }
                        listener.onMessage(value);
                    }
                }

                @Override
                public void onClose(Status status, Metadata trailers) {
                    synchronized (callbacks) {
                        if (!closed) {
                            closed = true;
                            listener.onClose(hiddenCommit ? Status.UNAVAILABLE.withDescription(
                                    "parity: successful commit response lost") : status, trailers);
                        }
                    }
                }

                @Override
                public void onReady() {
                    synchronized (callbacks) {
                        if (!closed) {
                            listener.onReady();
                        }
                    }
                }
            }, headers);
        }

        @Override
        public void sendMessage(W value) {
            if (value instanceof FromClient) {
                FromClient message = (FromClient) value;
                if (message.hasInitRequest()) {
                    producer = message.getInitRequest().getProducerId();
                }
                if (message.hasWriteRequest() && producer.startsWith("observed-")) {
                    events.add("SEND producer=" + producer + " request=" + message.getWriteRequest());
                    if ("observed-0".equals(producer) && fault == Fault.BEFORE_STAGING
                            && consumed.compareAndSet(false, true)) {
                        disconnect.set(this::fail);
                        events.add("BEFORE_STAGING_DROPPED");
                        reached.countDown();
                        return;
                    }
                }
            }
            real.sendMessage(value);
        }

        @Override
        public void request(int messages) {
            real.request(messages);
        }

        @Override
        public void halfClose() {
            real.halfClose();
        }

        @Override
        public void cancel(String message, Throwable cause) {
            real.cancel(message, cause);
        }
    }
}
