package tech.ydb.yql_internal.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_yql_internal.proto")
public final class YqlInternalServiceGrpc {

  private YqlInternalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.YqlInternal.V0.YqlInternalService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> METHOD_EXEC_DATA_QUERY_AST =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.YqlInternal.V0.YqlInternalService", "ExecDataQueryAst"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> METHOD_EXPLAIN_DATA_QUERY_AST =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.YqlInternal.V0.YqlInternalService", "ExplainDataQueryAst"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static YqlInternalServiceStub newStub(io.grpc.Channel channel) {
    return new YqlInternalServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static YqlInternalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new YqlInternalServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static YqlInternalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new YqlInternalServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class YqlInternalServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXEC_DATA_QUERY_AST, responseObserver);
    }

    /**
     */
    public void explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXPLAIN_DATA_QUERY_AST, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_EXEC_DATA_QUERY_AST,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest,
                tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse>(
                  this, METHODID_EXEC_DATA_QUERY_AST)))
          .addMethod(
            METHOD_EXPLAIN_DATA_QUERY_AST,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest,
                tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse>(
                  this, METHODID_EXPLAIN_DATA_QUERY_AST)))
          .build();
    }
  }

  /**
   */
  public static final class YqlInternalServiceStub extends io.grpc.stub.AbstractStub<YqlInternalServiceStub> {
    private YqlInternalServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private YqlInternalServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceStub(channel, callOptions);
    }

    /**
     */
    public void execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXEC_DATA_QUERY_AST, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXPLAIN_DATA_QUERY_AST, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class YqlInternalServiceBlockingStub extends io.grpc.stub.AbstractStub<YqlInternalServiceBlockingStub> {
    private YqlInternalServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private YqlInternalServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXEC_DATA_QUERY_AST, getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXPLAIN_DATA_QUERY_AST, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class YqlInternalServiceFutureStub extends io.grpc.stub.AbstractStub<YqlInternalServiceFutureStub> {
    private YqlInternalServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private YqlInternalServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> execDataQueryAst(
        tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXEC_DATA_QUERY_AST, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> explainDataQueryAst(
        tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXPLAIN_DATA_QUERY_AST, getCallOptions()), request);
    }
  }

  private static final int METHODID_EXEC_DATA_QUERY_AST = 0;
  private static final int METHODID_EXPLAIN_DATA_QUERY_AST = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final YqlInternalServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(YqlInternalServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXEC_DATA_QUERY_AST:
          serviceImpl.execDataQueryAst((tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse>) responseObserver);
          break;
        case METHODID_EXPLAIN_DATA_QUERY_AST:
          serviceImpl.explainDataQueryAst((tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse>) responseObserver);
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

  private static final class YqlInternalServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.yql_internal.v1.YdbYqlInternal.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (YqlInternalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new YqlInternalServiceDescriptorSupplier())
              .addMethod(METHOD_EXEC_DATA_QUERY_AST)
              .addMethod(METHOD_EXPLAIN_DATA_QUERY_AST)
              .build();
        }
      }
    }
    return result;
  }
}
