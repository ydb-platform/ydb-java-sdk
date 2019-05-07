package ru.yandex.persqueue;

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
    comments = "Source: kikimr/public/api/grpc/draft/persqueue.proto")
public final class PersQueueServiceGrpc {

  private PersQueueServiceGrpc() {}

  public static final String SERVICE_NAME = "NPersQueue.PersQueueService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.persqueue.Persqueue.WriteRequest,
      ru.yandex.ydb.persqueue.Persqueue.WriteResponse> METHOD_WRITE_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "NPersQueue.PersQueueService", "WriteSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.Persqueue.WriteRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.Persqueue.WriteResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.persqueue.Persqueue.ReadRequest,
      ru.yandex.ydb.persqueue.Persqueue.ReadResponse> METHOD_READ_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "NPersQueue.PersQueueService", "ReadSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.Persqueue.ReadRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.Persqueue.ReadResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersQueueServiceStub newStub(io.grpc.Channel channel) {
    return new PersQueueServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersQueueServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PersQueueServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static PersQueueServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PersQueueServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class PersQueueServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.WriteRequest> writeSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.WriteResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_WRITE_SESSION, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.ReadRequest> readSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.ReadResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_READ_SESSION, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_WRITE_SESSION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                ru.yandex.ydb.persqueue.Persqueue.WriteRequest,
                ru.yandex.ydb.persqueue.Persqueue.WriteResponse>(
                  this, METHODID_WRITE_SESSION)))
          .addMethod(
            METHOD_READ_SESSION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                ru.yandex.ydb.persqueue.Persqueue.ReadRequest,
                ru.yandex.ydb.persqueue.Persqueue.ReadResponse>(
                  this, METHODID_READ_SESSION)))
          .build();
    }
  }

  /**
   */
  public static final class PersQueueServiceStub extends io.grpc.stub.AbstractStub<PersQueueServiceStub> {
    private PersQueueServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersQueueServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersQueueServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.WriteRequest> writeSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.WriteResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_WRITE_SESSION, getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.ReadRequest> readSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.ReadResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_READ_SESSION, getCallOptions()), responseObserver);
    }
  }

  /**
   */
  public static final class PersQueueServiceBlockingStub extends io.grpc.stub.AbstractStub<PersQueueServiceBlockingStub> {
    private PersQueueServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersQueueServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PersQueueServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   */
  public static final class PersQueueServiceFutureStub extends io.grpc.stub.AbstractStub<PersQueueServiceFutureStub> {
    private PersQueueServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PersQueueServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.WriteResponse>) responseObserver);
        case METHODID_READ_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.readSession(
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.Persqueue.ReadResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class PersQueueServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ru.yandex.persqueue.PersqueueGrpc.getDescriptor();
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
              .setSchemaDescriptor(new PersQueueServiceDescriptorSupplier())
              .addMethod(METHOD_WRITE_SESSION)
              .addMethod(METHOD_READ_SESSION)
              .build();
        }
      }
    }
    return result;
  }
}
