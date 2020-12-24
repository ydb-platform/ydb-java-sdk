package tech.ydb.scripting.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_scripting_v1.proto")
public final class ScriptingServiceGrpc {

  private ScriptingServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Scripting.V1.ScriptingService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest,
      tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> getExecuteYqlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteYql",
      requestType = tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest.class,
      responseType = tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest,
      tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> getExecuteYqlMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest, tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> getExecuteYqlMethod;
    if ((getExecuteYqlMethod = ScriptingServiceGrpc.getExecuteYqlMethod) == null) {
      synchronized (ScriptingServiceGrpc.class) {
        if ((getExecuteYqlMethod = ScriptingServiceGrpc.getExecuteYqlMethod) == null) {
          ScriptingServiceGrpc.getExecuteYqlMethod = getExecuteYqlMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest, tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteYql"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScriptingServiceMethodDescriptorSupplier("ExecuteYql"))
              .build();
        }
      }
    }
    return getExecuteYqlMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest,
      tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> getExplainYqlMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExplainYql",
      requestType = tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest.class,
      responseType = tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest,
      tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> getExplainYqlMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest, tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> getExplainYqlMethod;
    if ((getExplainYqlMethod = ScriptingServiceGrpc.getExplainYqlMethod) == null) {
      synchronized (ScriptingServiceGrpc.class) {
        if ((getExplainYqlMethod = ScriptingServiceGrpc.getExplainYqlMethod) == null) {
          ScriptingServiceGrpc.getExplainYqlMethod = getExplainYqlMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest, tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExplainYql"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ScriptingServiceMethodDescriptorSupplier("ExplainYql"))
              .build();
        }
      }
    }
    return getExplainYqlMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ScriptingServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceStub>() {
        @java.lang.Override
        public ScriptingServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScriptingServiceStub(channel, callOptions);
        }
      };
    return ScriptingServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ScriptingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceBlockingStub>() {
        @java.lang.Override
        public ScriptingServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScriptingServiceBlockingStub(channel, callOptions);
        }
      };
    return ScriptingServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ScriptingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ScriptingServiceFutureStub>() {
        @java.lang.Override
        public ScriptingServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ScriptingServiceFutureStub(channel, callOptions);
        }
      };
    return ScriptingServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ScriptingServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void executeYql(tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteYqlMethod(), responseObserver);
    }

    /**
     */
    public void explainYql(tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExplainYqlMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExecuteYqlMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest,
                tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse>(
                  this, METHODID_EXECUTE_YQL)))
          .addMethod(
            getExplainYqlMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest,
                tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse>(
                  this, METHODID_EXPLAIN_YQL)))
          .build();
    }
  }

  /**
   */
  public static final class ScriptingServiceStub extends io.grpc.stub.AbstractAsyncStub<ScriptingServiceStub> {
    private ScriptingServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScriptingServiceStub(channel, callOptions);
    }

    /**
     */
    public void executeYql(tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteYqlMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void explainYql(tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExplainYqlMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ScriptingServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ScriptingServiceBlockingStub> {
    private ScriptingServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScriptingServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse executeYql(tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteYqlMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse explainYql(tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest request) {
      return blockingUnaryCall(
          getChannel(), getExplainYqlMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ScriptingServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ScriptingServiceFutureStub> {
    private ScriptingServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ScriptingServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> executeYql(
        tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteYqlMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse> explainYql(
        tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExplainYqlMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_YQL = 0;
  private static final int METHODID_EXPLAIN_YQL = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ScriptingServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ScriptingServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXECUTE_YQL:
          serviceImpl.executeYql((tech.ydb.scripting.ScriptingProtos.ExecuteYqlRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExecuteYqlResponse>) responseObserver);
          break;
        case METHODID_EXPLAIN_YQL:
          serviceImpl.explainYql((tech.ydb.scripting.ScriptingProtos.ExplainYqlRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scripting.ScriptingProtos.ExplainYqlResponse>) responseObserver);
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

  private static abstract class ScriptingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ScriptingServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.scripting.v1.YdbScriptingV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ScriptingService");
    }
  }

  private static final class ScriptingServiceFileDescriptorSupplier
      extends ScriptingServiceBaseDescriptorSupplier {
    ScriptingServiceFileDescriptorSupplier() {}
  }

  private static final class ScriptingServiceMethodDescriptorSupplier
      extends ScriptingServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ScriptingServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ScriptingServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ScriptingServiceFileDescriptorSupplier())
              .addMethod(getExecuteYqlMethod())
              .addMethod(getExplainYqlMethod())
              .build();
        }
      }
    }
    return result;
  }
}
