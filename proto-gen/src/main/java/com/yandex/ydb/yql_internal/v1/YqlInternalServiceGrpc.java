package tech.ydb.yql_internal.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_yql_internal.proto")
public final class YqlInternalServiceGrpc {

  private YqlInternalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.YqlInternal.V0.YqlInternalService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> getExecDataQueryAstMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecDataQueryAst",
      requestType = tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest.class,
      responseType = tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> getExecDataQueryAstMethod() {
    io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest, tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> getExecDataQueryAstMethod;
    if ((getExecDataQueryAstMethod = YqlInternalServiceGrpc.getExecDataQueryAstMethod) == null) {
      synchronized (YqlInternalServiceGrpc.class) {
        if ((getExecDataQueryAstMethod = YqlInternalServiceGrpc.getExecDataQueryAstMethod) == null) {
          YqlInternalServiceGrpc.getExecDataQueryAstMethod = getExecDataQueryAstMethod =
              io.grpc.MethodDescriptor.<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest, tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecDataQueryAst"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqlInternalServiceMethodDescriptorSupplier("ExecDataQueryAst"))
              .build();
        }
      }
    }
    return getExecDataQueryAstMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> getExplainDataQueryAstMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExplainDataQueryAst",
      requestType = tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest.class,
      responseType = tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest,
      tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> getExplainDataQueryAstMethod() {
    io.grpc.MethodDescriptor<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest, tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> getExplainDataQueryAstMethod;
    if ((getExplainDataQueryAstMethod = YqlInternalServiceGrpc.getExplainDataQueryAstMethod) == null) {
      synchronized (YqlInternalServiceGrpc.class) {
        if ((getExplainDataQueryAstMethod = YqlInternalServiceGrpc.getExplainDataQueryAstMethod) == null) {
          YqlInternalServiceGrpc.getExplainDataQueryAstMethod = getExplainDataQueryAstMethod =
              io.grpc.MethodDescriptor.<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest, tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExplainDataQueryAst"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YqlInternalServiceMethodDescriptorSupplier("ExplainDataQueryAst"))
              .build();
        }
      }
    }
    return getExplainDataQueryAstMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static YqlInternalServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceStub>() {
        @java.lang.Override
        public YqlInternalServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalServiceStub(channel, callOptions);
        }
      };
    return YqlInternalServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static YqlInternalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceBlockingStub>() {
        @java.lang.Override
        public YqlInternalServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalServiceBlockingStub(channel, callOptions);
        }
      };
    return YqlInternalServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static YqlInternalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YqlInternalServiceFutureStub>() {
        @java.lang.Override
        public YqlInternalServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YqlInternalServiceFutureStub(channel, callOptions);
        }
      };
    return YqlInternalServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class YqlInternalServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecDataQueryAstMethod(), responseObserver);
    }

    /**
     */
    public void explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExplainDataQueryAstMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExecDataQueryAstMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest,
                tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse>(
                  this, METHODID_EXEC_DATA_QUERY_AST)))
          .addMethod(
            getExplainDataQueryAstMethod(),
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
  public static final class YqlInternalServiceStub extends io.grpc.stub.AbstractAsyncStub<YqlInternalServiceStub> {
    private YqlInternalServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceStub(channel, callOptions);
    }

    /**
     */
    public void execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecDataQueryAstMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExplainDataQueryAstMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class YqlInternalServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<YqlInternalServiceBlockingStub> {
    private YqlInternalServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse execDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecDataQueryAstMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse explainDataQueryAst(tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request) {
      return blockingUnaryCall(
          getChannel(), getExplainDataQueryAstMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class YqlInternalServiceFutureStub extends io.grpc.stub.AbstractFutureStub<YqlInternalServiceFutureStub> {
    private YqlInternalServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YqlInternalServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YqlInternalServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstResponse> execDataQueryAst(
        tech.ydb.yql_internal.YqlInternalProtos.ExecDataQueryAstRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecDataQueryAstMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstResponse> explainDataQueryAst(
        tech.ydb.yql_internal.YqlInternalProtos.ExplainDataQueryAstRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExplainDataQueryAstMethod(), getCallOptions()), request);
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

  private static abstract class YqlInternalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    YqlInternalServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.yql_internal.v1.YdbYqlInternal.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("YqlInternalService");
    }
  }

  private static final class YqlInternalServiceFileDescriptorSupplier
      extends YqlInternalServiceBaseDescriptorSupplier {
    YqlInternalServiceFileDescriptorSupplier() {}
  }

  private static final class YqlInternalServiceMethodDescriptorSupplier
      extends YqlInternalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    YqlInternalServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (YqlInternalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new YqlInternalServiceFileDescriptorSupplier())
              .addMethod(getExecDataQueryAstMethod())
              .addMethod(getExplainDataQueryAstMethod())
              .build();
        }
      }
    }
    return result;
  }
}
