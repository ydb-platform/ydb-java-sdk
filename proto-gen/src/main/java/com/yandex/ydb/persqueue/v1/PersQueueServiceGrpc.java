package tech.ydb.persqueue.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_persqueue_v1.proto")
public final class PersQueueServiceGrpc {

  private PersQueueServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.PersQueue.V1.PersQueueService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamingWrite",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod;
    if ((getStreamingWriteMethod = PersQueueServiceGrpc.getStreamingWriteMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getStreamingWriteMethod = PersQueueServiceGrpc.getStreamingWriteMethod) == null) {
          PersQueueServiceGrpc.getStreamingWriteMethod = getStreamingWriteMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamingWrite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("StreamingWrite"))
              .build();
        }
      }
    }
    return getStreamingWriteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage> getStreamingReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamingRead",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage> getStreamingReadMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage> getStreamingReadMethod;
    if ((getStreamingReadMethod = PersQueueServiceGrpc.getStreamingReadMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getStreamingReadMethod = PersQueueServiceGrpc.getStreamingReadMethod) == null) {
          PersQueueServiceGrpc.getStreamingReadMethod = getStreamingReadMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamingRead"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("StreamingRead"))
              .build();
        }
      }
    }
    return getStreamingReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
      tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetReadSessionsInfo",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
      tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest, tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod;
    if ((getGetReadSessionsInfoMethod = PersQueueServiceGrpc.getGetReadSessionsInfoMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getGetReadSessionsInfoMethod = PersQueueServiceGrpc.getGetReadSessionsInfoMethod) == null) {
          PersQueueServiceGrpc.getGetReadSessionsInfoMethod = getGetReadSessionsInfoMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest, tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetReadSessionsInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("GetReadSessionsInfo"))
              .build();
        }
      }
    }
    return getGetReadSessionsInfoMethod;
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
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage> streamingWrite(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamingWriteMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Creates Read Session
     * Pipeline:
     * client                  server
     *         Init(Topics, ClientId, ...)
     *        ----------------&gt;
     *         Init(SessionId)
     *        &lt;----------------
     *         read1
     *        ----------------&gt;
     *         read2
     *        ----------------&gt;
     *         assign(Topic1, Cluster, Partition1, ...) - assigns and releases are optional
     *        &lt;----------------
     *         assign(Topic2, Clutster, Partition2, ...)
     *        &lt;----------------
     *         start_read(Topic1, Partition1, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         release(Topic1, Partition1, ...)
     *        &lt;----------------
     *         released(Topic1, Partition1, ...) - only after released server will give this parittion to other session.
     *        ----------------&gt;
     *         start_read(Topic2, Partition2, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         read data(data, ...)
     *        &lt;----------------
     *         commit(cookie1)
     *        ----------------&gt;
     *         committed(cookie1)
     *        &lt;----------------
     *         issue(description, ...)
     *        &lt;----------------
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage> streamingRead(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamingReadMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetReadSessionsInfoMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamingWriteMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
                tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>(
                  this, METHODID_STREAMING_WRITE)))
          .addMethod(
            getStreamingReadMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage,
                tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage>(
                  this, METHODID_STREAMING_READ)))
          .addMethod(
            getGetReadSessionsInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
                tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>(
                  this, METHODID_GET_READ_SESSIONS_INFO)))
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
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage> streamingWrite(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamingWriteMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     **
     * Creates Read Session
     * Pipeline:
     * client                  server
     *         Init(Topics, ClientId, ...)
     *        ----------------&gt;
     *         Init(SessionId)
     *        &lt;----------------
     *         read1
     *        ----------------&gt;
     *         read2
     *        ----------------&gt;
     *         assign(Topic1, Cluster, Partition1, ...) - assigns and releases are optional
     *        &lt;----------------
     *         assign(Topic2, Clutster, Partition2, ...)
     *        &lt;----------------
     *         start_read(Topic1, Partition1, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         release(Topic1, Partition1, ...)
     *        &lt;----------------
     *         released(Topic1, Partition1, ...) - only after released server will give this parittion to other session.
     *        ----------------&gt;
     *         start_read(Topic2, Partition2, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         read data(data, ...)
     *        &lt;----------------
     *         commit(cookie1)
     *        ----------------&gt;
     *         committed(cookie1)
     *        &lt;----------------
     *         issue(description, ...)
     *        &lt;----------------
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadClientMessage> streamingRead(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamingReadMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetReadSessionsInfoMethod(), getCallOptions()), request, responseObserver);
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

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetReadSessionsInfoMethod(), getCallOptions(), request);
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

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getReadSessionsInfo(
        tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetReadSessionsInfoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_READ_SESSIONS_INFO = 0;
  private static final int METHODID_STREAMING_WRITE = 1;
  private static final int METHODID_STREAMING_READ = 2;

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
          serviceImpl.getReadSessionsInfo((tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>) responseObserver);
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
        case METHODID_STREAMING_WRITE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamingWrite(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>) responseObserver);
        case METHODID_STREAMING_READ:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamingRead(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingReadServerMessage>) responseObserver);
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
      return tech.ydb.persqueue.v1.YdbPersqueueV1.getDescriptor();
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
              .addMethod(getStreamingWriteMethod())
              .addMethod(getStreamingReadMethod())
              .addMethod(getGetReadSessionsInfoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
