package ru.yandex.ydb.scripting.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_scripting_v1.proto")
public final class ScriptingServiceGrpc {

  private ScriptingServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Scripting.V1.ScriptingService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest,
      ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> METHOD_EXECUTE_YQL =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scripting.V1.ScriptingService", "ExecuteYql"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ScriptingServiceStub newStub(io.grpc.Channel channel) {
    return new ScriptingServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ScriptingServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ScriptingServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ScriptingServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ScriptingServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class ScriptingServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void executeYql(ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXECUTE_YQL, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_EXECUTE_YQL,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest,
                ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse>(
                  this, METHODID_EXECUTE_YQL)))
          .build();
    }
  }

  /**
   */
  public static final class ScriptingServiceStub extends io.grpc.stub.AbstractStub<ScriptingServiceStub> {
    private ScriptingServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ScriptingServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ScriptingServiceStub(channel, callOptions);
    }

    /**
     */
    public void executeYql(ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_YQL, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ScriptingServiceBlockingStub extends io.grpc.stub.AbstractStub<ScriptingServiceBlockingStub> {
    private ScriptingServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ScriptingServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ScriptingServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse executeYql(ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXECUTE_YQL, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ScriptingServiceFutureStub extends io.grpc.stub.AbstractStub<ScriptingServiceFutureStub> {
    private ScriptingServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ScriptingServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ScriptingServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ScriptingServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse> executeYql(
        ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_YQL, getCallOptions()), request);
    }
  }

  private static final int METHODID_EXECUTE_YQL = 0;

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
          serviceImpl.executeYql((ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.scripting.ScriptingProtos.ExecuteYqlResponse>) responseObserver);
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

  private static final class ScriptingServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ru.yandex.ydb.scripting.v1.YdbScriptingV1.getDescriptor();
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
              .setSchemaDescriptor(new ScriptingServiceDescriptorSupplier())
              .addMethod(METHOD_EXECUTE_YQL)
              .build();
        }
      }
    }
    return result;
  }
}
