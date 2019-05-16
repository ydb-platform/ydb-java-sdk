package tech.ydb.cms.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_cms_v1.proto")
public final class CmsServiceGrpc {

  private CmsServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Cms.V1.CmsService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.CreateDatabaseRequest,
      tech.ydb.cms.YdbCms.CreateDatabaseResponse> METHOD_CREATE_DATABASE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "CreateDatabase"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.CreateDatabaseRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.CreateDatabaseResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.GetDatabaseStatusRequest,
      tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> METHOD_GET_DATABASE_STATUS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "GetDatabaseStatus"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.GetDatabaseStatusResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.AlterDatabaseRequest,
      tech.ydb.cms.YdbCms.AlterDatabaseResponse> METHOD_ALTER_DATABASE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "AlterDatabase"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.AlterDatabaseRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.AlterDatabaseResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.ListDatabasesRequest,
      tech.ydb.cms.YdbCms.ListDatabasesResponse> METHOD_LIST_DATABASES =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "ListDatabases"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.ListDatabasesRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.ListDatabasesResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.RemoveDatabaseRequest,
      tech.ydb.cms.YdbCms.RemoveDatabaseResponse> METHOD_REMOVE_DATABASE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "RemoveDatabase"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.RemoveDatabaseRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.RemoveDatabaseResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest,
      tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> METHOD_DESCRIBE_DATABASE_OPTIONS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Cms.V1.CmsService", "DescribeDatabaseOptions"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CmsServiceStub newStub(io.grpc.Channel channel) {
    return new CmsServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CmsServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new CmsServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static CmsServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new CmsServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class CmsServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Create a new database.
     * </pre>
     */
    public void createDatabase(tech.ydb.cms.YdbCms.CreateDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.CreateDatabaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_DATABASE, responseObserver);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public void getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_DATABASE_STATUS, responseObserver);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public void alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.AlterDatabaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ALTER_DATABASE, responseObserver);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public void listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.ListDatabasesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_DATABASES, responseObserver);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public void removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_REMOVE_DATABASE, responseObserver);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public void describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_DATABASE_OPTIONS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_DATABASE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.CreateDatabaseRequest,
                tech.ydb.cms.YdbCms.CreateDatabaseResponse>(
                  this, METHODID_CREATE_DATABASE)))
          .addMethod(
            METHOD_GET_DATABASE_STATUS,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.GetDatabaseStatusRequest,
                tech.ydb.cms.YdbCms.GetDatabaseStatusResponse>(
                  this, METHODID_GET_DATABASE_STATUS)))
          .addMethod(
            METHOD_ALTER_DATABASE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.AlterDatabaseRequest,
                tech.ydb.cms.YdbCms.AlterDatabaseResponse>(
                  this, METHODID_ALTER_DATABASE)))
          .addMethod(
            METHOD_LIST_DATABASES,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.ListDatabasesRequest,
                tech.ydb.cms.YdbCms.ListDatabasesResponse>(
                  this, METHODID_LIST_DATABASES)))
          .addMethod(
            METHOD_REMOVE_DATABASE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.RemoveDatabaseRequest,
                tech.ydb.cms.YdbCms.RemoveDatabaseResponse>(
                  this, METHODID_REMOVE_DATABASE)))
          .addMethod(
            METHOD_DESCRIBE_DATABASE_OPTIONS,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest,
                tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse>(
                  this, METHODID_DESCRIBE_DATABASE_OPTIONS)))
          .build();
    }
  }

  /**
   */
  public static final class CmsServiceStub extends io.grpc.stub.AbstractStub<CmsServiceStub> {
    private CmsServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CmsServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CmsServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new database.
     * </pre>
     */
    public void createDatabase(tech.ydb.cms.YdbCms.CreateDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.CreateDatabaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_DATABASE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public void getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_DATABASE_STATUS, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public void alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.AlterDatabaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ALTER_DATABASE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public void listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.ListDatabasesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_DATABASES, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public void removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REMOVE_DATABASE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public void describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_DATABASE_OPTIONS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CmsServiceBlockingStub extends io.grpc.stub.AbstractStub<CmsServiceBlockingStub> {
    private CmsServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CmsServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CmsServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new database.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.CreateDatabaseResponse createDatabase(tech.ydb.cms.YdbCms.CreateDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_DATABASE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.GetDatabaseStatusResponse getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_DATABASE_STATUS, getCallOptions(), request);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.AlterDatabaseResponse alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ALTER_DATABASE, getCallOptions(), request);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.ListDatabasesResponse listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_DATABASES, getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.RemoveDatabaseResponse removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_REMOVE_DATABASE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_DATABASE_OPTIONS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CmsServiceFutureStub extends io.grpc.stub.AbstractStub<CmsServiceFutureStub> {
    private CmsServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CmsServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CmsServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new database.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.CreateDatabaseResponse> createDatabase(
        tech.ydb.cms.YdbCms.CreateDatabaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_DATABASE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> getDatabaseStatus(
        tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_DATABASE_STATUS, getCallOptions()), request);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.AlterDatabaseResponse> alterDatabase(
        tech.ydb.cms.YdbCms.AlterDatabaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ALTER_DATABASE, getCallOptions()), request);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.ListDatabasesResponse> listDatabases(
        tech.ydb.cms.YdbCms.ListDatabasesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_DATABASES, getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> removeDatabase(
        tech.ydb.cms.YdbCms.RemoveDatabaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REMOVE_DATABASE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> describeDatabaseOptions(
        tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_DATABASE_OPTIONS, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_DATABASE = 0;
  private static final int METHODID_GET_DATABASE_STATUS = 1;
  private static final int METHODID_ALTER_DATABASE = 2;
  private static final int METHODID_LIST_DATABASES = 3;
  private static final int METHODID_REMOVE_DATABASE = 4;
  private static final int METHODID_DESCRIBE_DATABASE_OPTIONS = 5;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CmsServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CmsServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_DATABASE:
          serviceImpl.createDatabase((tech.ydb.cms.YdbCms.CreateDatabaseRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.CreateDatabaseResponse>) responseObserver);
          break;
        case METHODID_GET_DATABASE_STATUS:
          serviceImpl.getDatabaseStatus((tech.ydb.cms.YdbCms.GetDatabaseStatusRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse>) responseObserver);
          break;
        case METHODID_ALTER_DATABASE:
          serviceImpl.alterDatabase((tech.ydb.cms.YdbCms.AlterDatabaseRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.AlterDatabaseResponse>) responseObserver);
          break;
        case METHODID_LIST_DATABASES:
          serviceImpl.listDatabases((tech.ydb.cms.YdbCms.ListDatabasesRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.ListDatabasesResponse>) responseObserver);
          break;
        case METHODID_REMOVE_DATABASE:
          serviceImpl.removeDatabase((tech.ydb.cms.YdbCms.RemoveDatabaseRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.RemoveDatabaseResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_DATABASE_OPTIONS:
          serviceImpl.describeDatabaseOptions((tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse>) responseObserver);
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

  private static final class CmsServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.cms.v1.YdbCmsV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CmsServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CmsServiceDescriptorSupplier())
              .addMethod(METHOD_CREATE_DATABASE)
              .addMethod(METHOD_GET_DATABASE_STATUS)
              .addMethod(METHOD_ALTER_DATABASE)
              .addMethod(METHOD_LIST_DATABASES)
              .addMethod(METHOD_REMOVE_DATABASE)
              .addMethod(METHOD_DESCRIBE_DATABASE_OPTIONS)
              .build();
        }
      }
    }
    return result;
  }
}
