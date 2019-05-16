package tech.ydb.scheme.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_scheme_v1.proto")
public final class SchemeServiceGrpc {

  private SchemeServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Scheme.V1.SchemeService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse> METHOD_MAKE_DIRECTORY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scheme.V1.SchemeService", "MakeDirectory"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> METHOD_REMOVE_DIRECTORY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scheme.V1.SchemeService", "RemoveDirectory"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest,
      tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> METHOD_LIST_DIRECTORY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scheme.V1.SchemeService", "ListDirectory"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest,
      tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> METHOD_DESCRIBE_PATH =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scheme.V1.SchemeService", "DescribePath"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest,
      tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> METHOD_MODIFY_PERMISSIONS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Scheme.V1.SchemeService", "ModifyPermissions"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SchemeServiceStub newStub(io.grpc.Channel channel) {
    return new SchemeServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SchemeServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new SchemeServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static SchemeServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new SchemeServiceFutureStub(channel);
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
      asyncUnimplementedUnaryCall(METHOD_MAKE_DIRECTORY, responseObserver);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public void removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_REMOVE_DIRECTORY, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public void listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_DIRECTORY, responseObserver);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public void describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_PATH, responseObserver);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public void modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_MODIFY_PERMISSIONS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_MAKE_DIRECTORY,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse>(
                  this, METHODID_MAKE_DIRECTORY)))
          .addMethod(
            METHOD_REMOVE_DIRECTORY,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse>(
                  this, METHODID_REMOVE_DIRECTORY)))
          .addMethod(
            METHOD_LIST_DIRECTORY,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest,
                tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse>(
                  this, METHODID_LIST_DIRECTORY)))
          .addMethod(
            METHOD_DESCRIBE_PATH,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest,
                tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse>(
                  this, METHODID_DESCRIBE_PATH)))
          .addMethod(
            METHOD_MODIFY_PERMISSIONS,
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
  public static final class SchemeServiceStub extends io.grpc.stub.AbstractStub<SchemeServiceStub> {
    private SchemeServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SchemeServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_MAKE_DIRECTORY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public void removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_REMOVE_DIRECTORY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public void listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_DIRECTORY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public void describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_PATH, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public void modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_MODIFY_PERMISSIONS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class SchemeServiceBlockingStub extends io.grpc.stub.AbstractStub<SchemeServiceBlockingStub> {
    private SchemeServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SchemeServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new SchemeServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make Directory.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryResponse makeDirectory(tech.ydb.scheme.SchemeOperationProtos.MakeDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_MAKE_DIRECTORY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse removeDirectory(tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_REMOVE_DIRECTORY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse listDirectory(tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_DIRECTORY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse describePath(tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_PATH, getCallOptions(), request);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse modifyPermissions(tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_MODIFY_PERMISSIONS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class SchemeServiceFutureStub extends io.grpc.stub.AbstractStub<SchemeServiceFutureStub> {
    private SchemeServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private SchemeServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SchemeServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_MAKE_DIRECTORY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove Directory.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryResponse> removeDirectory(
        tech.ydb.scheme.SchemeOperationProtos.RemoveDirectoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_REMOVE_DIRECTORY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about given directory and objects inside it.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.ListDirectoryResponse> listDirectory(
        tech.ydb.scheme.SchemeOperationProtos.ListDirectoryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_DIRECTORY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about object with given path.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.DescribePathResponse> describePath(
        tech.ydb.scheme.SchemeOperationProtos.DescribePathRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_PATH, getCallOptions()), request);
    }

    /**
     * <pre>
     * Modify permissions.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsResponse> modifyPermissions(
        tech.ydb.scheme.SchemeOperationProtos.ModifyPermissionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_MODIFY_PERMISSIONS, getCallOptions()), request);
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

  private static final class SchemeServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.scheme.v1.YdbSchemeV1.getDescriptor();
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
              .setSchemaDescriptor(new SchemeServiceDescriptorSupplier())
              .addMethod(METHOD_MAKE_DIRECTORY)
              .addMethod(METHOD_REMOVE_DIRECTORY)
              .addMethod(METHOD_LIST_DIRECTORY)
              .addMethod(METHOD_DESCRIBE_PATH)
              .addMethod(METHOD_MODIFY_PERMISSIONS)
              .build();
        }
      }
    }
    return result;
  }
}
