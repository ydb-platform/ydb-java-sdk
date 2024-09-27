package tech.ydb.core.impl.pool;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.grpc.GrpcTransportBuilder;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ManagedChannelMock extends ManagedChannel {
    private static final Logger logger = LoggerFactory.getLogger(ManagedChannelMock.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(1);
    private final BlockingQueue<ConnectivityState> nextStates = new LinkedBlockingDeque<>();
    private final Object sync = new Object();

    private volatile ConnectivityState state;
    private Runnable listener = null;

    public ManagedChannelMock(ConnectivityState state) {
        this.state = state;
    }

    public ManagedChannelMock nextStates(ConnectivityState... states) {
        nextStates.addAll(Arrays.asList(states));
        return this;
    }

    private void requestUpdate() {
        executor.submit(() -> {
            ConnectivityState next = nextStates.poll();
            if (next == null) {
                return;
            }

            logger.trace("next mock state {}", next);

            synchronized (sync) {
                this.state = next;
                if (this.listener != null) {
                    Runnable callback = this.listener;
                    logger.trace("call listener {}", callback.hashCode());
                    this.listener = null;
                    callback.run();
                }
            }
        });
    }

    @Override
    public ManagedChannel shutdown() {
        executor.shutdown();
        return this;
    }

    @Override
    public ManagedChannel shutdownNow() {
        executor.shutdownNow();
        return this;
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public String authority() {
        return "MOCKED";
    }

    @Override
    public ConnectivityState getState(boolean requestConnection) {
        synchronized (sync) {
            logger.trace("get state {} with request {}", state, requestConnection);
            if (requestConnection) {
                requestUpdate();
            }
            return state;
        }
    }

    @Override
    public void notifyWhenStateChanged(ConnectivityState source, Runnable callback) {
        synchronized (sync) {
            logger.trace("notify of changes for state {} with current {} and callback {}",
                    source, state, callback.hashCode());
            if (source != state) {
                callback.run();
            } else {
                this.listener = callback;
            }
        }
        requestUpdate();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
            MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static ManagedChannelMock good() {
        return new ManagedChannelMock(ConnectivityState.IDLE)
                .nextStates(ConnectivityState.CONNECTING, ConnectivityState.READY);
    }

    public static ManagedChannelMock wrongShutdown() {
        return new ManagedChannelMock(ConnectivityState.IDLE) {
            {
                nextStates(ConnectivityState.CONNECTING, ConnectivityState.READY);
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
                super.awaitTermination(timeout, unit);
                return false;
            }
        };
    }

    public static ManagedChannelFactory.Builder MOCKED = (GrpcTransportBuilder builder) -> new ManagedChannelFactory() {
        @Override
        public ManagedChannel newManagedChannel(String host, int port, String authority) {
            return good();
        }

        @Override
        public long getConnectTimeoutMs() {
            return builder.getConnectTimeoutMillis();
        }
    };
}
