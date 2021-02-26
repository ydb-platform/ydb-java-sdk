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
  private static volatile io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest,
      com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> getExecuteQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteQuery",
      requestType = com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest.class,
      responseType = com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest,
      com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> getExecuteQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest, com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> getExecuteQueryMethod;
    if ((getExecuteQueryMethod = StreamingServiceGrpc.getExecuteQueryMethod) == null) {
      synchronized (StreamingServiceGrpc.class) {
        if ((getExecuteQueryMethod = StreamingServiceGrpc.getExecuteQueryMethod) == null) {
          StreamingServiceGrpc.getExecuteQueryMethod = getExecuteQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest, com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new StreamingServiceMethodDescriptorSupplier("ExecuteQuery"))
              .build();
        }
      }
    }
    return getExecuteQueryMethod;
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
    public void executeQuery(com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteQueryMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExecuteQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest,
                com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse>(
                  this, METHODID_EXECUTE_QUERY)))
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
    public void executeQuery(com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteQueryMethod(), getCallOptions()), request, responseObserver);
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
    public com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse executeQuery(com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteQueryMethod(), getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse> executeQuery(
        com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteQueryMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_QUERY = 0;

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
        case METHODID_EXECUTE_QUERY:
          serviceImpl.executeQuery((com.yandex.Streaming.StreamingProtos.ExecuteQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.Streaming.StreamingProtos.ExecuteQueryResponse>) responseObserver);
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
              .addMethod(getExecuteQueryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
