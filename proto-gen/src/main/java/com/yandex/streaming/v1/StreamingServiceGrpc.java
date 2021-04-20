package com.yandex.streaming.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/streaming_v1.proto")
public final class StreamingServiceGrpc {

  private StreamingServiceGrpc() {}

  public static final String SERVICE_NAME = "Streaming.V1.StreamingService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.InstallQueryRequest,
      com.yandex.Streaming.StreamingProtos.InstallQueryResponse> getInstallQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InstallQuery",
      requestType = com.yandex.Streaming.StreamingProtos.InstallQueryRequest.class,
      responseType = com.yandex.Streaming.StreamingProtos.InstallQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.InstallQueryRequest,
      com.yandex.Streaming.StreamingProtos.InstallQueryResponse> getInstallQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.InstallQueryRequest, com.yandex.Streaming.StreamingProtos.InstallQueryResponse> getInstallQueryMethod;
    if ((getInstallQueryMethod = StreamingServiceGrpc.getInstallQueryMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getInstallQueryMethod = StreamingServiceGrpc.getInstallQueryMethod) == null) {
          StreamingServiceGrpc.getInstallQueryMethod = getInstallQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.Streaming.StreamingProtos.InstallQueryRequest, com.yandex.Streaming.StreamingProtos.InstallQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InstallQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.InstallQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.InstallQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("InstallQuery"))
              .build();
        }
      }
    }
    return getInstallQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DeleteQueryRequest,
      com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> getDeleteQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteQuery",
      requestType = com.yandex.Streaming.StreamingProtos.DeleteQueryRequest.class,
      responseType = com.yandex.Streaming.StreamingProtos.DeleteQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DeleteQueryRequest,
      com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> getDeleteQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DeleteQueryRequest, com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> getDeleteQueryMethod;
    if ((getDeleteQueryMethod = StreamingServiceGrpc.getDeleteQueryMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getDeleteQueryMethod = StreamingServiceGrpc.getDeleteQueryMethod) == null) {
          StreamingServiceGrpc.getDeleteQueryMethod = getDeleteQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.Streaming.StreamingProtos.DeleteQueryRequest, com.yandex.Streaming.StreamingProtos.DeleteQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.DeleteQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.DeleteQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("DeleteQuery"))
              .build();
        }
      }
    }
    return getDeleteQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ListQueriesRequest,
      com.yandex.Streaming.StreamingProtos.ListQueriesResponse> getListQueriesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListQueries",
      requestType = com.yandex.Streaming.StreamingProtos.ListQueriesRequest.class,
      responseType = com.yandex.Streaming.StreamingProtos.ListQueriesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ListQueriesRequest,
      com.yandex.Streaming.StreamingProtos.ListQueriesResponse> getListQueriesMethod() {
    io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ListQueriesRequest, com.yandex.Streaming.StreamingProtos.ListQueriesResponse> getListQueriesMethod;
    if ((getListQueriesMethod = StreamingServiceGrpc.getListQueriesMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getListQueriesMethod = StreamingServiceGrpc.getListQueriesMethod) == null) {
          StreamingServiceGrpc.getListQueriesMethod = getListQueriesMethod =
              io.grpc.MethodDescriptor.<com.yandex.Streaming.StreamingProtos.ListQueriesRequest, com.yandex.Streaming.StreamingProtos.ListQueriesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListQueries"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.ListQueriesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.ListQueriesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("ListQueries"))
              .build();
        }
      }
    }
    return getListQueriesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DescribeQueryRequest,
      com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> getDescribeQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeQuery",
      requestType = com.yandex.Streaming.StreamingProtos.DescribeQueryRequest.class,
      responseType = com.yandex.Streaming.StreamingProtos.DescribeQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DescribeQueryRequest,
      com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> getDescribeQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.DescribeQueryRequest, com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> getDescribeQueryMethod;
    if ((getDescribeQueryMethod = StreamingServiceGrpc.getDescribeQueryMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getDescribeQueryMethod = StreamingServiceGrpc.getDescribeQueryMethod) == null) {
          StreamingServiceGrpc.getDescribeQueryMethod = getDescribeQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.Streaming.StreamingProtos.DescribeQueryRequest, com.yandex.Streaming.StreamingProtos.DescribeQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.DescribeQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.DescribeQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("DescribeQuery"))
              .build();
        }
      }
    }
    return getDescribeQueryMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static StreamingServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceStub>() {
        @java.lang.Override
        public StreamingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceStub(channel, callOptions);
        }
      };
    return StreamingServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static StreamingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceBlockingStub>() {
        @java.lang.Override
        public StreamingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceBlockingStub(channel, callOptions);
        }
      };
    return StreamingServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static StreamingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<StreamingServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<StreamingServiceFutureStub>() {
        @java.lang.Override
        public StreamingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new StreamingServiceFutureStub(channel, callOptions);
        }
      };
    return StreamingServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class StreamingServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void installQuery(com.yandex.Streaming.StreamingProtos.InstallQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.InstallQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getInstallQueryMethod(), responseObserver);
    }

    /**
     */
    public void deleteQuery(com.yandex.Streaming.StreamingProtos.DeleteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteQueryMethod(), responseObserver);
    }

    /**
     */
    public void listQueries(com.yandex.Streaming.StreamingProtos.ListQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ListQueriesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListQueriesMethod(), responseObserver);
    }

    /**
     */
    public void describeQuery(com.yandex.Streaming.StreamingProtos.DescribeQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeQueryMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getInstallQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.Streaming.StreamingProtos.InstallQueryRequest,
                com.yandex.Streaming.StreamingProtos.InstallQueryResponse>(
                  this, METHODID_INSTALL_QUERY)))
          .addMethod(
            getDeleteQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.Streaming.StreamingProtos.DeleteQueryRequest,
                com.yandex.Streaming.StreamingProtos.DeleteQueryResponse>(
                  this, METHODID_DELETE_QUERY)))
          .addMethod(
            getListQueriesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.Streaming.StreamingProtos.ListQueriesRequest,
                com.yandex.Streaming.StreamingProtos.ListQueriesResponse>(
                  this, METHODID_LIST_QUERIES)))
          .addMethod(
            getDescribeQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.Streaming.StreamingProtos.DescribeQueryRequest,
                com.yandex.Streaming.StreamingProtos.DescribeQueryResponse>(
                  this, METHODID_DESCRIBE_QUERY)))
          .build();
    }
  }

  /**
   */
  public static final class StreamingServiceStub extends io.grpc.stub.AbstractAsyncStub<StreamingServiceStub> {
    private StreamingServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceStub(channel, callOptions);
    }

    /**
     */
    public void installQuery(com.yandex.Streaming.StreamingProtos.InstallQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.InstallQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getInstallQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteQuery(com.yandex.Streaming.StreamingProtos.DeleteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listQueries(com.yandex.Streaming.StreamingProtos.ListQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ListQueriesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListQueriesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeQuery(com.yandex.Streaming.StreamingProtos.DescribeQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeQueryMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class StreamingServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<StreamingServiceBlockingStub> {
    private StreamingServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.yandex.Streaming.StreamingProtos.InstallQueryResponse installQuery(com.yandex.Streaming.StreamingProtos.InstallQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getInstallQueryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.Streaming.StreamingProtos.DeleteQueryResponse deleteQuery(com.yandex.Streaming.StreamingProtos.DeleteQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteQueryMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.Streaming.StreamingProtos.ListQueriesResponse listQueries(com.yandex.Streaming.StreamingProtos.ListQueriesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListQueriesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.yandex.Streaming.StreamingProtos.DescribeQueryResponse describeQuery(com.yandex.Streaming.StreamingProtos.DescribeQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeQueryMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class StreamingServiceFutureStub extends io.grpc.stub.AbstractFutureStub<StreamingServiceFutureStub> {
    private StreamingServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected StreamingServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new StreamingServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.Streaming.StreamingProtos.InstallQueryResponse> installQuery(
        com.yandex.Streaming.StreamingProtos.InstallQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getInstallQueryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.Streaming.StreamingProtos.DeleteQueryResponse> deleteQuery(
        com.yandex.Streaming.StreamingProtos.DeleteQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteQueryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.Streaming.StreamingProtos.ListQueriesResponse> listQueries(
        com.yandex.Streaming.StreamingProtos.ListQueriesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListQueriesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.Streaming.StreamingProtos.DescribeQueryResponse> describeQuery(
        com.yandex.Streaming.StreamingProtos.DescribeQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeQueryMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_INSTALL_QUERY = 0;
  private static final int METHODID_DELETE_QUERY = 1;
  private static final int METHODID_LIST_QUERIES = 2;
  private static final int METHODID_DESCRIBE_QUERY = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final StreamingServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(StreamingServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_INSTALL_QUERY:
          serviceImpl.installQuery((com.yandex.Streaming.StreamingProtos.InstallQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.InstallQueryResponse>) responseObserver);
          break;
        case METHODID_DELETE_QUERY:
          serviceImpl.deleteQuery((com.yandex.Streaming.StreamingProtos.DeleteQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DeleteQueryResponse>) responseObserver);
          break;
        case METHODID_LIST_QUERIES:
          serviceImpl.listQueries((com.yandex.Streaming.StreamingProtos.ListQueriesRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ListQueriesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_QUERY:
          serviceImpl.describeQuery((com.yandex.Streaming.StreamingProtos.DescribeQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.DescribeQueryResponse>) responseObserver);
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

  private static abstract class StreamingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    StreamingServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.yandex.streaming.v1.StreamingV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("StreamingService");
    }
  }

  private static final class StreamingServiceFileDescriptorSupplier
      extends StreamingServiceBaseDescriptorSupplier {
    StreamingServiceFileDescriptorSupplier() {}
  }

  private static final class StreamingServiceMethodDescriptorSupplier
      extends StreamingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    StreamingServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (StreamingServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new StreamingServiceFileDescriptorSupplier())
              .addMethod(getInstallQueryMethod())
              .addMethod(getDeleteQueryMethod())
              .addMethod(getListQueriesMethod())
              .addMethod(getDescribeQueryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
