package tech.ydb.long_tx.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_long_tx_v1.proto")
public final class LongTxServiceGrpc {

  private LongTxServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.LongTx.V1.LongTxService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> getBeginTxMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BeginTx",
      requestType = tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest.class,
      responseType = tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> getBeginTxMethod() {
    io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest, tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> getBeginTxMethod;
    if ((getBeginTxMethod = LongTxServiceGrpc.getBeginTxMethod) == null) {
      synchronized (LongTxServiceGrpc.class) {
        if ((getBeginTxMethod = LongTxServiceGrpc.getBeginTxMethod) == null) {
          LongTxServiceGrpc.getBeginTxMethod = getBeginTxMethod =
              io.grpc.MethodDescriptor.<tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest, tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BeginTx"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LongTxServiceMethodDescriptorSupplier("BeginTx"))
              .build();
        }
      }
    }
    return getBeginTxMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> getCommitTxMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CommitTx",
      requestType = tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest.class,
      responseType = tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> getCommitTxMethod() {
    io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest, tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> getCommitTxMethod;
    if ((getCommitTxMethod = LongTxServiceGrpc.getCommitTxMethod) == null) {
      synchronized (LongTxServiceGrpc.class) {
        if ((getCommitTxMethod = LongTxServiceGrpc.getCommitTxMethod) == null) {
          LongTxServiceGrpc.getCommitTxMethod = getCommitTxMethod =
              io.grpc.MethodDescriptor.<tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest, tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CommitTx"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LongTxServiceMethodDescriptorSupplier("CommitTx"))
              .build();
        }
      }
    }
    return getCommitTxMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> getRollbackTxMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RollbackTx",
      requestType = tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest.class,
      responseType = tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest,
      tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> getRollbackTxMethod() {
    io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest, tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> getRollbackTxMethod;
    if ((getRollbackTxMethod = LongTxServiceGrpc.getRollbackTxMethod) == null) {
      synchronized (LongTxServiceGrpc.class) {
        if ((getRollbackTxMethod = LongTxServiceGrpc.getRollbackTxMethod) == null) {
          LongTxServiceGrpc.getRollbackTxMethod = getRollbackTxMethod =
              io.grpc.MethodDescriptor.<tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest, tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RollbackTx"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LongTxServiceMethodDescriptorSupplier("RollbackTx"))
              .build();
        }
      }
    }
    return getRollbackTxMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.WriteRequest,
      tech.ydb.long_tx.LongTxProtos.WriteResponse> getWriteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Write",
      requestType = tech.ydb.long_tx.LongTxProtos.WriteRequest.class,
      responseType = tech.ydb.long_tx.LongTxProtos.WriteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.WriteRequest,
      tech.ydb.long_tx.LongTxProtos.WriteResponse> getWriteMethod() {
    io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.WriteRequest, tech.ydb.long_tx.LongTxProtos.WriteResponse> getWriteMethod;
    if ((getWriteMethod = LongTxServiceGrpc.getWriteMethod) == null) {
      synchronized (LongTxServiceGrpc.class) {
        if ((getWriteMethod = LongTxServiceGrpc.getWriteMethod) == null) {
          LongTxServiceGrpc.getWriteMethod = getWriteMethod =
              io.grpc.MethodDescriptor.<tech.ydb.long_tx.LongTxProtos.WriteRequest, tech.ydb.long_tx.LongTxProtos.WriteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Write"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.WriteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.WriteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LongTxServiceMethodDescriptorSupplier("Write"))
              .build();
        }
      }
    }
    return getWriteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.ReadRequest,
      tech.ydb.long_tx.LongTxProtos.ReadResponse> getReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Read",
      requestType = tech.ydb.long_tx.LongTxProtos.ReadRequest.class,
      responseType = tech.ydb.long_tx.LongTxProtos.ReadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.ReadRequest,
      tech.ydb.long_tx.LongTxProtos.ReadResponse> getReadMethod() {
    io.grpc.MethodDescriptor<tech.ydb.long_tx.LongTxProtos.ReadRequest, tech.ydb.long_tx.LongTxProtos.ReadResponse> getReadMethod;
    if ((getReadMethod = LongTxServiceGrpc.getReadMethod) == null) {
      synchronized (LongTxServiceGrpc.class) {
        if ((getReadMethod = LongTxServiceGrpc.getReadMethod) == null) {
          LongTxServiceGrpc.getReadMethod = getReadMethod =
              io.grpc.MethodDescriptor.<tech.ydb.long_tx.LongTxProtos.ReadRequest, tech.ydb.long_tx.LongTxProtos.ReadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Read"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.ReadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.long_tx.LongTxProtos.ReadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LongTxServiceMethodDescriptorSupplier("Read"))
              .build();
        }
      }
    }
    return getReadMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LongTxServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LongTxServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LongTxServiceStub>() {
        @java.lang.Override
        public LongTxServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LongTxServiceStub(channel, callOptions);
        }
      };
    return LongTxServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LongTxServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LongTxServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LongTxServiceBlockingStub>() {
        @java.lang.Override
        public LongTxServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LongTxServiceBlockingStub(channel, callOptions);
        }
      };
    return LongTxServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LongTxServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LongTxServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LongTxServiceFutureStub>() {
        @java.lang.Override
        public LongTxServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LongTxServiceFutureStub(channel, callOptions);
        }
      };
    return LongTxServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class LongTxServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void beginTx(tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getBeginTxMethod(), responseObserver);
    }

    /**
     */
    public void commitTx(tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCommitTxMethod(), responseObserver);
    }

    /**
     */
    public void rollbackTx(tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRollbackTxMethod(), responseObserver);
    }

    /**
     */
    public void write(tech.ydb.long_tx.LongTxProtos.WriteRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.WriteResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWriteMethod(), responseObserver);
    }

    /**
     * <pre>
     *  rpc ResolveNodes(ResolveNodesRequest) returns (stream ResolveNodesResponse);
     * </pre>
     */
    public void read(tech.ydb.long_tx.LongTxProtos.ReadRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.ReadResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReadMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getBeginTxMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest,
                tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse>(
                  this, METHODID_BEGIN_TX)))
          .addMethod(
            getCommitTxMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest,
                tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse>(
                  this, METHODID_COMMIT_TX)))
          .addMethod(
            getRollbackTxMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest,
                tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse>(
                  this, METHODID_ROLLBACK_TX)))
          .addMethod(
            getWriteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.long_tx.LongTxProtos.WriteRequest,
                tech.ydb.long_tx.LongTxProtos.WriteResponse>(
                  this, METHODID_WRITE)))
          .addMethod(
            getReadMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.long_tx.LongTxProtos.ReadRequest,
                tech.ydb.long_tx.LongTxProtos.ReadResponse>(
                  this, METHODID_READ)))
          .build();
    }
  }

  /**
   */
  public static final class LongTxServiceStub extends io.grpc.stub.AbstractAsyncStub<LongTxServiceStub> {
    private LongTxServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LongTxServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LongTxServiceStub(channel, callOptions);
    }

    /**
     */
    public void beginTx(tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBeginTxMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void commitTx(tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCommitTxMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void rollbackTx(tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRollbackTxMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void write(tech.ydb.long_tx.LongTxProtos.WriteRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.WriteResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWriteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     *  rpc ResolveNodes(ResolveNodesRequest) returns (stream ResolveNodesResponse);
     * </pre>
     */
    public void read(tech.ydb.long_tx.LongTxProtos.ReadRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.ReadResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class LongTxServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<LongTxServiceBlockingStub> {
    private LongTxServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LongTxServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LongTxServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse beginTx(tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getBeginTxMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse commitTx(tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getCommitTxMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse rollbackTx(tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getRollbackTxMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.long_tx.LongTxProtos.WriteResponse write(tech.ydb.long_tx.LongTxProtos.WriteRequest request) {
      return blockingUnaryCall(
          getChannel(), getWriteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     *  rpc ResolveNodes(ResolveNodesRequest) returns (stream ResolveNodesResponse);
     * </pre>
     */
    public tech.ydb.long_tx.LongTxProtos.ReadResponse read(tech.ydb.long_tx.LongTxProtos.ReadRequest request) {
      return blockingUnaryCall(
          getChannel(), getReadMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class LongTxServiceFutureStub extends io.grpc.stub.AbstractFutureStub<LongTxServiceFutureStub> {
    private LongTxServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LongTxServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LongTxServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse> beginTx(
        tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBeginTxMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse> commitTx(
        tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCommitTxMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse> rollbackTx(
        tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRollbackTxMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.long_tx.LongTxProtos.WriteResponse> write(
        tech.ydb.long_tx.LongTxProtos.WriteRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWriteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     *  rpc ResolveNodes(ResolveNodesRequest) returns (stream ResolveNodesResponse);
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.long_tx.LongTxProtos.ReadResponse> read(
        tech.ydb.long_tx.LongTxProtos.ReadRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getReadMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_BEGIN_TX = 0;
  private static final int METHODID_COMMIT_TX = 1;
  private static final int METHODID_ROLLBACK_TX = 2;
  private static final int METHODID_WRITE = 3;
  private static final int METHODID_READ = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final LongTxServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(LongTxServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_BEGIN_TX:
          serviceImpl.beginTx((tech.ydb.long_tx.LongTxProtos.BeginTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.BeginTransactionResponse>) responseObserver);
          break;
        case METHODID_COMMIT_TX:
          serviceImpl.commitTx((tech.ydb.long_tx.LongTxProtos.CommitTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.CommitTransactionResponse>) responseObserver);
          break;
        case METHODID_ROLLBACK_TX:
          serviceImpl.rollbackTx((tech.ydb.long_tx.LongTxProtos.RollbackTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.RollbackTransactionResponse>) responseObserver);
          break;
        case METHODID_WRITE:
          serviceImpl.write((tech.ydb.long_tx.LongTxProtos.WriteRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.WriteResponse>) responseObserver);
          break;
        case METHODID_READ:
          serviceImpl.read((tech.ydb.long_tx.LongTxProtos.ReadRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.long_tx.LongTxProtos.ReadResponse>) responseObserver);
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

  private static abstract class LongTxServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LongTxServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.long_tx.v1.YdbLongTxV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LongTxService");
    }
  }

  private static final class LongTxServiceFileDescriptorSupplier
      extends LongTxServiceBaseDescriptorSupplier {
    LongTxServiceFileDescriptorSupplier() {}
  }

  private static final class LongTxServiceMethodDescriptorSupplier
      extends LongTxServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    LongTxServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (LongTxServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LongTxServiceFileDescriptorSupplier())
              .addMethod(getBeginTxMethod())
              .addMethod(getCommitTxMethod())
              .addMethod(getRollbackTxMethod())
              .addMethod(getWriteMethod())
              .addMethod(getReadMethod())
              .build();
        }
      }
    }
    return result;
  }
}
