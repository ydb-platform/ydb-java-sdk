package tech.ydb.operation.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_operation_v1.proto")
public final class OperationServiceGrpc {

  private OperationServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Operation.V1.OperationService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.OperationProtos.GetOperationRequest,
      tech.ydb.OperationProtos.GetOperationResponse> getGetOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetOperation",
      requestType = tech.ydb.OperationProtos.GetOperationRequest.class,
      responseType = tech.ydb.OperationProtos.GetOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.OperationProtos.GetOperationRequest,
      tech.ydb.OperationProtos.GetOperationResponse> getGetOperationMethod() {
    io.grpc.MethodDescriptor<tech.ydb.OperationProtos.GetOperationRequest, tech.ydb.OperationProtos.GetOperationResponse> getGetOperationMethod;
    if ((getGetOperationMethod = OperationServiceGrpc.getGetOperationMethod) == null) {
      synchronized (OperationServiceGrpc.class) {
        if ((getGetOperationMethod = OperationServiceGrpc.getGetOperationMethod) == null) {
          OperationServiceGrpc.getGetOperationMethod = getGetOperationMethod =
              io.grpc.MethodDescriptor.<tech.ydb.OperationProtos.GetOperationRequest, tech.ydb.OperationProtos.GetOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.GetOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.GetOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OperationServiceMethodDescriptorSupplier("GetOperation"))
              .build();
        }
      }
    }
    return getGetOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.OperationProtos.CancelOperationRequest,
      tech.ydb.OperationProtos.CancelOperationResponse> getCancelOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CancelOperation",
      requestType = tech.ydb.OperationProtos.CancelOperationRequest.class,
      responseType = tech.ydb.OperationProtos.CancelOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.OperationProtos.CancelOperationRequest,
      tech.ydb.OperationProtos.CancelOperationResponse> getCancelOperationMethod() {
    io.grpc.MethodDescriptor<tech.ydb.OperationProtos.CancelOperationRequest, tech.ydb.OperationProtos.CancelOperationResponse> getCancelOperationMethod;
    if ((getCancelOperationMethod = OperationServiceGrpc.getCancelOperationMethod) == null) {
      synchronized (OperationServiceGrpc.class) {
        if ((getCancelOperationMethod = OperationServiceGrpc.getCancelOperationMethod) == null) {
          OperationServiceGrpc.getCancelOperationMethod = getCancelOperationMethod =
              io.grpc.MethodDescriptor.<tech.ydb.OperationProtos.CancelOperationRequest, tech.ydb.OperationProtos.CancelOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.CancelOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.CancelOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OperationServiceMethodDescriptorSupplier("CancelOperation"))
              .build();
        }
      }
    }
    return getCancelOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ForgetOperationRequest,
      tech.ydb.OperationProtos.ForgetOperationResponse> getForgetOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ForgetOperation",
      requestType = tech.ydb.OperationProtos.ForgetOperationRequest.class,
      responseType = tech.ydb.OperationProtos.ForgetOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ForgetOperationRequest,
      tech.ydb.OperationProtos.ForgetOperationResponse> getForgetOperationMethod() {
    io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ForgetOperationRequest, tech.ydb.OperationProtos.ForgetOperationResponse> getForgetOperationMethod;
    if ((getForgetOperationMethod = OperationServiceGrpc.getForgetOperationMethod) == null) {
      synchronized (OperationServiceGrpc.class) {
        if ((getForgetOperationMethod = OperationServiceGrpc.getForgetOperationMethod) == null) {
          OperationServiceGrpc.getForgetOperationMethod = getForgetOperationMethod =
              io.grpc.MethodDescriptor.<tech.ydb.OperationProtos.ForgetOperationRequest, tech.ydb.OperationProtos.ForgetOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ForgetOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.ForgetOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.ForgetOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OperationServiceMethodDescriptorSupplier("ForgetOperation"))
              .build();
        }
      }
    }
    return getForgetOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ListOperationsRequest,
      tech.ydb.OperationProtos.ListOperationsResponse> getListOperationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListOperations",
      requestType = tech.ydb.OperationProtos.ListOperationsRequest.class,
      responseType = tech.ydb.OperationProtos.ListOperationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ListOperationsRequest,
      tech.ydb.OperationProtos.ListOperationsResponse> getListOperationsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ListOperationsRequest, tech.ydb.OperationProtos.ListOperationsResponse> getListOperationsMethod;
    if ((getListOperationsMethod = OperationServiceGrpc.getListOperationsMethod) == null) {
      synchronized (OperationServiceGrpc.class) {
        if ((getListOperationsMethod = OperationServiceGrpc.getListOperationsMethod) == null) {
          OperationServiceGrpc.getListOperationsMethod = getListOperationsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.OperationProtos.ListOperationsRequest, tech.ydb.OperationProtos.ListOperationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListOperations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.ListOperationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.OperationProtos.ListOperationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new OperationServiceMethodDescriptorSupplier("ListOperations"))
              .build();
        }
      }
    }
    return getListOperationsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static OperationServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OperationServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OperationServiceStub>() {
        @java.lang.Override
        public OperationServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OperationServiceStub(channel, callOptions);
        }
      };
    return OperationServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static OperationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OperationServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OperationServiceBlockingStub>() {
        @java.lang.Override
        public OperationServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OperationServiceBlockingStub(channel, callOptions);
        }
      };
    return OperationServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static OperationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<OperationServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<OperationServiceFutureStub>() {
        @java.lang.Override
        public OperationServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new OperationServiceFutureStub(channel, callOptions);
        }
      };
    return OperationServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class OperationServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Check status for a given operation.
     * </pre>
     */
    public void getOperation(tech.ydb.OperationProtos.GetOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.GetOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Starts cancellation of a long-running operation,
     * Clients can use GetOperation to check whether the cancellation succeeded
     * or whether the operation completed despite cancellation.
     * </pre>
     */
    public void cancelOperation(tech.ydb.OperationProtos.CancelOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.CancelOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCancelOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public void forgetOperation(tech.ydb.OperationProtos.ForgetOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ForgetOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getForgetOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public void listOperations(tech.ydb.OperationProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ListOperationsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListOperationsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGetOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.GetOperationRequest,
                tech.ydb.OperationProtos.GetOperationResponse>(
                  this, METHODID_GET_OPERATION)))
          .addMethod(
            getCancelOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.CancelOperationRequest,
                tech.ydb.OperationProtos.CancelOperationResponse>(
                  this, METHODID_CANCEL_OPERATION)))
          .addMethod(
            getForgetOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.ForgetOperationRequest,
                tech.ydb.OperationProtos.ForgetOperationResponse>(
                  this, METHODID_FORGET_OPERATION)))
          .addMethod(
            getListOperationsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.ListOperationsRequest,
                tech.ydb.OperationProtos.ListOperationsResponse>(
                  this, METHODID_LIST_OPERATIONS)))
          .build();
    }
  }

  /**
   */
  public static final class OperationServiceStub extends io.grpc.stub.AbstractAsyncStub<OperationServiceStub> {
    private OperationServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OperationServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Check status for a given operation.
     * </pre>
     */
    public void getOperation(tech.ydb.OperationProtos.GetOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.GetOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Starts cancellation of a long-running operation,
     * Clients can use GetOperation to check whether the cancellation succeeded
     * or whether the operation completed despite cancellation.
     * </pre>
     */
    public void cancelOperation(tech.ydb.OperationProtos.CancelOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.CancelOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCancelOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public void forgetOperation(tech.ydb.OperationProtos.ForgetOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ForgetOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getForgetOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public void listOperations(tech.ydb.OperationProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ListOperationsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListOperationsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class OperationServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<OperationServiceBlockingStub> {
    private OperationServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OperationServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Check status for a given operation.
     * </pre>
     */
    public tech.ydb.OperationProtos.GetOperationResponse getOperation(tech.ydb.OperationProtos.GetOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Starts cancellation of a long-running operation,
     * Clients can use GetOperation to check whether the cancellation succeeded
     * or whether the operation completed despite cancellation.
     * </pre>
     */
    public tech.ydb.OperationProtos.CancelOperationResponse cancelOperation(tech.ydb.OperationProtos.CancelOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getCancelOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public tech.ydb.OperationProtos.ForgetOperationResponse forgetOperation(tech.ydb.OperationProtos.ForgetOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getForgetOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public tech.ydb.OperationProtos.ListOperationsResponse listOperations(tech.ydb.OperationProtos.ListOperationsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListOperationsMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class OperationServiceFutureStub extends io.grpc.stub.AbstractFutureStub<OperationServiceFutureStub> {
    private OperationServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new OperationServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Check status for a given operation.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.OperationProtos.GetOperationResponse> getOperation(
        tech.ydb.OperationProtos.GetOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Starts cancellation of a long-running operation,
     * Clients can use GetOperation to check whether the cancellation succeeded
     * or whether the operation completed despite cancellation.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.OperationProtos.CancelOperationResponse> cancelOperation(
        tech.ydb.OperationProtos.CancelOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCancelOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.OperationProtos.ForgetOperationResponse> forgetOperation(
        tech.ydb.OperationProtos.ForgetOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getForgetOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.OperationProtos.ListOperationsResponse> listOperations(
        tech.ydb.OperationProtos.ListOperationsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListOperationsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_OPERATION = 0;
  private static final int METHODID_CANCEL_OPERATION = 1;
  private static final int METHODID_FORGET_OPERATION = 2;
  private static final int METHODID_LIST_OPERATIONS = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final OperationServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(OperationServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_OPERATION:
          serviceImpl.getOperation((tech.ydb.OperationProtos.GetOperationRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.GetOperationResponse>) responseObserver);
          break;
        case METHODID_CANCEL_OPERATION:
          serviceImpl.cancelOperation((tech.ydb.OperationProtos.CancelOperationRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.CancelOperationResponse>) responseObserver);
          break;
        case METHODID_FORGET_OPERATION:
          serviceImpl.forgetOperation((tech.ydb.OperationProtos.ForgetOperationRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ForgetOperationResponse>) responseObserver);
          break;
        case METHODID_LIST_OPERATIONS:
          serviceImpl.listOperations((tech.ydb.OperationProtos.ListOperationsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ListOperationsResponse>) responseObserver);
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

  private static abstract class OperationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    OperationServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.operation.v1.YdbOperationV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("OperationService");
    }
  }

  private static final class OperationServiceFileDescriptorSupplier
      extends OperationServiceBaseDescriptorSupplier {
    OperationServiceFileDescriptorSupplier() {}
  }

  private static final class OperationServiceMethodDescriptorSupplier
      extends OperationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    OperationServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (OperationServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new OperationServiceFileDescriptorSupplier())
              .addMethod(getGetOperationMethod())
              .addMethod(getCancelOperationMethod())
              .addMethod(getForgetOperationMethod())
              .addMethod(getListOperationsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
