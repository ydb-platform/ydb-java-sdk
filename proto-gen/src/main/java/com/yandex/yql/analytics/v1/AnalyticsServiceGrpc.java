package com.yandex.yql.analytics.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/yql_analytics_v1.proto")
public final class AnalyticsServiceGrpc {

  private AnalyticsServiceGrpc() {}

  public static final String SERVICE_NAME = "Yql.Analytics.V1.AnalyticsService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest,
      com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> getExecuteQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteQuery",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest,
      com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> getExecuteQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest, com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> getExecuteQueryMethod;
    if ((getExecuteQueryMethod = AnalyticsServiceGrpc.getExecuteQueryMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getExecuteQueryMethod = AnalyticsServiceGrpc.getExecuteQueryMethod) == null) {
          AnalyticsServiceGrpc.getExecuteQueryMethod = getExecuteQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest, com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("ExecuteQuery"))
              .build();
        }
      }
    }
    return getExecuteQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> getGetResultInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResultInfo",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> getGetResultInfoMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest, com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> getGetResultInfoMethod;
    if ((getGetResultInfoMethod = AnalyticsServiceGrpc.getGetResultInfoMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getGetResultInfoMethod = AnalyticsServiceGrpc.getGetResultInfoMethod) == null) {
          AnalyticsServiceGrpc.getGetResultInfoMethod = getGetResultInfoMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest, com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResultInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("GetResultInfo"))
              .build();
        }
      }
    }
    return getGetResultInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> getGetResultDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResultData",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> getGetResultDataMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest, com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> getGetResultDataMethod;
    if ((getGetResultDataMethod = AnalyticsServiceGrpc.getGetResultDataMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getGetResultDataMethod = AnalyticsServiceGrpc.getGetResultDataMethod) == null) {
          AnalyticsServiceGrpc.getGetResultDataMethod = getGetResultDataMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest, com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResultData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("GetResultData"))
              .build();
        }
      }
    }
    return getGetResultDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AnalyticsServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceStub>() {
        @java.lang.Override
        public AnalyticsServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AnalyticsServiceStub(channel, callOptions);
        }
      };
    return AnalyticsServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AnalyticsServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceBlockingStub>() {
        @java.lang.Override
        public AnalyticsServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AnalyticsServiceBlockingStub(channel, callOptions);
        }
      };
    return AnalyticsServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AnalyticsServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AnalyticsServiceFutureStub>() {
        @java.lang.Override
        public AnalyticsServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AnalyticsServiceFutureStub(channel, callOptions);
        }
      };
    return AnalyticsServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class AnalyticsServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void executeQuery(com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteQueryMethod(), responseObserver);
    }

    /**
     */
    public void getResultInfo(com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetResultInfoMethod(), responseObserver);
    }

    /**
     */
    public void getResultData(com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetResultDataMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExecuteQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest,
                com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse>(
                  this, METHODID_EXECUTE_QUERY)))
          .addMethod(
            getGetResultInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest,
                com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse>(
                  this, METHODID_GET_RESULT_INFO)))
          .addMethod(
            getGetResultDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest,
                com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse>(
                  this, METHODID_GET_RESULT_DATA)))
          .build();
    }
  }

  /**
   */
  public static final class AnalyticsServiceStub extends io.grpc.stub.AbstractAsyncStub<AnalyticsServiceStub> {
    private AnalyticsServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AnalyticsServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AnalyticsServiceStub(channel, callOptions);
    }

    /**
     */
    public void executeQuery(com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getResultInfo(com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetResultInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getResultData(com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetResultDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class AnalyticsServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<AnalyticsServiceBlockingStub> {
    private AnalyticsServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AnalyticsServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AnalyticsServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse executeQuery(com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteQueryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse getResultInfo(com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetResultInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse getResultData(com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetResultDataMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class AnalyticsServiceFutureStub extends io.grpc.stub.AbstractFutureStub<AnalyticsServiceFutureStub> {
    private AnalyticsServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AnalyticsServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AnalyticsServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse> executeQuery(
        com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteQueryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse> getResultInfo(
        com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetResultInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse> getResultData(
        com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetResultDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_QUERY = 0;
  private static final int METHODID_GET_RESULT_INFO = 1;
  private static final int METHODID_GET_RESULT_DATA = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AnalyticsServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(AnalyticsServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXECUTE_QUERY:
          serviceImpl.executeQuery((com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ExecuteQueryResponse>) responseObserver);
          break;
        case METHODID_GET_RESULT_INFO:
          serviceImpl.getResultInfo((com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultInfoResponse>) responseObserver);
          break;
        case METHODID_GET_RESULT_DATA:
          serviceImpl.getResultData((com.yandex.yql.analytics.AnalyticsProtos.GetResultDataRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetResultDataResponse>) responseObserver);
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

  private static abstract class AnalyticsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AnalyticsServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.yandex.yql.analytics.v1.YqlAnalyticsV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AnalyticsService");
    }
  }

  private static final class AnalyticsServiceFileDescriptorSupplier
      extends AnalyticsServiceBaseDescriptorSupplier {
    AnalyticsServiceFileDescriptorSupplier() {}
  }

  private static final class AnalyticsServiceMethodDescriptorSupplier
      extends AnalyticsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    AnalyticsServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (AnalyticsServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AnalyticsServiceFileDescriptorSupplier())
              .addMethod(getExecuteQueryMethod())
              .addMethod(getGetResultInfoMethod())
              .addMethod(getGetResultDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
