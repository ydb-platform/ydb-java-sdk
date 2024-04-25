package tech.ydb.core.impl;


import java.util.concurrent.Executor;

import com.google.protobuf.Any;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;

import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.proto.OperationProtos;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.discovery.DiscoveryProtos;

public abstract class MockedCall<ResT, RespT> extends ClientCall<ResT, RespT> {
    private final Executor executor;

    protected MockedCall(Executor executor) {
        this.executor = executor;
    }

    protected abstract void complete(Listener<RespT> listener);

    @Override
    public void start(Listener<RespT> listener, Metadata headers) {
        executor.execute(() -> complete(listener));
    }

    @Override
    public void request(int numMessages) {
        // nothing
    }

    @Override
    public void cancel(String message, Throwable cause) {
        // nothing
    }

    @Override
    public void halfClose() {
        // nothing
    }

    @Override
    public void sendMessage(ResT message) {
        // nothing
    }

    public static <ReqT, RespT> MockedCall<ReqT, RespT> neverAnswer() {
        return neverAnswer(Runnable::run);
    }

    public static <ReqT, RespT> MockedCall<ReqT, RespT> neverAnswer(Executor executor) {
        return new MockedCall<ReqT, RespT>(executor) {
            @Override
            protected void complete(Listener<RespT> listener) { }
        };
    }

    public static <ReqT, RespT> MockedCall<ReqT, RespT> unavailable() {
        return unavailable(Runnable::run);
    }

    public static <ReqT, RespT> MockedCall<ReqT, RespT> unavailable(Executor executor) {
        return new MockedCall<ReqT, RespT>(executor) {
            @Override
            protected void complete(Listener<RespT> listener) {
                listener.onClose(Status.UNAVAILABLE, null);
            }
        };
    }

    public static abstract class WhoAmICall extends
            MockedCall<DiscoveryProtos.WhoAmIRequest, DiscoveryProtos.WhoAmIResponse> {
        protected WhoAmICall(Executor executor) {
            super(executor);
        }
    }

    public static WhoAmICall whoAmICall(String user) {
        return whoAmICall(Runnable::run, user);
    }

    public static WhoAmICall whoAmICall(Executor executor, String user) {
        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setResult(Any.pack(DiscoveryProtos.WhoAmIResult.newBuilder().setUser(user).build()))
                .setId("discovery-id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .build();


        return new WhoAmICall(executor) {
            @Override
            protected void complete(Listener<DiscoveryProtos.WhoAmIResponse> listener) {
                listener.onMessage(DiscoveryProtos.WhoAmIResponse.newBuilder().setOperation(operation).build());
                listener.onClose(Status.OK, null);
            }
        };
    }

    public static abstract class DiscoveryCall extends
            MockedCall<DiscoveryProtos.ListEndpointsRequest, DiscoveryProtos.ListEndpointsResponse> {
        protected DiscoveryCall(Executor executor) {
            super(executor);
        }
    }

    public static DiscoveryCall discovery(String selfLocation, EndpointRecord... endpoints) {
        return discovery(Runnable::run, selfLocation, endpoints);
    }

    public static DiscoveryCall discovery(Executor executor, String selfLocation, EndpointRecord... endpoints) {
        DiscoveryProtos.ListEndpointsResult.Builder builder = DiscoveryProtos.ListEndpointsResult.newBuilder();
        for (EndpointRecord e : endpoints) {
            DiscoveryProtos.EndpointInfo.Builder b = DiscoveryProtos.EndpointInfo.newBuilder();
            b.setAddress(e.getHost());
            b.setPort(e.getPort());
            b.setNodeId(e.getNodeId());
            if (e.getLocation() != null) {
                b.setLocation(b.getLocation());
            }

            builder.addEndpoints(b.build());
        }
        builder.setSelfLocation(selfLocation);

        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setResult(Any.pack(builder.build()))
                .setId("discovery-id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .build();


        return new DiscoveryCall(executor) {
            @Override
            protected void complete(Listener<DiscoveryProtos.ListEndpointsResponse> listener) {
                listener.onMessage(DiscoveryProtos.ListEndpointsResponse.newBuilder().setOperation(operation).build());
                listener.onClose(Status.OK, null);
            }
        };
    }

    public static DiscoveryCall discoveryInternalError() {
        return discoveryInternalError(Runnable::run);
    }

    public static DiscoveryCall discoveryInternalError(Executor executor) {
        OperationProtos.Operation operation = OperationProtos.Operation.newBuilder()
                .setReady(true)
                .setId("discovery-id")
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.INTERNAL_ERROR)
                .build();

        return new DiscoveryCall(executor) {
            @Override
            protected void complete(Listener<DiscoveryProtos.ListEndpointsResponse> listener) {
                listener.onMessage(DiscoveryProtos.ListEndpointsResponse.newBuilder().setOperation(operation).build());
                listener.onClose(Status.OK, null);
            }
        };
    }
}
