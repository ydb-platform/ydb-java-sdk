package tech.ydb.operation.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_operation_v1.proto")
public final class OperationServiceGrpc {

  private OperationServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Operation.V1.OperationService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.OperationProtos.GetOperationRequest,
      tech.ydb.OperationProtos.GetOperationResponse> METHOD_GET_OPERATION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Operation.V1.OperationService", "GetOperation"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.GetOperationRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.GetOperationResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.OperationProtos.CancelOperationRequest,
      tech.ydb.OperationProtos.CancelOperationResponse> METHOD_CANCEL_OPERATION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Operation.V1.OperationService", "CancelOperation"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.CancelOperationRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.CancelOperationResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ForgetOperationRequest,
      tech.ydb.OperationProtos.ForgetOperationResponse> METHOD_FORGET_OPERATION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Operation.V1.OperationService", "ForgetOperation"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.ForgetOperationRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.ForgetOperationResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.OperationProtos.ListOperationsRequest,
      tech.ydb.OperationProtos.ListOperationsResponse> METHOD_LIST_OPERATIONS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Operation.V1.OperationService", "ListOperations"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.ListOperationsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.OperationProtos.ListOperationsResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static OperationServiceStub newStub(io.grpc.Channel channel) {
    return new OperationServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static OperationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new OperationServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static OperationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new OperationServiceFutureStub(channel);
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
      asyncUnimplementedUnaryCall(METHOD_GET_OPERATION, responseObserver);
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
      asyncUnimplementedUnaryCall(METHOD_CANCEL_OPERATION, responseObserver);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public void forgetOperation(tech.ydb.OperationProtos.ForgetOperationRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ForgetOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_FORGET_OPERATION, responseObserver);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public void listOperations(tech.ydb.OperationProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ListOperationsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_OPERATIONS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_GET_OPERATION,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.GetOperationRequest,
                tech.ydb.OperationProtos.GetOperationResponse>(
                  this, METHODID_GET_OPERATION)))
          .addMethod(
            METHOD_CANCEL_OPERATION,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.CancelOperationRequest,
                tech.ydb.OperationProtos.CancelOperationResponse>(
                  this, METHODID_CANCEL_OPERATION)))
          .addMethod(
            METHOD_FORGET_OPERATION,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.OperationProtos.ForgetOperationRequest,
                tech.ydb.OperationProtos.ForgetOperationResponse>(
                  this, METHODID_FORGET_OPERATION)))
          .addMethod(
            METHOD_LIST_OPERATIONS,
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
  public static final class OperationServiceStub extends io.grpc.stub.AbstractStub<OperationServiceStub> {
    private OperationServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private OperationServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_GET_OPERATION, getCallOptions()), request, responseObserver);
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
          getChannel().newCall(METHOD_CANCEL_OPERATION, getCallOptions()), request, responseObserver);
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
          getChannel().newCall(METHOD_FORGET_OPERATION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public void listOperations(tech.ydb.OperationProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.OperationProtos.ListOperationsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_OPERATIONS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class OperationServiceBlockingStub extends io.grpc.stub.AbstractStub<OperationServiceBlockingStub> {
    private OperationServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private OperationServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new OperationServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Check status for a given operation.
     * </pre>
     */
    public tech.ydb.OperationProtos.GetOperationResponse getOperation(tech.ydb.OperationProtos.GetOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_OPERATION, getCallOptions(), request);
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
          getChannel(), METHOD_CANCEL_OPERATION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Forgets long-running operation. It does not cancel the operation and returns
     * an error if operation was not completed.
     * </pre>
     */
    public tech.ydb.OperationProtos.ForgetOperationResponse forgetOperation(tech.ydb.OperationProtos.ForgetOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_FORGET_OPERATION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public tech.ydb.OperationProtos.ListOperationsResponse listOperations(tech.ydb.OperationProtos.ListOperationsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_OPERATIONS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class OperationServiceFutureStub extends io.grpc.stub.AbstractStub<OperationServiceFutureStub> {
    private OperationServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private OperationServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected OperationServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_GET_OPERATION, getCallOptions()), request);
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
          getChannel().newCall(METHOD_CANCEL_OPERATION, getCallOptions()), request);
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
          getChannel().newCall(METHOD_FORGET_OPERATION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Lists operations that match the specified filter in the request.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.OperationProtos.ListOperationsResponse> listOperations(
        tech.ydb.OperationProtos.ListOperationsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_OPERATIONS, getCallOptions()), request);
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

  private static final class OperationServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.operation.v1.YdbOperationV1.getDescriptor();
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
              .setSchemaDescriptor(new OperationServiceDescriptorSupplier())
              .addMethod(METHOD_GET_OPERATION)
              .addMethod(METHOD_CANCEL_OPERATION)
              .addMethod(METHOD_FORGET_OPERATION)
              .addMethod(METHOD_LIST_OPERATIONS)
              .build();
        }
      }
    }
    return result;
  }
}
