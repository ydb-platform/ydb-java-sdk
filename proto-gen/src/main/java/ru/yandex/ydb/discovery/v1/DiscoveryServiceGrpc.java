package ru.yandex.ydb.discovery.v1;

import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: kikimr/public/api/grpc/ydb_discovery_v1.proto")
public final class DiscoveryServiceGrpc {

  private DiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Discovery.V1.DiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest,
      ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> METHOD_LIST_ENDPOINTS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Discovery.V1.DiscoveryService", "ListEndpoints"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DiscoveryServiceStub newStub(io.grpc.Channel channel) {
    return new DiscoveryServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DiscoveryServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static DiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DiscoveryServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class DiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void listEndpoints(ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_ENDPOINTS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_LIST_ENDPOINTS,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest,
                ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse>(
                  this, METHODID_LIST_ENDPOINTS)))
          .build();
    }
  }

  /**
   */
  public static final class DiscoveryServiceStub extends io.grpc.stub.AbstractStub<DiscoveryServiceStub> {
    private DiscoveryServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DiscoveryServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public void listEndpoints(ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_ENDPOINTS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class DiscoveryServiceBlockingStub extends io.grpc.stub.AbstractStub<DiscoveryServiceBlockingStub> {
    private DiscoveryServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DiscoveryServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse listEndpoints(ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_ENDPOINTS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class DiscoveryServiceFutureStub extends io.grpc.stub.AbstractStub<DiscoveryServiceFutureStub> {
    private DiscoveryServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DiscoveryServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> listEndpoints(
        ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_ENDPOINTS, getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_ENDPOINTS = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DiscoveryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_LIST_ENDPOINTS:
          serviceImpl.listEndpoints((ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.discovery.DiscoveryProtos.ListEndpointsResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class DiscoveryServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ru.yandex.ydb.discovery.v1.YdbDiscoveryV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DiscoveryServiceDescriptorSupplier())
              .addMethod(METHOD_LIST_ENDPOINTS)
              .build();
        }
      }
    }
    return result;
  }
}
