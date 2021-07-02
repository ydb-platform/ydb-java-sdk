package com.yandex.yql.analytics.db.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/yql_db_v1.proto")
public final class YqlInternalTaskServiceGrpc {

  private YqlInternalTaskServiceGrpc() {}

  public static final String SERVICE_NAME = "Yql.Analytics.V1.YqlInternalTaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> getGetTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetTask",
      requestType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest.class,
      responseType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> getGetTaskMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> getGetTaskMethod;
    if ((getGetTaskMethod = YqlInternalTaskServiceGrpc.getGetTaskMethod) == null) {
      synchronized (YqlInternalTaskServiceGrpc.class) {
        if ((getGetTaskMethod = YqlInternalTaskServiceGrpc.getGetTaskMethod) == null) {
          YqlInternalTaskServiceGrpc.getGetTaskMethod = getGetTaskMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqlInternalTaskServiceMethodDescriptorSupplier("GetTask"))
              .build();
        }
      }
    }
    return getGetTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> getPingTaskMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PingTask",
      requestType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest.class,
      responseType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> getPingTaskMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> getPingTaskMethod;
    if ((getPingTaskMethod = YqlInternalTaskServiceGrpc.getPingTaskMethod) == null) {
      synchronized (YqlInternalTaskServiceGrpc.class) {
        if ((getPingTaskMethod = YqlInternalTaskServiceGrpc.getPingTaskMethod) == null) {
          YqlInternalTaskServiceGrpc.getPingTaskMethod = getPingTaskMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PingTask"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqlInternalTaskServiceMethodDescriptorSupplier("PingTask"))
              .build();
        }
      }
    }
    return getPingTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> getWriteTaskResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteTaskResult",
      requestType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest.class,
      responseType = com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest,
      com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> getWriteTaskResultMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> getWriteTaskResultMethod;
    if ((getWriteTaskResultMethod = YqlInternalTaskServiceGrpc.getWriteTaskResultMethod) == null) {
      synchronized (YqlInternalTaskServiceGrpc.class) {
        if ((getWriteTaskResultMethod = YqlInternalTaskServiceGrpc.getWriteTaskResultMethod) == null) {
          YqlInternalTaskServiceGrpc.getWriteTaskResultMethod = getWriteTaskResultMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest, com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteTaskResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqlInternalTaskServiceMethodDescriptorSupplier("WriteTaskResult"))
              .build();
        }
      }
    }
    return getWriteTaskResultMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static YqlInternalTaskServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceStub>() {
        @java.lang.Override
        public YqlInternalTaskServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalTaskServiceStub(channel, callOptions);
        }
      };
    return YqlInternalTaskServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static YqlInternalTaskServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceBlockingStub>() {
        @java.lang.Override
        public YqlInternalTaskServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalTaskServiceBlockingStub(channel, callOptions);
        }
      };
    return YqlInternalTaskServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static YqlInternalTaskServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalTaskServiceFutureStub>() {
        @java.lang.Override
        public YqlInternalTaskServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalTaskServiceFutureStub(channel, callOptions);
        }
      };
    return YqlInternalTaskServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class YqlInternalTaskServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public void getTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public void pingTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPingTaskMethod(), responseObserver);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public void writeTaskResult(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteTaskResultMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetTaskMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest,
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse>(
                  this, METHODID_GET_TASK)))
          .addMethod(
            getPingTaskMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest,
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse>(
                  this, METHODID_PING_TASK)))
          .addMethod(
            getWriteTaskResultMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest,
                com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse>(
                  this, METHODID_WRITE_TASK_RESULT)))
          .build();
    }
  }

  /**
   */
  public static final class YqlInternalTaskServiceStub extends io.grpc.stub.AbstractAsyncStub<YqlInternalTaskServiceStub> {
    private YqlInternalTaskServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalTaskServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalTaskServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public void getTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public void pingTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPingTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public void writeTaskResult(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteTaskResultMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class YqlInternalTaskServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<YqlInternalTaskServiceBlockingStub> {
    private YqlInternalTaskServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalTaskServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalTaskServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse getTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse pingTask(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest request) {
      return blockingUnaryCall(
          getChannel(), getPingTaskMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse writeTaskResult(com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest request) {
      return blockingUnaryCall(
          getChannel(), getWriteTaskResultMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class YqlInternalTaskServiceFutureStub extends io.grpc.stub.AbstractFutureStub<YqlInternalTaskServiceFutureStub> {
    private YqlInternalTaskServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalTaskServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalTaskServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * gets new task
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse> getTask(
        com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * pings new task (also can update metadata)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse> pingTask(
        com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPingTaskMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * writes rows
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse> writeTaskResult(
        com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteTaskResultMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_TASK = 0;
  private static final int METHODID_PING_TASK = 1;
  private static final int METHODID_WRITE_TASK_RESULT = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final YqlInternalTaskServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(YqlInternalTaskServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_TASK:
          serviceImpl.getTask((com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.GetTaskResponse>) responseObserver);
          break;
        case METHODID_PING_TASK:
          serviceImpl.pingTask((com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.PingTaskResponse>) responseObserver);
          break;
        case METHODID_WRITE_TASK_RESULT:
          serviceImpl.writeTaskResult((com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.internal.AnalyticsIntenalProtos.WriteTaskResultResponse>) responseObserver);
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

  private static abstract class YqlInternalTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    YqlInternalTaskServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.yandex.yql.analytics.db.v1.YqlDbV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("YqlInternalTaskService");
    }
  }

  private static final class YqlInternalTaskServiceFileDescriptorSupplier
      extends YqlInternalTaskServiceBaseDescriptorSupplier {
    YqlInternalTaskServiceFileDescriptorSupplier() {}
  }

  private static final class YqlInternalTaskServiceMethodDescriptorSupplier
      extends YqlInternalTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    YqlInternalTaskServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (YqlInternalTaskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new YqlInternalTaskServiceFileDescriptorSupplier())
              .addMethod(getGetTaskMethod())
              .addMethod(getPingTaskMethod())
              .addMethod(getWriteTaskResultMethod())
              .build();
        }
      }
    }
    return result;
  }
}
