package tech.ydb.experimental.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_experimental_v1.proto")
public final class ExperimentalServiceGrpc {

  private ExperimentalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Experimental.V1.ExperimentalService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest,
      tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> getUploadRowsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadRows",
      requestType = tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest.class,
      responseType = tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest,
      tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> getUploadRowsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest, tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse> getUploadRowsMethod;
    if ((getUploadRowsMethod = ExperimentalServiceGrpc.getUploadRowsMethod) == null) {
      synchronized (ExperimentalServiceGrpc.class) {
        if ((getUploadRowsMethod = ExperimentalServiceGrpc.getUploadRowsMethod) == null) {
          ExperimentalServiceGrpc.getUploadRowsMethod = getUploadRowsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest, tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadRows"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExperimentalServiceMethodDescriptorSupplier("UploadRows"))
              .build();
        }
      }
    }
    return getUploadRowsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest,
      tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> getExecuteStreamQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteStreamQuery",
      requestType = tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest.class,
      responseType = tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest,
      tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> getExecuteStreamQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest, tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> getExecuteStreamQueryMethod;
    if ((getExecuteStreamQueryMethod = ExperimentalServiceGrpc.getExecuteStreamQueryMethod) == null) {
      synchronized (ExperimentalServiceGrpc.class) {
        if ((getExecuteStreamQueryMethod = ExperimentalServiceGrpc.getExecuteStreamQueryMethod) == null) {
          ExperimentalServiceGrpc.getExecuteStreamQueryMethod = getExecuteStreamQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest, tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteStreamQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExperimentalServiceMethodDescriptorSupplier("ExecuteStreamQuery"))
              .build();
        }
      }
    }
    return getExecuteStreamQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest,
      tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> getGetDiskSpaceUsageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDiskSpaceUsage",
      requestType = tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest.class,
      responseType = tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest,
      tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> getGetDiskSpaceUsageMethod() {
    io.grpc.MethodDescriptor<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest, tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> getGetDiskSpaceUsageMethod;
    if ((getGetDiskSpaceUsageMethod = ExperimentalServiceGrpc.getGetDiskSpaceUsageMethod) == null) {
      synchronized (ExperimentalServiceGrpc.class) {
        if ((getGetDiskSpaceUsageMethod = ExperimentalServiceGrpc.getGetDiskSpaceUsageMethod) == null) {
          ExperimentalServiceGrpc.getGetDiskSpaceUsageMethod = getGetDiskSpaceUsageMethod =
              io.grpc.MethodDescriptor.<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest, tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDiskSpaceUsage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExperimentalServiceMethodDescriptorSupplier("GetDiskSpaceUsage"))
              .build();
        }
      }
    }
    return getGetDiskSpaceUsageMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExperimentalServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceStub>() {
        @java.lang.Override
        public ExperimentalServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExperimentalServiceStub(channel, callOptions);
        }
      };
    return ExperimentalServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExperimentalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceBlockingStub>() {
        @java.lang.Override
        public ExperimentalServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExperimentalServiceBlockingStub(channel, callOptions);
        }
      };
    return ExperimentalServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ExperimentalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExperimentalServiceFutureStub>() {
        @java.lang.Override
        public ExperimentalServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExperimentalServiceFutureStub(channel, callOptions);
        }
      };
    return ExperimentalServiceFutureStub.newStub(factory, channel);
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
      asyncUnimplementedUnaryCall(getUploadRowsMethod(), responseObserver);
    }

    /**
     */
    public void executeStreamQuery(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteStreamQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns disk space usage by database
     * </pre>
     */
    public void getDiskSpaceUsage(tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDiskSpaceUsageMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getUploadRowsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest,
                tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse>(
                  this, METHODID_UPLOAD_ROWS)))
          .addMethod(
            getExecuteStreamQueryMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest,
                tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse>(
                  this, METHODID_EXECUTE_STREAM_QUERY)))
          .addMethod(
            getGetDiskSpaceUsageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest,
                tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse>(
                  this, METHODID_GET_DISK_SPACE_USAGE)))
          .build();
    }
  }

  /**
   */
  public static final class ExperimentalServiceStub extends io.grpc.stub.AbstractAsyncStub<ExperimentalServiceStub> {
    private ExperimentalServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getUploadRowsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void executeStreamQuery(tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getExecuteStreamQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns disk space usage by database
     * </pre>
     */
    public void getDiskSpaceUsage(tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDiskSpaceUsageMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ExperimentalServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ExperimentalServiceBlockingStub> {
    private ExperimentalServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExperimentalServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Fast bulk load rows to a table bypassing transaction logic.
     * </pre>
     */
    public tech.ydb.experimental.ExperimentalProtos.UploadRowsResponse uploadRows(tech.ydb.experimental.ExperimentalProtos.UploadRowsRequest request) {
      return blockingUnaryCall(
          getChannel(), getUploadRowsMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryResponse> executeStreamQuery(
        tech.ydb.experimental.ExperimentalProtos.ExecuteStreamQueryRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getExecuteStreamQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns disk space usage by database
     * </pre>
     */
    public tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse getDiskSpaceUsage(tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetDiskSpaceUsageMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ExperimentalServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ExperimentalServiceFutureStub> {
    private ExperimentalServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExperimentalServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getUploadRowsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns disk space usage by database
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse> getDiskSpaceUsage(
        tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDiskSpaceUsageMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_UPLOAD_ROWS = 0;
  private static final int METHODID_EXECUTE_STREAM_QUERY = 1;
  private static final int METHODID_GET_DISK_SPACE_USAGE = 2;

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
        case METHODID_GET_DISK_SPACE_USAGE:
          serviceImpl.getDiskSpaceUsage((tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.experimental.ExperimentalProtos.GetDiskSpaceUsageResponse>) responseObserver);
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

  private static abstract class ExperimentalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ExperimentalServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.experimental.v1.YdbExperimentalV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ExperimentalService");
    }
  }

  private static final class ExperimentalServiceFileDescriptorSupplier
      extends ExperimentalServiceBaseDescriptorSupplier {
    ExperimentalServiceFileDescriptorSupplier() {}
  }

  private static final class ExperimentalServiceMethodDescriptorSupplier
      extends ExperimentalServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ExperimentalServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ExperimentalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ExperimentalServiceFileDescriptorSupplier())
              .addMethod(getUploadRowsMethod())
              .addMethod(getExecuteStreamQueryMethod())
              .addMethod(getGetDiskSpaceUsageMethod())
              .build();
        }
      }
    }
    return result;
  }
}
