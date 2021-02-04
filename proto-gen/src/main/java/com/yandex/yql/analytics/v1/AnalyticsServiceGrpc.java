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

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> getGetConnectionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConnections",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> getGetConnectionsMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest, com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> getGetConnectionsMethod;
    if ((getGetConnectionsMethod = AnalyticsServiceGrpc.getGetConnectionsMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getGetConnectionsMethod = AnalyticsServiceGrpc.getGetConnectionsMethod) == null) {
          AnalyticsServiceGrpc.getGetConnectionsMethod = getGetConnectionsMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest, com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConnections"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("GetConnections"))
              .build();
        }
      }
    }
    return getGetConnectionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest,
      com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> getModifyConnectionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyConnections",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest,
      com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> getModifyConnectionsMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest, com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> getModifyConnectionsMethod;
    if ((getModifyConnectionsMethod = AnalyticsServiceGrpc.getModifyConnectionsMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getModifyConnectionsMethod = AnalyticsServiceGrpc.getModifyConnectionsMethod) == null) {
          AnalyticsServiceGrpc.getModifyConnectionsMethod = getModifyConnectionsMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest, com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyConnections"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("ModifyConnections"))
              .build();
        }
      }
    }
    return getModifyConnectionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> getGetQueriesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetQueries",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> getGetQueriesMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest, com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> getGetQueriesMethod;
    if ((getGetQueriesMethod = AnalyticsServiceGrpc.getGetQueriesMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getGetQueriesMethod = AnalyticsServiceGrpc.getGetQueriesMethod) == null) {
          AnalyticsServiceGrpc.getGetQueriesMethod = getGetQueriesMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest, com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetQueries"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("GetQueries"))
              .build();
        }
      }
    }
    return getGetQueriesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> getGetHistoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetHistory",
      requestType = com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest.class,
      responseType = com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest,
      com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> getGetHistoryMethod() {
    io.grpc.MethodDescriptor<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest, com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> getGetHistoryMethod;
    if ((getGetHistoryMethod = AnalyticsServiceGrpc.getGetHistoryMethod) == null) {
      synchronized (AnalyticsServiceGrpc.class) {
        if ((getGetHistoryMethod = AnalyticsServiceGrpc.getGetHistoryMethod) == null) {
          AnalyticsServiceGrpc.getGetHistoryMethod = getGetHistoryMethod =
              io.grpc.MethodDescriptor.<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest, com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetHistory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AnalyticsServiceMethodDescriptorSupplier("GetHistory"))
              .build();
        }
      }
    }
    return getGetHistoryMethod;
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

    /**
     */
    public void getConnections(com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetConnectionsMethod(), responseObserver);
    }

    /**
     */
    public void modifyConnections(com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyConnectionsMethod(), responseObserver);
    }

    /**
     */
    public void getQueries(com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetQueriesMethod(), responseObserver);
    }

    /**
     */
    public void getHistory(com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetHistoryMethod(), responseObserver);
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
          .addMethod(
            getGetConnectionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest,
                com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse>(
                  this, METHODID_GET_CONNECTIONS)))
          .addMethod(
            getModifyConnectionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest,
                com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse>(
                  this, METHODID_MODIFY_CONNECTIONS)))
          .addMethod(
            getGetQueriesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest,
                com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse>(
                  this, METHODID_GET_QUERIES)))
          .addMethod(
            getGetHistoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest,
                com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse>(
                  this, METHODID_GET_HISTORY)))
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

    /**
     */
    public void getConnections(com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetConnectionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void modifyConnections(com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyConnectionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getQueries(com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetQueriesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getHistory(com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetHistoryMethod(), getCallOptions()), request, responseObserver);
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

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse getConnections(com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetConnectionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse modifyConnections(com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyConnectionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse getQueries(com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetQueriesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse getHistory(com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetHistoryMethod(), getCallOptions(), request);
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

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse> getConnections(
        com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetConnectionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse> modifyConnections(
        com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyConnectionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse> getQueries(
        com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetQueriesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse> getHistory(
        com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetHistoryMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_QUERY = 0;
  private static final int METHODID_GET_RESULT_INFO = 1;
  private static final int METHODID_GET_RESULT_DATA = 2;
  private static final int METHODID_GET_CONNECTIONS = 3;
  private static final int METHODID_MODIFY_CONNECTIONS = 4;
  private static final int METHODID_GET_QUERIES = 5;
  private static final int METHODID_GET_HISTORY = 6;

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
        case METHODID_GET_CONNECTIONS:
          serviceImpl.getConnections((com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetConnectionsResponse>) responseObserver);
          break;
        case METHODID_MODIFY_CONNECTIONS:
          serviceImpl.modifyConnections((com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.ModifyConnectionsResponse>) responseObserver);
          break;
        case METHODID_GET_QUERIES:
          serviceImpl.getQueries((com.yandex.yql.analytics.AnalyticsProtos.GetQueriesRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetQueriesResponse>) responseObserver);
          break;
        case METHODID_GET_HISTORY:
          serviceImpl.getHistory((com.yandex.yql.analytics.AnalyticsProtos.GetHistoryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.yql.analytics.AnalyticsProtos.GetHistoryResponse>) responseObserver);
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
              .addMethod(getGetConnectionsMethod())
              .addMethod(getModifyConnectionsMethod())
              .addMethod(getGetQueriesMethod())
              .addMethod(getGetHistoryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
