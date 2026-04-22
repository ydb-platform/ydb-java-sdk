package tech.ydb.topic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.WriteRequest;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.WriteResponse;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class FailableWriterInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    private final AtomicInteger initCounter = new AtomicInteger();

    private final Map<Integer, Error> initErrors = new HashMap<>();
    private final TreeMap<Long, Error> ackErrors = new TreeMap<>();
    private final TreeMap<Long, Error> sendErrors = new TreeMap<>();

    public void reset() {
        initErrors.clear();
        ackErrors.clear();
        sendErrors.clear();
        initCounter.set(0);
    }

    @Override
    public void accept(ManagedChannelBuilder<?> t) {
        t.intercept(this);
    }

    public void unavailableOnInit(int number) {
        initErrors.put(number, closeStream(Status.UNAVAILABLE));
    }

    public void badRequestOnInit(int number) {
        initErrors.put(number, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST));
    }

    public void unavailableOnAckWithSeqNo(long seqNo) {
        ackErrors.put(seqNo, closeStream(Status.UNAVAILABLE));
    }

    public void badRequestOnAckWithSeqNo(long seqNo) {
        ackErrors.put(seqNo, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST));
    }

    public void unavailableOnSendMsgWithSeqNo(long seqNo) {
        sendErrors.put(seqNo, closeStream(Status.UNAVAILABLE));
    }

    public void badSessionOnSendMsgWithSeqNo(long seqNo) {
        sendErrors.put(seqNo, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_SESSION));
    }


    @Override
    public <W, R> ClientCall<W, R> interceptCall(MethodDescriptor<W, R> method, CallOptions callOptions, Channel next) {
        return new ProxyCall<>(next.newCall(method, callOptions));
    }

    interface Error {
        boolean fail(ClientCall.Listener<FromServer> listener);
    }

    private class ProxyCall<W, R> extends ClientCall<W, R> {

        private final ClientCall<W, R> realCall;
        private volatile ProxyListener<R> proxyListener;
        private volatile boolean isClosed = false;

        ProxyCall(ClientCall<W, R> delegate) {
            this.realCall = delegate;
        }

        @Override
        public void start(Listener<R> listener, Metadata headers) {
            proxyListener = new ProxyListener<>(listener);
            realCall.start(proxyListener, headers);
        }

        @Override
        public void request(int numMessages) {
            realCall.request(numMessages);
        }

        @Override
        public void cancel(String message, Throwable cause) {
            realCall.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            realCall.halfClose();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void sendMessage(W message) {
            if (isClosed) {
                return;
            }

            Error error = null;
            if (message instanceof FromClient) {
                FromClient msg = (FromClient) message;
                if (msg.hasWriteRequest()) {
                    List<WriteRequest.MessageData> list = msg.getWriteRequest().getMessagesList();
                    long seqNo = list.get(list.size() - 1).getSeqNo();
                    NavigableMap<Long, Error> errors = sendErrors.headMap(seqNo, true);
                    if (errors.lastEntry() != null) {
                        error = errors.lastEntry().getValue();
                    }
                    errors.clear();
                }
            }

            if (error == null) {
                realCall.sendMessage(message);
                return;
            }

            isClosed = error.fail((Listener<FromServer>) proxyListener);
            if (isClosed) {
                realCall.halfClose();
            }
        }

        private class ProxyListener<R> extends Listener<R> {
            private final Listener<R> realListener;

            ProxyListener(Listener<R> realListener) {
                this.realListener = realListener;
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                if (!isClosed) {
                    realListener.onClose(status, trailers);
                }
            }

            @Override
            public void onHeaders(Metadata headers) {
                if (!isClosed) {
                    realListener.onHeaders(headers);
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onMessage(R message) {
                if (isClosed) {
                    return;
                }

                Error error = null;
                if (message instanceof FromServer) {
                    FromServer msg = (FromServer) message;
                    if (msg.hasInitResponse()) {
                        error = initErrors.get(initCounter.incrementAndGet());
                    }
                    if (msg.hasWriteResponse()) {
                        List<WriteResponse.WriteAck> acks = msg.getWriteResponse().getAcksList();
                        long lastAck = acks.get(acks.size() - 1).getSeqNo();
                        NavigableMap<Long, Error> errors = ackErrors.headMap(lastAck, true);
                        if (errors.lastEntry() != null) {
                            error = errors.lastEntry().getValue();
                        }
                        errors.clear();
                    }
                }
                if (error == null) {
                    realListener.onMessage(message);
                    return;
                }

                isClosed = error.fail((Listener<FromServer>) realListener);
                if (isClosed) {
                    realCall.halfClose();
                }
            }
        }
    }

    private static Error closeStream(Status grpcStatus) {
        return (ClientCall.Listener<FromServer> listener) -> {
            listener.onClose(grpcStatus, new Metadata());
            return true;
        };
    }

    private static Error sendError(StatusCodesProtos.StatusIds.StatusCode ydbStatus) {
        return (ClientCall.Listener<FromServer> listener) -> {
            listener.onMessage(FromServer.newBuilder().setStatus(ydbStatus).build());
            return false;
        };
    }
}

