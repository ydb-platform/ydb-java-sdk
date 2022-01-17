package tech.ydb.export.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_export_v1.proto")
public final class ExportServiceGrpc {

  private ExportServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Export.V1.ExportService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToYtRequest,
      tech.ydb.export.YdbExport.ExportToYtResponse> getExportToYtMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExportToYt",
      requestType = tech.ydb.export.YdbExport.ExportToYtRequest.class,
      responseType = tech.ydb.export.YdbExport.ExportToYtResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToYtRequest,
      tech.ydb.export.YdbExport.ExportToYtResponse> getExportToYtMethod() {
    io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToYtRequest, tech.ydb.export.YdbExport.ExportToYtResponse> getExportToYtMethod;
    if ((getExportToYtMethod = ExportServiceGrpc.getExportToYtMethod) == null) {
      synchronized (ExportServiceGrpc.class) {
        if ((getExportToYtMethod = ExportServiceGrpc.getExportToYtMethod) == null) {
          ExportServiceGrpc.getExportToYtMethod = getExportToYtMethod =
              io.grpc.MethodDescriptor.<tech.ydb.export.YdbExport.ExportToYtRequest, tech.ydb.export.YdbExport.ExportToYtResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExportToYt"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.export.YdbExport.ExportToYtRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.export.YdbExport.ExportToYtResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExportServiceMethodDescriptorSupplier("ExportToYt"))
              .build();
        }
      }
    }
    return getExportToYtMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToS3Request,
      tech.ydb.export.YdbExport.ExportToS3Response> getExportToS3Method;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExportToS3",
      requestType = tech.ydb.export.YdbExport.ExportToS3Request.class,
      responseType = tech.ydb.export.YdbExport.ExportToS3Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToS3Request,
      tech.ydb.export.YdbExport.ExportToS3Response> getExportToS3Method() {
    io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToS3Request, tech.ydb.export.YdbExport.ExportToS3Response> getExportToS3Method;
    if ((getExportToS3Method = ExportServiceGrpc.getExportToS3Method) == null) {
      synchronized (ExportServiceGrpc.class) {
        if ((getExportToS3Method = ExportServiceGrpc.getExportToS3Method) == null) {
          ExportServiceGrpc.getExportToS3Method = getExportToS3Method =
              io.grpc.MethodDescriptor.<tech.ydb.export.YdbExport.ExportToS3Request, tech.ydb.export.YdbExport.ExportToS3Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExportToS3"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.export.YdbExport.ExportToS3Request.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.export.YdbExport.ExportToS3Response.getDefaultInstance()))
              .setSchemaDescriptor(new ExportServiceMethodDescriptorSupplier("ExportToS3"))
              .build();
        }
      }
    }
    return getExportToS3Method;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExportServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExportServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExportServiceStub>() {
        @java.lang.Override
        public ExportServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExportServiceStub(channel, callOptions);
        }
      };
    return ExportServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExportServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExportServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExportServiceBlockingStub>() {
        @java.lang.Override
        public ExportServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExportServiceBlockingStub(channel, callOptions);
        }
      };
    return ExportServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ExportServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExportServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExportServiceFutureStub>() {
        @java.lang.Override
        public ExportServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExportServiceFutureStub(channel, callOptions);
        }
      };
    return ExportServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ExportServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Exports data to YT.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void exportToYt(tech.ydb.export.YdbExport.ExportToYtRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToYtResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExportToYtMethod(), responseObserver);
    }

    /**
     * <pre>
     * Exports data to S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void exportToS3(tech.ydb.export.YdbExport.ExportToS3Request request,
        io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToS3Response> responseObserver) {
      asyncUnimplementedUnaryCall(getExportToS3Method(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getExportToYtMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.export.YdbExport.ExportToYtRequest,
                tech.ydb.export.YdbExport.ExportToYtResponse>(
                  this, METHODID_EXPORT_TO_YT)))
          .addMethod(
            getExportToS3Method(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.export.YdbExport.ExportToS3Request,
                tech.ydb.export.YdbExport.ExportToS3Response>(
                  this, METHODID_EXPORT_TO_S3)))
          .build();
    }
  }

  /**
   */
  public static final class ExportServiceStub extends io.grpc.stub.AbstractAsyncStub<ExportServiceStub> {
    private ExportServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExportServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Exports data to YT.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void exportToYt(tech.ydb.export.YdbExport.ExportToYtRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToYtResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExportToYtMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Exports data to S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void exportToS3(tech.ydb.export.YdbExport.ExportToS3Request request,
        io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToS3Response> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExportToS3Method(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ExportServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ExportServiceBlockingStub> {
    private ExportServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExportServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Exports data to YT.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public tech.ydb.export.YdbExport.ExportToYtResponse exportToYt(tech.ydb.export.YdbExport.ExportToYtRequest request) {
      return blockingUnaryCall(
          getChannel(), getExportToYtMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Exports data to S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public tech.ydb.export.YdbExport.ExportToS3Response exportToS3(tech.ydb.export.YdbExport.ExportToS3Request request) {
      return blockingUnaryCall(
          getChannel(), getExportToS3Method(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ExportServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ExportServiceFutureStub> {
    private ExportServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExportServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Exports data to YT.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.export.YdbExport.ExportToYtResponse> exportToYt(
        tech.ydb.export.YdbExport.ExportToYtRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExportToYtMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Exports data to S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.export.YdbExport.ExportToS3Response> exportToS3(
        tech.ydb.export.YdbExport.ExportToS3Request request) {
      return futureUnaryCall(
          getChannel().newCall(getExportToS3Method(), getCallOptions()), request);
    }
  }

  private static final int METHODID_EXPORT_TO_YT = 0;
  private static final int METHODID_EXPORT_TO_S3 = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ExportServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ExportServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_EXPORT_TO_YT:
          serviceImpl.exportToYt((tech.ydb.export.YdbExport.ExportToYtRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToYtResponse>) responseObserver);
          break;
        case METHODID_EXPORT_TO_S3:
          serviceImpl.exportToS3((tech.ydb.export.YdbExport.ExportToS3Request) request,
              (io.grpc.stub.StreamObserver<tech.ydb.export.YdbExport.ExportToS3Response>) responseObserver);
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

  private static abstract class ExportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ExportServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.export.v1.YdbExportV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ExportService");
    }
  }

  private static final class ExportServiceFileDescriptorSupplier
      extends ExportServiceBaseDescriptorSupplier {
    ExportServiceFileDescriptorSupplier() {}
  }

  private static final class ExportServiceMethodDescriptorSupplier
      extends ExportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ExportServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ExportServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ExportServiceFileDescriptorSupplier())
              .addMethod(getExportToYtMethod())
              .addMethod(getExportToS3Method())
              .build();
        }
      }
    }
    return result;
  }
}
