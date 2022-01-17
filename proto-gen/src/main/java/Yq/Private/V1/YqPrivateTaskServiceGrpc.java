package Yq.Private.V1;

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
    comments = "Source: ydb/public/api/grpc/draft/yql_db_v1.proto")
public final class YqPrivateTaskServiceGrpc {

  private YqPrivateTaskServiceGrpc() {}

  public static final String SERVICE_NAME = "Yq.Private.V1.YqPrivateTaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<Yq.Private.YqPrivate.GetTaskRequest,
      Yq.Private.YqPrivate.GetTaskResponse> getGetTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTask",
      requestType = Yq.Private.YqPrivate.GetTaskRequest.class,
      responseType = Yq.Private.YqPrivate.GetTaskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Yq.Private.YqPrivate.GetTaskRequest,
      Yq.Private.YqPrivate.GetTaskResponse> getGetTaskMethod() {
    io.grpc.MethodDescriptor<Yq.Private.YqPrivate.GetTaskRequest, Yq.Private.YqPrivate.GetTaskResponse> getGetTaskMethod;
    if ((getGetTaskMethod = YqPrivateTaskServiceGrpc.getGetTaskMethod) == null) {
      synchronized (YqPrivateTaskServiceGrpc.class) {
        if ((getGetTaskMethod = YqPrivateTaskServiceGrpc.getGetTaskMethod) == null) {
          YqPrivateTaskServiceGrpc.getGetTaskMethod = getGetTaskMethod =
              io.grpc.MethodDescriptor.<Yq.Private.YqPrivate.GetTaskRequest, Yq.Private.YqPrivate.GetTaskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.GetTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.GetTaskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqPrivateTaskServiceMethodDescriptorSupplier("GetTask"))
              .build();
        }
      }
    }
    return getGetTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Yq.Private.YqPrivate.PingTaskRequest,
      Yq.Private.YqPrivate.PingTaskResponse> getPingTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PingTask",
      requestType = Yq.Private.YqPrivate.PingTaskRequest.class,
      responseType = Yq.Private.YqPrivate.PingTaskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Yq.Private.YqPrivate.PingTaskRequest,
      Yq.Private.YqPrivate.PingTaskResponse> getPingTaskMethod() {
    io.grpc.MethodDescriptor<Yq.Private.YqPrivate.PingTaskRequest, Yq.Private.YqPrivate.PingTaskResponse> getPingTaskMethod;
    if ((getPingTaskMethod = YqPrivateTaskServiceGrpc.getPingTaskMethod) == null) {
      synchronized (YqPrivateTaskServiceGrpc.class) {
        if ((getPingTaskMethod = YqPrivateTaskServiceGrpc.getPingTaskMethod) == null) {
          YqPrivateTaskServiceGrpc.getPingTaskMethod = getPingTaskMethod =
              io.grpc.MethodDescriptor.<Yq.Private.YqPrivate.PingTaskRequest, Yq.Private.YqPrivate.PingTaskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PingTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.PingTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.PingTaskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqPrivateTaskServiceMethodDescriptorSupplier("PingTask"))
              .build();
        }
      }
    }
    return getPingTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Yq.Private.YqPrivate.WriteTaskResultRequest,
      Yq.Private.YqPrivate.WriteTaskResultResponse> getWriteTaskResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteTaskResult",
      requestType = Yq.Private.YqPrivate.WriteTaskResultRequest.class,
      responseType = Yq.Private.YqPrivate.WriteTaskResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Yq.Private.YqPrivate.WriteTaskResultRequest,
      Yq.Private.YqPrivate.WriteTaskResultResponse> getWriteTaskResultMethod() {
    io.grpc.MethodDescriptor<Yq.Private.YqPrivate.WriteTaskResultRequest, Yq.Private.YqPrivate.WriteTaskResultResponse> getWriteTaskResultMethod;
    if ((getWriteTaskResultMethod = YqPrivateTaskServiceGrpc.getWriteTaskResultMethod) == null) {
      synchronized (YqPrivateTaskServiceGrpc.class) {
        if ((getWriteTaskResultMethod = YqPrivateTaskServiceGrpc.getWriteTaskResultMethod) == null) {
          YqPrivateTaskServiceGrpc.getWriteTaskResultMethod = getWriteTaskResultMethod =
              io.grpc.MethodDescriptor.<Yq.Private.YqPrivate.WriteTaskResultRequest, Yq.Private.YqPrivate.WriteTaskResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteTaskResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.WriteTaskResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.WriteTaskResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqPrivateTaskServiceMethodDescriptorSupplier("WriteTaskResult"))
              .build();
        }
      }
    }
    return getWriteTaskResultMethod;
  }

  private static volatile io.grpc.MethodDescriptor<Yq.Private.YqPrivate.NodesHealthCheckRequest,
      Yq.Private.YqPrivate.NodesHealthCheckResponse> getNodesHealthCheckMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "NodesHealthCheck",
      requestType = Yq.Private.YqPrivate.NodesHealthCheckRequest.class,
      responseType = Yq.Private.YqPrivate.NodesHealthCheckResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<Yq.Private.YqPrivate.NodesHealthCheckRequest,
      Yq.Private.YqPrivate.NodesHealthCheckResponse> getNodesHealthCheckMethod() {
    io.grpc.MethodDescriptor<Yq.Private.YqPrivate.NodesHealthCheckRequest, Yq.Private.YqPrivate.NodesHealthCheckResponse> getNodesHealthCheckMethod;
    if ((getNodesHealthCheckMethod = YqPrivateTaskServiceGrpc.getNodesHealthCheckMethod) == null) {
      synchronized (YqPrivateTaskServiceGrpc.class) {
        if ((getNodesHealthCheckMethod = YqPrivateTaskServiceGrpc.getNodesHealthCheckMethod) == null) {
          YqPrivateTaskServiceGrpc.getNodesHealthCheckMethod = getNodesHealthCheckMethod =
              io.grpc.MethodDescriptor.<Yq.Private.YqPrivate.NodesHealthCheckRequest, Yq.Private.YqPrivate.NodesHealthCheckResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "NodesHealthCheck"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.NodesHealthCheckRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  Yq.Private.YqPrivate.NodesHealthCheckResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqPrivateTaskServiceMethodDescriptorSupplier("NodesHealthCheck"))
              .build();
        }
      }
    }
    return getNodesHealthCheckMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static YqPrivateTaskServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceStub>() {
        @java.lang.Override
        public YqPrivateTaskServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqPrivateTaskServiceStub(channel, callOptions);
        }
      };
    return YqPrivateTaskServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static YqPrivateTaskServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceBlockingStub>() {
        @java.lang.Override
        public YqPrivateTaskServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqPrivateTaskServiceBlockingStub(channel, callOptions);
        }
      };
    return YqPrivateTaskServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static YqPrivateTaskServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqPrivateTaskServiceFutureStub>() {
        @java.lang.Override
        public YqPrivateTaskServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqPrivateTaskServiceFutureStub(channel, callOptions);
        }
      };
    return YqPrivateTaskServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class YqPrivateTaskServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public void getTask(Yq.Private.YqPrivate.GetTaskRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.GetTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public void pingTask(Yq.Private.YqPrivate.PingTaskRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.PingTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPingTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public void writeTaskResult(Yq.Private.YqPrivate.WriteTaskResultRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.WriteTaskResultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteTaskResultMethod(), responseObserver);
    }

    /**
     * <pre>
     *Nodes
     * </pre>
     */
    public void nodesHealthCheck(Yq.Private.YqPrivate.NodesHealthCheckRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.NodesHealthCheckResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getNodesHealthCheckMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetTaskMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Yq.Private.YqPrivate.GetTaskRequest,
                Yq.Private.YqPrivate.GetTaskResponse>(
                  this, METHODID_GET_TASK)))
          .addMethod(
            getPingTaskMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Yq.Private.YqPrivate.PingTaskRequest,
                Yq.Private.YqPrivate.PingTaskResponse>(
                  this, METHODID_PING_TASK)))
          .addMethod(
            getWriteTaskResultMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Yq.Private.YqPrivate.WriteTaskResultRequest,
                Yq.Private.YqPrivate.WriteTaskResultResponse>(
                  this, METHODID_WRITE_TASK_RESULT)))
          .addMethod(
            getNodesHealthCheckMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                Yq.Private.YqPrivate.NodesHealthCheckRequest,
                Yq.Private.YqPrivate.NodesHealthCheckResponse>(
                  this, METHODID_NODES_HEALTH_CHECK)))
          .build();
    }
  }

  /**
   */
  public static final class YqPrivateTaskServiceStub extends io.grpc.stub.AbstractAsyncStub<YqPrivateTaskServiceStub> {
    private YqPrivateTaskServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqPrivateTaskServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqPrivateTaskServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public void getTask(Yq.Private.YqPrivate.GetTaskRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.GetTaskResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public void pingTask(Yq.Private.YqPrivate.PingTaskRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.PingTaskResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPingTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public void writeTaskResult(Yq.Private.YqPrivate.WriteTaskResultRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.WriteTaskResultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteTaskResultMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *Nodes
     * </pre>
     */
    public void nodesHealthCheck(Yq.Private.YqPrivate.NodesHealthCheckRequest request,
        io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.NodesHealthCheckResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getNodesHealthCheckMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class YqPrivateTaskServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<YqPrivateTaskServiceBlockingStub> {
    private YqPrivateTaskServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqPrivateTaskServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqPrivateTaskServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public Yq.Private.YqPrivate.GetTaskResponse getTask(Yq.Private.YqPrivate.GetTaskRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public Yq.Private.YqPrivate.PingTaskResponse pingTask(Yq.Private.YqPrivate.PingTaskRequest request) {
      return blockingUnaryCall(
          getChannel(), getPingTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public Yq.Private.YqPrivate.WriteTaskResultResponse writeTaskResult(Yq.Private.YqPrivate.WriteTaskResultRequest request) {
      return blockingUnaryCall(
          getChannel(), getWriteTaskResultMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *Nodes
     * </pre>
     */
    public Yq.Private.YqPrivate.NodesHealthCheckResponse nodesHealthCheck(Yq.Private.YqPrivate.NodesHealthCheckRequest request) {
      return blockingUnaryCall(
          getChannel(), getNodesHealthCheckMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class YqPrivateTaskServiceFutureStub extends io.grpc.stub.AbstractFutureStub<YqPrivateTaskServiceFutureStub> {
    private YqPrivateTaskServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqPrivateTaskServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqPrivateTaskServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Yq.Private.YqPrivate.GetTaskResponse> getTask(
        Yq.Private.YqPrivate.GetTaskRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Yq.Private.YqPrivate.PingTaskResponse> pingTask(
        Yq.Private.YqPrivate.PingTaskRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPingTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Yq.Private.YqPrivate.WriteTaskResultResponse> writeTaskResult(
        Yq.Private.YqPrivate.WriteTaskResultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteTaskResultMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *Nodes
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<Yq.Private.YqPrivate.NodesHealthCheckResponse> nodesHealthCheck(
        Yq.Private.YqPrivate.NodesHealthCheckRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getNodesHealthCheckMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TASK = 0;
  private static final int METHODID_PING_TASK = 1;
  private static final int METHODID_WRITE_TASK_RESULT = 2;
  private static final int METHODID_NODES_HEALTH_CHECK = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final YqPrivateTaskServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(YqPrivateTaskServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TASK:
          serviceImpl.getTask((Yq.Private.YqPrivate.GetTaskRequest) request,
              (io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.GetTaskResponse>) responseObserver);
          break;
        case METHODID_PING_TASK:
          serviceImpl.pingTask((Yq.Private.YqPrivate.PingTaskRequest) request,
              (io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.PingTaskResponse>) responseObserver);
          break;
        case METHODID_WRITE_TASK_RESULT:
          serviceImpl.writeTaskResult((Yq.Private.YqPrivate.WriteTaskResultRequest) request,
              (io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.WriteTaskResultResponse>) responseObserver);
          break;
        case METHODID_NODES_HEALTH_CHECK:
          serviceImpl.nodesHealthCheck((Yq.Private.YqPrivate.NodesHealthCheckRequest) request,
              (io.grpc.stub.StreamObserver<Yq.Private.YqPrivate.NodesHealthCheckResponse>) responseObserver);
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

  private static abstract class YqPrivateTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    YqPrivateTaskServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return Yq.Private.V1.YqlDbV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("YqPrivateTaskService");
    }
  }

  private static final class YqPrivateTaskServiceFileDescriptorSupplier
      extends YqPrivateTaskServiceBaseDescriptorSupplier {
    YqPrivateTaskServiceFileDescriptorSupplier() {}
  }

  private static final class YqPrivateTaskServiceMethodDescriptorSupplier
      extends YqPrivateTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    YqPrivateTaskServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (YqPrivateTaskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new YqPrivateTaskServiceFileDescriptorSupplier())
              .addMethod(getGetTaskMethod())
              .addMethod(getPingTaskMethod())
              .addMethod(getWriteTaskResultMethod())
              .addMethod(getNodesHealthCheckMethod())
              .build();
        }
      }
    }
    return result;
  }
}
