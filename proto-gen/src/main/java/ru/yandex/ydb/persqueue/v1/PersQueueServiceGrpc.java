package ru.yandex.ydb.persqueue.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_persqueue_v1.proto")
public final class PersQueueServiceGrpc {

  private PersQueueServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.PersQueue.V1.PersQueueService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionRequest,
      ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse> METHOD_CREATE_WRITE_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "Ydb.PersQueue.V1.PersQueueService", "CreateWriteSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionRequest,
      ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse> METHOD_CREATE_READ_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "Ydb.PersQueue.V1.PersQueueService", "CreateReadSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
      ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> METHOD_GET_READ_SESSIONS_INFO =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.PersQueue.V1.PersQueueService", "GetReadSessionsInfo"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse.getDefaultInstance()));

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
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionRequest> createWriteSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_CREATE_WRITE_SESSION, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionRequest> createReadSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_CREATE_READ_SESSION, responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_READ_SESSIONS_INFO, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_WRITE_SESSION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionRequest,
                ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse>(
                  this, METHODID_CREATE_WRITE_SESSION)))
          .addMethod(
            METHOD_CREATE_READ_SESSION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionRequest,
                ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse>(
                  this, METHODID_CREATE_READ_SESSION)))
          .addMethod(
            METHOD_GET_READ_SESSIONS_INFO,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
                ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>(
                  this, METHODID_GET_READ_SESSIONS_INFO)))
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
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionRequest> createWriteSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_CREATE_WRITE_SESSION, getCallOptions()), responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionRequest> createReadSession(
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_CREATE_READ_SESSION, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_READ_SESSIONS_INFO, getCallOptions()), request, responseObserver);
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

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse getReadSessionsInfo(ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_READ_SESSIONS_INFO, getCallOptions(), request);
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

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getReadSessionsInfo(
        ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_READ_SESSIONS_INFO, getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_READ_SESSIONS_INFO = 0;
  private static final int METHODID_CREATE_WRITE_SESSION = 1;
  private static final int METHODID_CREATE_READ_SESSION = 2;

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
        case METHODID_GET_READ_SESSIONS_INFO:
          serviceImpl.getReadSessionsInfo((ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>) responseObserver);
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
        case METHODID_CREATE_WRITE_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.createWriteSession(
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.WriteSessionResponse>) responseObserver);
        case METHODID_CREATE_READ_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.createReadSession(
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.persqueue.YdbPersqueueV1.ReadSessionResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class PersQueueServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ru.yandex.ydb.persqueue.v1.YdbPersqueueV1.getDescriptor();
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
              .addMethod(METHOD_CREATE_WRITE_SESSION)
              .addMethod(METHOD_CREATE_READ_SESSION)
              .addMethod(METHOD_GET_READ_SESSIONS_INFO)
              .build();
        }
      }
    }
    return result;
  }
}
