package tech.ydb.discovery.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: ydb/public/api/grpc/ydb_discovery_v1.proto")
public final class DiscoveryServiceGrpc {

  private DiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Discovery.V1.DiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest,
      tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> getListEndpointsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListEndpoints",
      requestType = tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest.class,
      responseType = tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest,
      tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> getListEndpointsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest, tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> getListEndpointsMethod;
    if ((getListEndpointsMethod = DiscoveryServiceGrpc.getListEndpointsMethod) == null) {
      synchronized (DiscoveryServiceGrpc.class) {
        if ((getListEndpointsMethod = DiscoveryServiceGrpc.getListEndpointsMethod) == null) {
          DiscoveryServiceGrpc.getListEndpointsMethod = getListEndpointsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest, tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListEndpoints"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DiscoveryServiceMethodDescriptorSupplier("ListEndpoints"))
              .build();
        }
      }
    }
    return getListEndpointsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest,
      tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> getWhoAmIMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WhoAmI",
      requestType = tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest.class,
      responseType = tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest,
      tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> getWhoAmIMethod() {
    io.grpc.MethodDescriptor<tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest, tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> getWhoAmIMethod;
    if ((getWhoAmIMethod = DiscoveryServiceGrpc.getWhoAmIMethod) == null) {
      synchronized (DiscoveryServiceGrpc.class) {
        if ((getWhoAmIMethod = DiscoveryServiceGrpc.getWhoAmIMethod) == null) {
          DiscoveryServiceGrpc.getWhoAmIMethod = getWhoAmIMethod =
              io.grpc.MethodDescriptor.<tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest, tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WhoAmI"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DiscoveryServiceMethodDescriptorSupplier("WhoAmI"))
              .build();
        }
      }
    }
    return getWhoAmIMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceStub>() {
        @java.lang.Override
        public DiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiscoveryServiceStub(channel, callOptions);
        }
      };
    return DiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public DiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return DiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DiscoveryServiceFutureStub>() {
        @java.lang.Override
        public DiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return DiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class DiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void listEndpoints(tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListEndpointsMethod(), responseObserver);
    }

    /**
     */
    public void whoAmI(tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWhoAmIMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getListEndpointsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest,
                tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse>(
                  this, METHODID_LIST_ENDPOINTS)))
          .addMethod(
            getWhoAmIMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest,
                tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse>(
                  this, METHODID_WHO_AM_I)))
          .build();
    }
  }

  /**
   */
  public static final class DiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<DiscoveryServiceStub> {
    private DiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceStub(channel, callOptions);
    }

    /**
     */
    public void listEndpoints(tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListEndpointsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void whoAmI(tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWhoAmIMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class DiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<DiscoveryServiceBlockingStub> {
    private DiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse listEndpoints(tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListEndpointsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse whoAmI(tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest request) {
      return blockingUnaryCall(
          getChannel(), getWhoAmIMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class DiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<DiscoveryServiceFutureStub> {
    private DiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DiscoveryServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse> listEndpoints(
        tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListEndpointsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse> whoAmI(
        tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWhoAmIMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_ENDPOINTS = 0;
  private static final int METHODID_WHO_AM_I = 1;

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
          serviceImpl.listEndpoints((tech.ydb.discovery.DiscoveryProtos.ListEndpointsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.ListEndpointsResponse>) responseObserver);
          break;
        case METHODID_WHO_AM_I:
          serviceImpl.whoAmI((tech.ydb.discovery.DiscoveryProtos.WhoAmIRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.discovery.DiscoveryProtos.WhoAmIResponse>) responseObserver);
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

  private static abstract class DiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.discovery.v1.YdbDiscoveryV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DiscoveryService");
    }
  }

  private static final class DiscoveryServiceFileDescriptorSupplier
      extends DiscoveryServiceBaseDescriptorSupplier {
    DiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class DiscoveryServiceMethodDescriptorSupplier
      extends DiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DiscoveryServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
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
              .setSchemaDescriptor(new DiscoveryServiceFileDescriptorSupplier())
              .addMethod(getListEndpointsMethod())
              .addMethod(getWhoAmIMethod())
              .build();
        }
      }
    }
    return result;
  }
}
