package tech.ydb.experimental.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_experimental_v1.proto")
public final class ExperimentalServiceGrpc {

  private ExperimentalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Experimental.V1.ExperimentalService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest,
      tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> METHOD_UPLOAD_ROWS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Experimental.V1.ExperimentalService", "UploadRows"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest,
      tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> METHOD_EXECUTE_STREAM_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "Ydb.Experimental.V1.ExperimentalService", "ExecuteStreamQuery"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExperimentalServiceStub newStub(io.grpc.Channel channel) {
    return new ExperimentalServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExperimentalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ExperimentalServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ExperimentalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ExperimentalServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class ExperimentalServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Fast bulk load rows to a table bypassing transaction logic.
     * </pre>
     */
    public void uploadRows(tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_UPLOAD_ROWS, responseObserver);
    }

    /**
     */
    public void executeStreamQuery(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXECUTE_STREAM_QUERY, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_UPLOAD_ROWS,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest,
                tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse>(
                  this, METHODID_UPLOAD_ROWS)))
          .addMethod(
            METHOD_EXECUTE_STREAM_QUERY,
            asyncServerStreamingCall(
              new MethodHandlers<
                tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest,
                tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse>(
                  this, METHODID_EXECUTE_STREAM_QUERY)))
          .build();
    }
  }

  /**
   */
  public static final class ExperimentalServiceStub extends io.grpc.stub.AbstractStub<ExperimentalServiceStub> {
    private ExperimentalServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExperimentalServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExperimentalServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fast bulk load rows to a table bypassing transaction logic.
     * </pre>
     */
    public void uploadRows(tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_UPLOAD_ROWS, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeStreamQuery(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_EXECUTE_STREAM_QUERY, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ExperimentalServiceBlockingStub extends io.grpc.stub.AbstractStub<ExperimentalServiceBlockingStub> {
    private ExperimentalServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExperimentalServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExperimentalServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fast bulk load rows to a table bypassing transaction logic.
     * </pre>
     */
    public tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse uploadRows(tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_UPLOAD_ROWS, getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> executeStreamQuery(
        tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_EXECUTE_STREAM_QUERY, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ExperimentalServiceFutureStub extends io.grpc.stub.AbstractStub<ExperimentalServiceFutureStub> {
    private ExperimentalServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExperimentalServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ExperimentalServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fast bulk load rows to a table bypassing transaction logic.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> uploadRows(
        tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_UPLOAD_ROWS, getCallOptions()), request);
    }
  }

  private static final int METHODID_UPLOAD_ROWS = 0;
  private static final int METHODID_EXECUTE_STREAM_QUERY = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ExperimentalServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ExperimentalServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_UPLOAD_ROWS:
          serviceImpl.uploadRows((tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_STREAM_QUERY:
          serviceImpl.executeStreamQuery((tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse>) responseObserver);
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

  private static final class ExperimentalServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.experimental.v1.YdbExperimentalV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ExperimentalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ExperimentalServiceDescriptorSupplier())
              .addMethod(METHOD_UPLOAD_ROWS)
              .addMethod(METHOD_EXECUTE_STREAM_QUERY)
              .build();
        }
      }
    }
    return result;
  }
}
