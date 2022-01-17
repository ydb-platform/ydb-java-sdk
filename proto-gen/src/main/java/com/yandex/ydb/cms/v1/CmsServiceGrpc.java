package tech.ydb.cms.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_cms_v1.proto")
public final class CmsServiceGrpc {

  private CmsServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Cms.V1.CmsService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.CreateDatabaseRequest,
      tech.ydb.cms.YdbCms.CreateDatabaseResponse> getCreateDatabaseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateDatabase",
      requestType = tech.ydb.cms.YdbCms.CreateDatabaseRequest.class,
      responseType = tech.ydb.cms.YdbCms.CreateDatabaseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.CreateDatabaseRequest,
      tech.ydb.cms.YdbCms.CreateDatabaseResponse> getCreateDatabaseMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.CreateDatabaseRequest, tech.ydb.cms.YdbCms.CreateDatabaseResponse> getCreateDatabaseMethod;
    if ((getCreateDatabaseMethod = CmsServiceGrpc.getCreateDatabaseMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getCreateDatabaseMethod = CmsServiceGrpc.getCreateDatabaseMethod) == null) {
          CmsServiceGrpc.getCreateDatabaseMethod = getCreateDatabaseMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.CreateDatabaseRequest, tech.ydb.cms.YdbCms.CreateDatabaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateDatabase"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.CreateDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.CreateDatabaseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("CreateDatabase"))
              .build();
        }
      }
    }
    return getCreateDatabaseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.GetDatabaseStatusRequest,
      tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> getGetDatabaseStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetDatabaseStatus",
      requestType = tech.ydb.cms.YdbCms.GetDatabaseStatusRequest.class,
      responseType = tech.ydb.cms.YdbCms.GetDatabaseStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.GetDatabaseStatusRequest,
      tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> getGetDatabaseStatusMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.GetDatabaseStatusRequest, tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> getGetDatabaseStatusMethod;
    if ((getGetDatabaseStatusMethod = CmsServiceGrpc.getGetDatabaseStatusMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getGetDatabaseStatusMethod = CmsServiceGrpc.getGetDatabaseStatusMethod) == null) {
          CmsServiceGrpc.getGetDatabaseStatusMethod = getGetDatabaseStatusMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.GetDatabaseStatusRequest, tech.ydb.cms.YdbCms.GetDatabaseStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetDatabaseStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.GetDatabaseStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.GetDatabaseStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("GetDatabaseStatus"))
              .build();
        }
      }
    }
    return getGetDatabaseStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.AlterDatabaseRequest,
      tech.ydb.cms.YdbCms.AlterDatabaseResponse> getAlterDatabaseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterDatabase",
      requestType = tech.ydb.cms.YdbCms.AlterDatabaseRequest.class,
      responseType = tech.ydb.cms.YdbCms.AlterDatabaseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.AlterDatabaseRequest,
      tech.ydb.cms.YdbCms.AlterDatabaseResponse> getAlterDatabaseMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.AlterDatabaseRequest, tech.ydb.cms.YdbCms.AlterDatabaseResponse> getAlterDatabaseMethod;
    if ((getAlterDatabaseMethod = CmsServiceGrpc.getAlterDatabaseMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getAlterDatabaseMethod = CmsServiceGrpc.getAlterDatabaseMethod) == null) {
          CmsServiceGrpc.getAlterDatabaseMethod = getAlterDatabaseMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.AlterDatabaseRequest, tech.ydb.cms.YdbCms.AlterDatabaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterDatabase"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.AlterDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.AlterDatabaseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("AlterDatabase"))
              .build();
        }
      }
    }
    return getAlterDatabaseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.ListDatabasesRequest,
      tech.ydb.cms.YdbCms.ListDatabasesResponse> getListDatabasesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDatabases",
      requestType = tech.ydb.cms.YdbCms.ListDatabasesRequest.class,
      responseType = tech.ydb.cms.YdbCms.ListDatabasesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.ListDatabasesRequest,
      tech.ydb.cms.YdbCms.ListDatabasesResponse> getListDatabasesMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.ListDatabasesRequest, tech.ydb.cms.YdbCms.ListDatabasesResponse> getListDatabasesMethod;
    if ((getListDatabasesMethod = CmsServiceGrpc.getListDatabasesMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getListDatabasesMethod = CmsServiceGrpc.getListDatabasesMethod) == null) {
          CmsServiceGrpc.getListDatabasesMethod = getListDatabasesMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.ListDatabasesRequest, tech.ydb.cms.YdbCms.ListDatabasesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDatabases"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.ListDatabasesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.ListDatabasesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("ListDatabases"))
              .build();
        }
      }
    }
    return getListDatabasesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.RemoveDatabaseRequest,
      tech.ydb.cms.YdbCms.RemoveDatabaseResponse> getRemoveDatabaseMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveDatabase",
      requestType = tech.ydb.cms.YdbCms.RemoveDatabaseRequest.class,
      responseType = tech.ydb.cms.YdbCms.RemoveDatabaseResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.RemoveDatabaseRequest,
      tech.ydb.cms.YdbCms.RemoveDatabaseResponse> getRemoveDatabaseMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.RemoveDatabaseRequest, tech.ydb.cms.YdbCms.RemoveDatabaseResponse> getRemoveDatabaseMethod;
    if ((getRemoveDatabaseMethod = CmsServiceGrpc.getRemoveDatabaseMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getRemoveDatabaseMethod = CmsServiceGrpc.getRemoveDatabaseMethod) == null) {
          CmsServiceGrpc.getRemoveDatabaseMethod = getRemoveDatabaseMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.RemoveDatabaseRequest, tech.ydb.cms.YdbCms.RemoveDatabaseResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveDatabase"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.RemoveDatabaseRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.RemoveDatabaseResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("RemoveDatabase"))
              .build();
        }
      }
    }
    return getRemoveDatabaseMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest,
      tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> getDescribeDatabaseOptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeDatabaseOptions",
      requestType = tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest.class,
      responseType = tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest,
      tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> getDescribeDatabaseOptionsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest, tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> getDescribeDatabaseOptionsMethod;
    if ((getDescribeDatabaseOptionsMethod = CmsServiceGrpc.getDescribeDatabaseOptionsMethod) == null) {
      synchronized (CmsServiceGrpc.class) {
        if ((getDescribeDatabaseOptionsMethod = CmsServiceGrpc.getDescribeDatabaseOptionsMethod) == null) {
          CmsServiceGrpc.getDescribeDatabaseOptionsMethod = getDescribeDatabaseOptionsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest, tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeDatabaseOptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CmsServiceMethodDescriptorSupplier("DescribeDatabaseOptions"))
              .build();
        }
      }
    }
    return getDescribeDatabaseOptionsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CmsServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CmsServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CmsServiceStub>() {
        @java.lang.Override
        public CmsServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CmsServiceStub(channel, callOptions);
        }
      };
    return CmsServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CmsServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CmsServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CmsServiceBlockingStub>() {
        @java.lang.Override
        public CmsServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CmsServiceBlockingStub(channel, callOptions);
        }
      };
    return CmsServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CmsServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CmsServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CmsServiceFutureStub>() {
        @java.lang.Override
        public CmsServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CmsServiceFutureStub(channel, callOptions);
        }
      };
    return CmsServiceFutureStub.newStub(factory, channel);
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
      asyncUnimplementedUnaryCall(getCreateDatabaseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public void getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetDatabaseStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public void alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.AlterDatabaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterDatabaseMethod(), responseObserver);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public void listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.ListDatabasesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListDatabasesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public void removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveDatabaseMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public void describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeDatabaseOptionsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateDatabaseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.CreateDatabaseRequest,
                tech.ydb.cms.YdbCms.CreateDatabaseResponse>(
                  this, METHODID_CREATE_DATABASE)))
          .addMethod(
            getGetDatabaseStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.GetDatabaseStatusRequest,
                tech.ydb.cms.YdbCms.GetDatabaseStatusResponse>(
                  this, METHODID_GET_DATABASE_STATUS)))
          .addMethod(
            getAlterDatabaseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.AlterDatabaseRequest,
                tech.ydb.cms.YdbCms.AlterDatabaseResponse>(
                  this, METHODID_ALTER_DATABASE)))
          .addMethod(
            getListDatabasesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.ListDatabasesRequest,
                tech.ydb.cms.YdbCms.ListDatabasesResponse>(
                  this, METHODID_LIST_DATABASES)))
          .addMethod(
            getRemoveDatabaseMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.cms.YdbCms.RemoveDatabaseRequest,
                tech.ydb.cms.YdbCms.RemoveDatabaseResponse>(
                  this, METHODID_REMOVE_DATABASE)))
          .addMethod(
            getDescribeDatabaseOptionsMethod(),
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
  public static final class CmsServiceStub extends io.grpc.stub.AbstractAsyncStub<CmsServiceStub> {
    private CmsServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getCreateDatabaseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public void getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetDatabaseStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public void alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.AlterDatabaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterDatabaseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public void listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.ListDatabasesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListDatabasesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public void removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveDatabaseMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public void describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeDatabaseOptionsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CmsServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<CmsServiceBlockingStub> {
    private CmsServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CmsServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new database.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.CreateDatabaseResponse createDatabase(tech.ydb.cms.YdbCms.CreateDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateDatabaseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.GetDatabaseStatusResponse getDatabaseStatus(tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetDatabaseStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.AlterDatabaseResponse alterDatabase(tech.ydb.cms.YdbCms.AlterDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterDatabaseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.ListDatabasesResponse listDatabases(tech.ydb.cms.YdbCms.ListDatabasesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListDatabasesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.RemoveDatabaseResponse removeDatabase(tech.ydb.cms.YdbCms.RemoveDatabaseRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveDatabaseMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse describeDatabaseOptions(tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeDatabaseOptionsMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CmsServiceFutureStub extends io.grpc.stub.AbstractFutureStub<CmsServiceFutureStub> {
    private CmsServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CmsServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getCreateDatabaseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get current database's status.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.GetDatabaseStatusResponse> getDatabaseStatus(
        tech.ydb.cms.YdbCms.GetDatabaseStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetDatabaseStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Alter database resources.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.AlterDatabaseResponse> alterDatabase(
        tech.ydb.cms.YdbCms.AlterDatabaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterDatabaseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List all databases.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.ListDatabasesResponse> listDatabases(
        tech.ydb.cms.YdbCms.ListDatabasesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListDatabasesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove database.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.RemoveDatabaseResponse> removeDatabase(
        tech.ydb.cms.YdbCms.RemoveDatabaseRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveDatabaseMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe supported database options.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.cms.YdbCms.DescribeDatabaseOptionsResponse> describeDatabaseOptions(
        tech.ydb.cms.YdbCms.DescribeDatabaseOptionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeDatabaseOptionsMethod(), getCallOptions()), request);
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

  private static abstract class CmsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CmsServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.cms.v1.YdbCmsV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CmsService");
    }
  }

  private static final class CmsServiceFileDescriptorSupplier
      extends CmsServiceBaseDescriptorSupplier {
    CmsServiceFileDescriptorSupplier() {}
  }

  private static final class CmsServiceMethodDescriptorSupplier
      extends CmsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CmsServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (CmsServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CmsServiceFileDescriptorSupplier())
              .addMethod(getCreateDatabaseMethod())
              .addMethod(getGetDatabaseStatusMethod())
              .addMethod(getAlterDatabaseMethod())
              .addMethod(getListDatabasesMethod())
              .addMethod(getRemoveDatabaseMethod())
              .addMethod(getDescribeDatabaseOptionsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
