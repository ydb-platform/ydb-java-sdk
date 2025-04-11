package tech.ydb.test.integration.docker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.io.ByteStreams;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Grpc;
import io.grpc.HandlerRegistry;
import io.grpc.InsecureServerCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerMethodDefinition;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class GrpcProxyServer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(GrpcProxyServer.class);
    private final ManagedChannel target;
    private final Server server;
    private final EndpointRecord endpoint;

    public GrpcProxyServer(ManagedChannel target, int port) {
        this.target = target;
        this.server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .permitKeepAliveTime(10, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .fallbackHandlerRegistry(new ProxyRegistry())
                .build();
        try {
            server.start();
            logger.info("grpc proxy server started on port {}", server.getPort());
        } catch (IOException ex) {
            logger.error("cannot start proxy server", ex);
        }

        endpoint = new EndpointRecord(InetAddress.getLoopbackAddress().getHostName(), server.getPort());
    }

    public EndpointRecord endpoint() {
        return endpoint;
    }

    @Override
    public void close() {
        server.shutdown();
        try {
            server.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            logger.error("cannot await proxy server closing", ex);
            Thread.currentThread().interrupt();
        }
    }

    private static class CallProxy<ReqT, RespT> {
        final RequestProxy serverCallListener;
        final ResponseProxy clientCallListener;

        CallProxy(ServerCall<ReqT, RespT> serverCall, ClientCall<ReqT, RespT> clientCall) {
            serverCallListener = new RequestProxy(clientCall);
            clientCallListener = new ResponseProxy(serverCall);
        }

        private class RequestProxy extends ServerCall.Listener<ReqT> {
            private final Lock clientCallLock = new ReentrantLock();
            private final ClientCall<ReqT, ?> clientCall;
            // Hold 'this' lock when accessing
            private boolean needToRequest;

            RequestProxy(ClientCall<ReqT, ?> clientCall) {
                this.clientCall = clientCall;
            }

            @Override
            public void onCancel() {
                clientCall.cancel("Server cancelled", null);
            }

            @Override
            public void onHalfClose() {
                clientCall.halfClose();
            }

            @Override
            public void onMessage(ReqT message) {
                clientCall.sendMessage(message);
                clientCallLock.lock();
                try {
                    if (clientCall.isReady()) {
                        clientCallListener.serverCall.request(1);
                    } else {
                        // The outgoing call is not ready for more requests. Stop requesting additional data and
                        // wait for it to catch up.
                        needToRequest = true;
                    }
                } finally {
                    clientCallLock.unlock();
                }
            }

            @Override
            public void onReady() {
                clientCallListener.onServerReady();
            }

            // Called from ResponseProxy, which is a different thread than the ServerCall.Listener
            // callbacks.
            void onClientReady() {
                clientCallLock.lock();
                try {
                    if (needToRequest) {
                        clientCallListener.serverCall.request(1);
                        needToRequest = false;
                    }
                } finally {
                    clientCallLock.unlock();
                }
            }
        }

        private class ResponseProxy extends ClientCall.Listener<RespT> {
            private final Lock serverCallLock = new ReentrantLock();
            private final ServerCall<?, RespT> serverCall;
            // Hold 'this' lock when accessing
            private boolean needToRequest;

            ResponseProxy(ServerCall<?, RespT> serverCall) {
                this.serverCall = serverCall;
            }

            @Override
            public void onClose(Status status, Metadata trailers) {
                serverCall.close(status, trailers);
            }

            @Override
            public void onHeaders(Metadata headers) {
                serverCall.sendHeaders(headers);
            }

            @Override
            public void onMessage(RespT message) {
                serverCall.sendMessage(message);
                serverCallLock.lock();
                try {
                    if (serverCall.isReady()) {
                        serverCallListener.clientCall.request(1);
                    } else {
                        // The incoming call is not ready for more responses. Stop requesting additional data
                        // and wait for it to catch up.
                        needToRequest = true;
                    }
                } finally {
                    serverCallLock.unlock();
                }
            }

            @Override
            public void onReady() {
                serverCallListener.onClientReady();
            }

            // Called from RequestProxy, which is a different thread than the ClientCall.Listener
            // callbacks.
            void onServerReady() {
                serverCallLock.lock();
                try {
                    if (needToRequest) {
                        serverCallListener.clientCall.request(1);
                        needToRequest = false;
                    }
                } finally {
                    serverCallLock.unlock();
                }
            }
        }
    }

    private class ProxyHandler<ReqT, RespT> implements ServerCallHandler<ReqT, RespT> {
        @Override
        public ServerCall.Listener<ReqT> startCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            ClientCall<ReqT, RespT> clientCall = target.newCall(serverCall.getMethodDescriptor(), CallOptions.DEFAULT);
            CallProxy<ReqT, RespT> proxy = new CallProxy<>(serverCall, clientCall);
            clientCall.start(proxy.clientCallListener, metadata);
            serverCall.request(1);
            clientCall.request(1);
            return proxy.serverCallListener;
        }
    }

    private class ProxyRegistry extends HandlerRegistry {
        private final ByteMarshaller marshaller = new ByteMarshaller();
        private final ProxyHandler<byte[], byte[]> handler = new ProxyHandler<>();

        @Override
        public ServerMethodDefinition<?, ?> lookupMethod(String methodName, String authority) {
            if (DiscoveryServiceGrpc.getListEndpointsMethod().getFullMethodName().equals(methodName)) {
                logger.info("use custom proxy for method {}", methodName);
                return new DiscoveryServiceProxy(endpoint).toMethodDefinition();
            }

            MethodDescriptor<byte[], byte[]> descriptor = MethodDescriptor.newBuilder(marshaller, marshaller)
                    .setFullMethodName(methodName)
                    .setType(MethodDescriptor.MethodType.UNKNOWN)
                    .build();
            return ServerMethodDefinition.create(descriptor, handler);
        }
    }

    private static class ByteMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        @Override
        public byte[] parse(InputStream stream) {
            try {
                return ByteStreams.toByteArray(stream);
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }
    }
}
