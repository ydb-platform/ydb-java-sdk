package tech.ydb.export.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_export_v1.proto")
public final class ExportServiceGrpc {

  private ExportServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Export.V1.ExportService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.export.YdbExport.ExportToYtRequest,
      tech.ydb.export.YdbExport.ExportToYtResponse> METHOD_EXPORT_TO_YT =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Export.V1.ExportService", "ExportToYt"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.export.YdbExport.ExportToYtRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.export.YdbExport.ExportToYtResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExportServiceStub newStub(io.grpc.Channel channel) {
    return new ExportServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExportServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ExportServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ExportServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ExportServiceFutureStub(channel);
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
      asyncUnimplementedUnaryCall(METHOD_EXPORT_TO_YT, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_EXPORT_TO_YT,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.export.YdbExport.ExportToYtRequest,
                tech.ydb.export.YdbExport.ExportToYtResponse>(
                  this, METHODID_EXPORT_TO_YT)))
          .build();
    }
  }

  /**
   */
  public static final class ExportServiceStub extends io.grpc.stub.AbstractStub<ExportServiceStub> {
    private ExportServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExportServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_EXPORT_TO_YT, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ExportServiceBlockingStub extends io.grpc.stub.AbstractStub<ExportServiceBlockingStub> {
    private ExportServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExportServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel(), METHOD_EXPORT_TO_YT, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ExportServiceFutureStub extends io.grpc.stub.AbstractStub<ExportServiceFutureStub> {
    private ExportServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ExportServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExportServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_EXPORT_TO_YT, getCallOptions()), request);
    }
  }

  private static final int METHODID_EXPORT_TO_YT = 0;

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

  private static final class ExportServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.export.v1.YdbExportV1.getDescriptor();
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
              .setSchemaDescriptor(new ExportServiceDescriptorSupplier())
              .addMethod(METHOD_EXPORT_TO_YT)
              .build();
        }
      }
    }
    return result;
  }
}
