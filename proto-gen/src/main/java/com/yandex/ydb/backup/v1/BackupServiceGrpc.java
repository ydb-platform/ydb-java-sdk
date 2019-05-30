package tech.ydb.backup.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_backup_v1.proto")
public final class BackupServiceGrpc {

  private BackupServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Backup.V1.BackupService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.backup.YdbBackup.CreateBackupRequest,
      tech.ydb.backup.YdbBackup.CreateBackupResponse> METHOD_CREATE_BACKUP =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Backup.V1.BackupService", "CreateBackup"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.backup.YdbBackup.CreateBackupRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.backup.YdbBackup.CreateBackupResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static BackupServiceStub newStub(io.grpc.Channel channel) {
    return new BackupServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static BackupServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new BackupServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static BackupServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new BackupServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class BackupServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Creates backup.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void createBackup(tech.ydb.backup.YdbBackup.CreateBackupRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.backup.YdbBackup.CreateBackupResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_BACKUP, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_BACKUP,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.backup.YdbBackup.CreateBackupRequest,
                tech.ydb.backup.YdbBackup.CreateBackupResponse>(
                  this, METHODID_CREATE_BACKUP)))
          .build();
    }
  }

  /**
   */
  public static final class BackupServiceStub extends io.grpc.stub.AbstractStub<BackupServiceStub> {
    private BackupServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BackupServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BackupServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BackupServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates backup.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public void createBackup(tech.ydb.backup.YdbBackup.CreateBackupRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.backup.YdbBackup.CreateBackupResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_BACKUP, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class BackupServiceBlockingStub extends io.grpc.stub.AbstractStub<BackupServiceBlockingStub> {
    private BackupServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BackupServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BackupServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BackupServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates backup.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public tech.ydb.backup.YdbBackup.CreateBackupResponse createBackup(tech.ydb.backup.YdbBackup.CreateBackupRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_BACKUP, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class BackupServiceFutureStub extends io.grpc.stub.AbstractStub<BackupServiceFutureStub> {
    private BackupServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private BackupServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected BackupServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new BackupServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates backup.
     * Method starts an asynchronous operation that can be cancelled while it is in progress.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.backup.YdbBackup.CreateBackupResponse> createBackup(
        tech.ydb.backup.YdbBackup.CreateBackupRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_BACKUP, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_BACKUP = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final BackupServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(BackupServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_BACKUP:
          serviceImpl.createBackup((tech.ydb.backup.YdbBackup.CreateBackupRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.backup.YdbBackup.CreateBackupResponse>) responseObserver);
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

  private static final class BackupServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.backup.v1.YdbBackupV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (BackupServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new BackupServiceDescriptorSupplier())
              .addMethod(METHOD_CREATE_BACKUP)
              .build();
        }
      }
    }
    return result;
  }
}
