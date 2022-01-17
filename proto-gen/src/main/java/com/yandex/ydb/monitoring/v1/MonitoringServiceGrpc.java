package tech.ydb.monitoring.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_monitoring_v1.proto")
public final class MonitoringServiceGrpc {

  private MonitoringServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Monitoring.V1.MonitoringService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest,
      tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> getSelfCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SelfCheck",
      requestType = tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest.class,
      responseType = tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest,
      tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> getSelfCheckMethod() {
    io.grpc.MethodDescriptor<tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest, tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> getSelfCheckMethod;
    if ((getSelfCheckMethod = MonitoringServiceGrpc.getSelfCheckMethod) == null) {
      synchronized (MonitoringServiceGrpc.class) {
        if ((getSelfCheckMethod = MonitoringServiceGrpc.getSelfCheckMethod) == null) {
          MonitoringServiceGrpc.getSelfCheckMethod = getSelfCheckMethod =
              io.grpc.MethodDescriptor.<tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest, tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SelfCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new MonitoringServiceMethodDescriptorSupplier("SelfCheck"))
              .build();
        }
      }
    }
    return getSelfCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MonitoringServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceStub>() {
        @java.lang.Override
        public MonitoringServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MonitoringServiceStub(channel, callOptions);
        }
      };
    return MonitoringServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MonitoringServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceBlockingStub>() {
        @java.lang.Override
        public MonitoringServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MonitoringServiceBlockingStub(channel, callOptions);
        }
      };
    return MonitoringServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MonitoringServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MonitoringServiceFutureStub>() {
        @java.lang.Override
        public MonitoringServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MonitoringServiceFutureStub(channel, callOptions);
        }
      };
    return MonitoringServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class MonitoringServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Gets the health status of the database.
     * </pre>
     */
    public void selfCheck(tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSelfCheckMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSelfCheckMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest,
                tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse>(
                  this, METHODID_SELF_CHECK)))
          .build();
    }
  }

  /**
   */
  public static final class MonitoringServiceStub extends io.grpc.stub.AbstractAsyncStub<MonitoringServiceStub> {
    private MonitoringServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MonitoringServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MonitoringServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gets the health status of the database.
     * </pre>
     */
    public void selfCheck(tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSelfCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class MonitoringServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<MonitoringServiceBlockingStub> {
    private MonitoringServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MonitoringServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MonitoringServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gets the health status of the database.
     * </pre>
     */
    public tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse selfCheck(tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest request) {
      return blockingUnaryCall(
          getChannel(), getSelfCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class MonitoringServiceFutureStub extends io.grpc.stub.AbstractFutureStub<MonitoringServiceFutureStub> {
    private MonitoringServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MonitoringServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MonitoringServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Gets the health status of the database.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse> selfCheck(
        tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSelfCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SELF_CHECK = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final MonitoringServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(MonitoringServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SELF_CHECK:
          serviceImpl.selfCheck((tech.ydb.monitoring.MonitoringProtos.SelfCheckRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.monitoring.MonitoringProtos.SelfCheckResponse>) responseObserver);
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

  private static abstract class MonitoringServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    MonitoringServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.monitoring.v1.YdbMonitoringV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("MonitoringService");
    }
  }

  private static final class MonitoringServiceFileDescriptorSupplier
      extends MonitoringServiceBaseDescriptorSupplier {
    MonitoringServiceFileDescriptorSupplier() {}
  }

  private static final class MonitoringServiceMethodDescriptorSupplier
      extends MonitoringServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    MonitoringServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (MonitoringServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new MonitoringServiceFileDescriptorSupplier())
              .addMethod(getSelfCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
