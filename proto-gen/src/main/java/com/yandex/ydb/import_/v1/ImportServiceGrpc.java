package tech.ydb.import_.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_import_v1.proto")
public final class ImportServiceGrpc {

  private ImportServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Import.V1.ImportService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportFromS3Request,
      tech.ydb.import_.YdbImport.ImportFromS3Response> getImportFromS3Method;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ImportFromS3",
      requestType = tech.ydb.import_.YdbImport.ImportFromS3Request.class,
      responseType = tech.ydb.import_.YdbImport.ImportFromS3Response.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportFromS3Request,
      tech.ydb.import_.YdbImport.ImportFromS3Response> getImportFromS3Method() {
    io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportFromS3Request, tech.ydb.import_.YdbImport.ImportFromS3Response> getImportFromS3Method;
    if ((getImportFromS3Method = ImportServiceGrpc.getImportFromS3Method) == null) {
      synchronized (ImportServiceGrpc.class) {
        if ((getImportFromS3Method = ImportServiceGrpc.getImportFromS3Method) == null) {
          ImportServiceGrpc.getImportFromS3Method = getImportFromS3Method =
              io.grpc.MethodDescriptor.<tech.ydb.import_.YdbImport.ImportFromS3Request, tech.ydb.import_.YdbImport.ImportFromS3Response>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ImportFromS3"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.import_.YdbImport.ImportFromS3Request.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.import_.YdbImport.ImportFromS3Response.getDefaultInstance()))
              .setSchemaDescriptor(new ImportServiceMethodDescriptorSupplier("ImportFromS3"))
              .build();
        }
      }
    }
    return getImportFromS3Method;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportDataRequest,
      tech.ydb.import_.YdbImport.ImportDataResponse> getImportDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ImportData",
      requestType = tech.ydb.import_.YdbImport.ImportDataRequest.class,
      responseType = tech.ydb.import_.YdbImport.ImportDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportDataRequest,
      tech.ydb.import_.YdbImport.ImportDataResponse> getImportDataMethod() {
    io.grpc.MethodDescriptor<tech.ydb.import_.YdbImport.ImportDataRequest, tech.ydb.import_.YdbImport.ImportDataResponse> getImportDataMethod;
    if ((getImportDataMethod = ImportServiceGrpc.getImportDataMethod) == null) {
      synchronized (ImportServiceGrpc.class) {
        if ((getImportDataMethod = ImportServiceGrpc.getImportDataMethod) == null) {
          ImportServiceGrpc.getImportDataMethod = getImportDataMethod =
              io.grpc.MethodDescriptor.<tech.ydb.import_.YdbImport.ImportDataRequest, tech.ydb.import_.YdbImport.ImportDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ImportData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.import_.YdbImport.ImportDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.import_.YdbImport.ImportDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ImportServiceMethodDescriptorSupplier("ImportData"))
              .build();
        }
      }
    }
    return getImportDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ImportServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImportServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImportServiceStub>() {
        @java.lang.Override
        public ImportServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImportServiceStub(channel, callOptions);
        }
      };
    return ImportServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ImportServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImportServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImportServiceBlockingStub>() {
        @java.lang.Override
        public ImportServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImportServiceBlockingStub(channel, callOptions);
        }
      };
    return ImportServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ImportServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImportServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImportServiceFutureStub>() {
        @java.lang.Override
        public ImportServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImportServiceFutureStub(channel, callOptions);
        }
      };
    return ImportServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ImportServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Imports data from S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void importFromS3(tech.ydb.import_.YdbImport.ImportFromS3Request request,
        io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportFromS3Response> responseObserver) {
      asyncUnimplementedUnaryCall(getImportFromS3Method(), responseObserver);
    }

    /**
     * <pre>
     * Writes data to a table.
     * Method accepts serialized data in the selected format and writes it non-transactionally.
     * </pre>
     */
    public void importData(tech.ydb.import_.YdbImport.ImportDataRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportDataResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getImportDataMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getImportFromS3Method(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.import_.YdbImport.ImportFromS3Request,
                tech.ydb.import_.YdbImport.ImportFromS3Response>(
                  this, METHODID_IMPORT_FROM_S3)))
          .addMethod(
            getImportDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.import_.YdbImport.ImportDataRequest,
                tech.ydb.import_.YdbImport.ImportDataResponse>(
                  this, METHODID_IMPORT_DATA)))
          .build();
    }
  }

  /**
   */
  public static final class ImportServiceStub extends io.grpc.stub.AbstractAsyncStub<ImportServiceStub> {
    private ImportServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImportServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImportServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Imports data from S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void importFromS3(tech.ydb.import_.YdbImport.ImportFromS3Request request,
        io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportFromS3Response> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getImportFromS3Method(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Writes data to a table.
     * Method accepts serialized data in the selected format and writes it non-transactionally.
     * </pre>
     */
    public void importData(tech.ydb.import_.YdbImport.ImportDataRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportDataResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getImportDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ImportServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ImportServiceBlockingStub> {
    private ImportServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImportServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImportServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Imports data from S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public tech.ydb.import_.YdbImport.ImportFromS3Response importFromS3(tech.ydb.import_.YdbImport.ImportFromS3Request request) {
      return blockingUnaryCall(
          getChannel(), getImportFromS3Method(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Writes data to a table.
     * Method accepts serialized data in the selected format and writes it non-transactionally.
     * </pre>
     */
    public tech.ydb.import_.YdbImport.ImportDataResponse importData(tech.ydb.import_.YdbImport.ImportDataRequest request) {
      return blockingUnaryCall(
          getChannel(), getImportDataMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ImportServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ImportServiceFutureStub> {
    private ImportServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImportServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImportServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Imports data from S3.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.import_.YdbImport.ImportFromS3Response> importFromS3(
        tech.ydb.import_.YdbImport.ImportFromS3Request request) {
      return futureUnaryCall(
          getChannel().newCall(getImportFromS3Method(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Writes data to a table.
     * Method accepts serialized data in the selected format and writes it non-transactionally.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.import_.YdbImport.ImportDataResponse> importData(
        tech.ydb.import_.YdbImport.ImportDataRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getImportDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_IMPORT_FROM_S3 = 0;
  private static final int METHODID_IMPORT_DATA = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ImportServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ImportServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_IMPORT_FROM_S3:
          serviceImpl.importFromS3((tech.ydb.import_.YdbImport.ImportFromS3Request) request,
              (io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportFromS3Response>) responseObserver);
          break;
        case METHODID_IMPORT_DATA:
          serviceImpl.importData((tech.ydb.import_.YdbImport.ImportDataRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.import_.YdbImport.ImportDataResponse>) responseObserver);
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

  private static abstract class ImportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ImportServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.import_.v1.YdbImportV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ImportService");
    }
  }

  private static final class ImportServiceFileDescriptorSupplier
      extends ImportServiceBaseDescriptorSupplier {
    ImportServiceFileDescriptorSupplier() {}
  }

  private static final class ImportServiceMethodDescriptorSupplier
      extends ImportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ImportServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ImportServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ImportServiceFileDescriptorSupplier())
              .addMethod(getImportFromS3Method())
              .addMethod(getImportDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
