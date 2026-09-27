package tech.ydb.topic;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.CommitOffsetRequest.PartitionCommitOffset;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.CommitOffsetResponse.PartitionCommittedOffset;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamReadMessage.FromServer;


/**
 *
 * @author Aleksandr Gorshenin
 */
public class FailableReaderInterceptor implements Consumer<ManagedChannelBuilder<?>>, ClientInterceptor {
    private final AtomicInteger initCounter = new AtomicInteger();
    private final AtomicInteger readCounter = new AtomicInteger();

    private final Map<Integer, Error> initErrors = new HashMap<>();
    private final Map<Integer, Error> readErrors = new HashMap<>();
    private final Map<Long, NavigableMap<Long, Error>> ackErrors = new ConcurrentHashMap<>();
    private final Map<Long, NavigableMap<Long, Error>> sendErrors = new ConcurrentHashMap<>();

    public void reset() {
        initErrors.clear();
        readErrors.clear();
        ackErrors.clear();
        sendErrors.clear();
        initCounter.set(0);
        readCounter.set(0);
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

    public void unavailableOnReadResponse(int number) {
        readErrors.put(number, closeStream(Status.UNAVAILABLE));
    }

    public void badRequestOnReadResponse(int number) {
        readErrors.put(number, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST));
    }

    public void unavailableOnCommitAck(long partitionID, long offset) {
        ackErrors.computeIfAbsent(partitionID, id -> new ConcurrentSkipListMap<>())
                .put(offset, closeStream(Status.UNAVAILABLE));
    }

    public void badRequestOnCommitAck(long partitionID, long offset) {
        ackErrors.computeIfAbsent(partitionID, id -> new ConcurrentSkipListMap<>())
                .put(offset, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_REQUEST));
    }

    public void unavailableOnCommitWithOffset(long partitionID, long offset) {
        sendErrors.computeIfAbsent(partitionID, id -> new ConcurrentSkipListMap<>())
                .put(offset, closeStream(Status.UNAVAILABLE));
    }

    public void badSessionOnCommitWithOffset(long partitionID, long offset) {
        sendErrors.computeIfAbsent(partitionID, id -> new ConcurrentSkipListMap<>())
                .put(offset, sendError(StatusCodesProtos.StatusIds.StatusCode.BAD_SESSION));
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
        private final Map<Long, Long> partitions = new ConcurrentHashMap<>();
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
                if (msg.hasCommitOffsetRequest()) {
                    for (PartitionCommitOffset p: msg.getCommitOffsetRequest().getCommitOffsetsList()) {
                        Long pid = partitions.getOrDefault(p.getPartitionSessionId(), -1L);
                        NavigableMap<Long, Error> local = sendErrors.get(pid);
                        if (local == null || local.isEmpty()) {
                            continue;
                        }
                        long offset = p.getOffsetsList().get(p.getOffsetsCount() - 1).getEnd();
                        Map.Entry<Long, Error> nextError = local.firstEntry();
                        if (nextError != null && nextError.getKey() <= offset) {
                            error = local.remove(nextError.getKey());
                            break;
                        }
                    }
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
                    if (msg.hasStartPartitionSessionRequest()) {
                        tech.ydb.proto.topic.YdbTopic.StreamReadMessage.PartitionSession partition =
                                msg.getStartPartitionSessionRequest().getPartitionSession();
                        partitions.put(partition.getPartitionSessionId(), partition.getPartitionId());
                    }
                    if (msg.hasInitResponse()) {
                        error = initErrors.get(initCounter.incrementAndGet());
                    }
                    if (msg.hasReadResponse()) {
                        error = readErrors.get(readCounter.incrementAndGet());
                    }
                    if (msg.hasCommitOffsetResponse()) {
                        for (PartitionCommittedOffset p: msg.getCommitOffsetResponse().getPartitionsCommittedOffsetsList()) {
                            Long pid = partitions.getOrDefault(p.getPartitionSessionId(), -1L);
                            NavigableMap<Long, Error> local = ackErrors.get(pid);
                            if (local == null || local.isEmpty()) {
                                continue;
                            }
                            long lastAck = p.getCommittedOffset();
                            Map.Entry<Long, Error> nextError = local.firstEntry();
                            if (nextError != null && nextError.getKey() <= lastAck) {
                                error = local.remove(nextError.getKey());
                                break;
                            }
                        }
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

