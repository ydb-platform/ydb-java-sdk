package tech.ydb.scheme.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_scheme_v1.proto")
public final class SchemeServiceGrpc {

  private SchemeServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Scheme.V1.SchemeService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> getMakeDirectoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MakeDirectory",
      requestType = tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest.class,
      responseType = tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> getMakeDirectoryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> getMakeDirectoryMethod;
    if ((getMakeDirectoryMethod = SchemeServiceGrpc.getMakeDirectoryMethod) == null) {
      synchronized (SchemeServiceGrpc.class) {
        if ((getMakeDirectoryMethod = SchemeServiceGrpc.getMakeDirectoryMethod) == null) {
          SchemeServiceGrpc.getMakeDirectoryMethod = getMakeDirectoryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MakeDirectory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SchemeServiceMethodDescriptorSupplier("MakeDirectory"))
              .build();
        }
      }
    }
    return getMakeDirectoryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> getRemoveDirectoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveDirectory",
      requestType = tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest.class,
      responseType = tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> getRemoveDirectoryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> getRemoveDirectoryMethod;
    if ((getRemoveDirectoryMethod = SchemeServiceGrpc.getRemoveDirectoryMethod) == null) {
      synchronized (SchemeServiceGrpc.class) {
        if ((getRemoveDirectoryMethod = SchemeServiceGrpc.getRemoveDirectoryMethod) == null) {
          SchemeServiceGrpc.getRemoveDirectoryMethod = getRemoveDirectoryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveDirectory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SchemeServiceMethodDescriptorSupplier("RemoveDirectory"))
              .build();
        }
      }
    }
    return getRemoveDirectoryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> getListDirectoryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListDirectory",
      requestType = tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest.class,
      responseType = tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> getListDirectoryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> getListDirectoryMethod;
    if ((getListDirectoryMethod = SchemeServiceGrpc.getListDirectoryMethod) == null) {
      synchronized (SchemeServiceGrpc.class) {
        if ((getListDirectoryMethod = SchemeServiceGrpc.getListDirectoryMethod) == null) {
          SchemeServiceGrpc.getListDirectoryMethod = getListDirectoryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest, tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListDirectory"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SchemeServiceMethodDescriptorSupplier("ListDirectory"))
              .build();
        }
      }
    }
    return getListDirectoryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest,
      tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> getDescribePathMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribePath",
      requestType = tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest.class,
      responseType = tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest,
      tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> getDescribePathMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest, tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> getDescribePathMethod;
    if ((getDescribePathMethod = SchemeServiceGrpc.getDescribePathMethod) == null) {
      synchronized (SchemeServiceGrpc.class) {
        if ((getDescribePathMethod = SchemeServiceGrpc.getDescribePathMethod) == null) {
          SchemeServiceGrpc.getDescribePathMethod = getDescribePathMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest, tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribePath"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SchemeServiceMethodDescriptorSupplier("DescribePath"))
              .build();
        }
      }
    }
    return getDescribePathMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest,
      tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> getModifyPermissionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyPermissions",
      requestType = tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest.class,
      responseType = tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest,
      tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> getModifyPermissionsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest, tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> getModifyPermissionsMethod;
    if ((getModifyPermissionsMethod = SchemeServiceGrpc.getModifyPermissionsMethod) == null) {
      synchronized (SchemeServiceGrpc.class) {
        if ((getModifyPermissionsMethod = SchemeServiceGrpc.getModifyPermissionsMethod) == null) {
          SchemeServiceGrpc.getModifyPermissionsMethod = getModifyPermissionsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest, tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyPermissions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SchemeServiceMethodDescriptorSupplier("ModifyPermissions"))
              .build();
        }
      }
    }
    return getModifyPermissionsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SchemeServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SchemeServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SchemeServiceStub>() {
        @java.lang.Override
        public SchemeServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SchemeServiceStub(channel, callOptions);
        }
      };
    return SchemeServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SchemeServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SchemeServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SchemeServiceBlockingStub>() {
        @java.lang.Override
        public SchemeServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SchemeServiceBlockingStub(channel, callOptions);
        }
      };
    return SchemeServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SchemeServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SchemeServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SchemeServiceFutureStub>() {
        @java.lang.Override
        public SchemeServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SchemeServiceFutureStub(channel, callOptions);
        }
      };
    return SchemeServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class SchemeServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Make Directory.
     * </pre>
     */
    public void makeDirectory(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getMakeDirectoryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public void removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveDirectoryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public void listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListDirectoryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public void describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribePathMethod(), responseObserver);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public void modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyPermissionsMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getMakeDirectoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse>(
                  this, METHODID_MAKE_DIRECTORY)))
          .addMethod(
            getRemoveDirectoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse>(
                  this, METHODID_REMOVE_DIRECTORY)))
          .addMethod(
            getListDirectoryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse>(
                  this, METHODID_LIST_DIRECTORY)))
          .addMethod(
            getDescribePathMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest,
                tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse>(
                  this, METHODID_DESCRIBE_PATH)))
          .addMethod(
            getModifyPermissionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest,
                tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse>(
                  this, METHODID_MODIFY_PERMISSIONS)))
          .build();
    }
  }

  /**
   */
  public static final class SchemeServiceStub extends io.grpc.stub.AbstractAsyncStub<SchemeServiceStub> {
    private SchemeServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SchemeServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make Directory.
     * </pre>
     */
    public void makeDirectory(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getMakeDirectoryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public void removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveDirectoryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public void listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListDirectoryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public void describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribePathMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public void modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyPermissionsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class SchemeServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<SchemeServiceBlockingStub> {
    private SchemeServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SchemeServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make Directory.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse makeDirectory(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), getMakeDirectoryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveDirectoryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), getListDirectoryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribePathMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyPermissionsMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class SchemeServiceFutureStub extends io.grpc.stub.AbstractFutureStub<SchemeServiceFutureStub> {
    private SchemeServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SchemeServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make Directory.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> makeDirectory(
        tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getMakeDirectoryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> removeDirectory(
        tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveDirectoryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> listDirectory(
        tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListDirectoryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> describePath(
        tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribePathMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> modifyPermissions(
        tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyPermissionsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_MAKE_DIRECTORY = 0;
  private static final int METHODID_REMOVE_DIRECTORY = 1;
  private static final int METHODID_LIST_DIRECTORY = 2;
  private static final int METHODID_DESCRIBE_PATH = 3;
  private static final int METHODID_MODIFY_PERMISSIONS = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final SchemeServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(SchemeServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_MAKE_DIRECTORY:
          serviceImpl.makeDirectory((tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse>) responseObserver);
          break;
        case METHODID_REMOVE_DIRECTORY:
          serviceImpl.removeDirectory((tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse>) responseObserver);
          break;
        case METHODID_LIST_DIRECTORY:
          serviceImpl.listDirectory((tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_PATH:
          serviceImpl.describePath((tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse>) responseObserver);
          break;
        case METHODID_MODIFY_PERMISSIONS:
          serviceImpl.modifyPermissions((tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse>) responseObserver);
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

  private static abstract class SchemeServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SchemeServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.scheme.v1.YdbSchemeV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SchemeService");
    }
  }

  private static final class SchemeServiceFileDescriptorSupplier
      extends SchemeServiceBaseDescriptorSupplier {
    SchemeServiceFileDescriptorSupplier() {}
  }

  private static final class SchemeServiceMethodDescriptorSupplier
      extends SchemeServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    SchemeServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (SchemeServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SchemeServiceFileDescriptorSupplier())
              .addMethod(getMakeDirectoryMethod())
              .addMethod(getRemoveDirectoryMethod())
              .addMethod(getListDirectoryMethod())
              .addMethod(getDescribePathMethod())
              .addMethod(getModifyPermissionsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
