package com.yandex.persqueue;

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
    comments = "Source: kikimr/public/api/grpc/draft/persqueue.proto")
public final class PersQueueServiceGrpc {

  private PersQueueServiceGrpc() {}

  public static final String SERVICE_NAME = "NPersQueue.PersQueueService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.WriteRequest,
      tech.ydb.persqueue.Persqueue.WriteResponse> getWriteSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteSession",
      requestType = tech.ydb.persqueue.Persqueue.WriteRequest.class,
      responseType = tech.ydb.persqueue.Persqueue.WriteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.WriteRequest,
      tech.ydb.persqueue.Persqueue.WriteResponse> getWriteSessionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.WriteRequest, tech.ydb.persqueue.Persqueue.WriteResponse> getWriteSessionMethod;
    if ((getWriteSessionMethod = PersQueueServiceGrpc.getWriteSessionMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getWriteSessionMethod = PersQueueServiceGrpc.getWriteSessionMethod) == null) {
          PersQueueServiceGrpc.getWriteSessionMethod = getWriteSessionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.Persqueue.WriteRequest, tech.ydb.persqueue.Persqueue.WriteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteSession"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.Persqueue.WriteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.Persqueue.WriteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("WriteSession"))
              .build();
        }
      }
    }
    return getWriteSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.ReadRequest,
      tech.ydb.persqueue.Persqueue.ReadResponse> getReadSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReadSession",
      requestType = tech.ydb.persqueue.Persqueue.ReadRequest.class,
      responseType = tech.ydb.persqueue.Persqueue.ReadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.ReadRequest,
      tech.ydb.persqueue.Persqueue.ReadResponse> getReadSessionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.Persqueue.ReadRequest, tech.ydb.persqueue.Persqueue.ReadResponse> getReadSessionMethod;
    if ((getReadSessionMethod = PersQueueServiceGrpc.getReadSessionMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getReadSessionMethod = PersQueueServiceGrpc.getReadSessionMethod) == null) {
          PersQueueServiceGrpc.getReadSessionMethod = getReadSessionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.Persqueue.ReadRequest, tech.ydb.persqueue.Persqueue.ReadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReadSession"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.Persqueue.ReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.Persqueue.ReadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("ReadSession"))
              .build();
        }
      }
    }
    return getReadSessionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersQueueServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceStub>() {
        @java.lang.Override
        public PersQueueServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceStub(channel, callOptions);
        }
      };
    return PersQueueServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersQueueServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceBlockingStub>() {
        @java.lang.Override
        public PersQueueServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceBlockingStub(channel, callOptions);
        }
      };
    return PersQueueServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PersQueueServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceFutureStub>() {
        @java.lang.Override
        public PersQueueServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceFutureStub(channel, callOptions);
        }
      };
    return PersQueueServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PersQueueServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.WriteRequest> writeSession(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.WriteResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getWriteSessionMethod(), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.ReadRequest> readSession(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.ReadResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(getReadSessionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getWriteSessionMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.Persqueue.WriteRequest,
                tech.ydb.persqueue.Persqueue.WriteResponse>(
                  this, METHODID_WRITE_SESSION)))
          .addMethod(
            getReadSessionMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.Persqueue.ReadRequest,
                tech.ydb.persqueue.Persqueue.ReadResponse>(
                  this, METHODID_READ_SESSION)))
          .build();
    }
  }

  /**
   */
  public static final class PersQueueServiceStub extends io.grpc.stub.AbstractAsyncStub<PersQueueServiceStub> {
    private PersQueueServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.WriteRequest> writeSession(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.WriteResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getWriteSessionMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.ReadRequest> readSession(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.ReadResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getReadSessionMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class PersQueueServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<PersQueueServiceBlockingStub> {
    private PersQueueServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class PersQueueServiceFutureStub extends io.grpc.stub.AbstractFutureStub<PersQueueServiceFutureStub> {
    private PersQueueServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_WRITE_SESSION = 0;
  private static final int METHODID_READ_SESSION = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PersQueueServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PersQueueServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_WRITE_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.writeSession(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.WriteResponse>) responseObserver);
        case METHODID_READ_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.readSession(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.Persqueue.ReadResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PersQueueServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PersQueueServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.yandex.persqueue.PersqueueGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PersQueueService");
    }
  }

  private static final class PersQueueServiceFileDescriptorSupplier
      extends PersQueueServiceBaseDescriptorSupplier {
    PersQueueServiceFileDescriptorSupplier() {}
  }

  private static final class PersQueueServiceMethodDescriptorSupplier
      extends PersQueueServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PersQueueServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PersQueueServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PersQueueServiceFileDescriptorSupplier())
              .addMethod(getWriteSessionMethod())
              .addMethod(getReadSessionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
