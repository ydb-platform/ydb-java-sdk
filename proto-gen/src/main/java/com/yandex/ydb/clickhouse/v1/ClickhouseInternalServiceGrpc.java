package tech.ydb.clickhouse.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_clickhouse_internal_v1.proto")
public final class ClickhouseInternalServiceGrpc {

  private ClickhouseInternalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.ClickhouseInternal.V1.ClickhouseInternalService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest,
      tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse> METHOD_SCAN =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.ClickhouseInternal.V1.ClickhouseInternalService", "Scan"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest,
      tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse> METHOD_GET_SHARD_LOCATIONS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.ClickhouseInternal.V1.ClickhouseInternalService", "GetShardLocations"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest,
      tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse> METHOD_DESCRIBE_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.ClickhouseInternal.V1.ClickhouseInternalService", "DescribeTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClickhouseInternalServiceStub newStub(io.grpc.Channel channel) {
    return new ClickhouseInternalServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClickhouseInternalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ClickhouseInternalServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ClickhouseInternalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ClickhouseInternalServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class ClickhouseInternalServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void scan(tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_SCAN, responseObserver);
    }

    /**
     */
    public void getShardLocations(tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_GET_SHARD_LOCATIONS, responseObserver);
    }

    /**
     */
    public void describeTable(tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_TABLE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SCAN,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest,
                tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse>(
                  this, METHODID_SCAN)))
          .addMethod(
            METHOD_GET_SHARD_LOCATIONS,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest,
                tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse>(
                  this, METHODID_GET_SHARD_LOCATIONS)))
          .addMethod(
            METHOD_DESCRIBE_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest,
                tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse>(
                  this, METHODID_DESCRIBE_TABLE)))
          .build();
    }
  }

  /**
   */
  public static final class ClickhouseInternalServiceStub extends io.grpc.stub.AbstractStub<ClickhouseInternalServiceStub> {
    private ClickhouseInternalServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClickhouseInternalServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClickhouseInternalServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClickhouseInternalServiceStub(channel, callOptions);
    }

    /**
     */
    public void scan(tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_SCAN, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getShardLocations(tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_GET_SHARD_LOCATIONS, getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeTable(tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ClickhouseInternalServiceBlockingStub extends io.grpc.stub.AbstractStub<ClickhouseInternalServiceBlockingStub> {
    private ClickhouseInternalServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClickhouseInternalServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClickhouseInternalServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClickhouseInternalServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse scan(tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_SCAN, getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse getShardLocations(tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_GET_SHARD_LOCATIONS, getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse describeTable(tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_TABLE, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ClickhouseInternalServiceFutureStub extends io.grpc.stub.AbstractStub<ClickhouseInternalServiceFutureStub> {
    private ClickhouseInternalServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClickhouseInternalServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClickhouseInternalServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClickhouseInternalServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse> scan(
        tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_SCAN, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse> getShardLocations(
        tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_GET_SHARD_LOCATIONS, getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse> describeTable(
        tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE, getCallOptions()), request);
    }
  }

  private static final int METHODID_SCAN = 0;
  private static final int METHODID_GET_SHARD_LOCATIONS = 1;
  private static final int METHODID_DESCRIBE_TABLE = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ClickhouseInternalServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ClickhouseInternalServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SCAN:
          serviceImpl.scan((tech.ydb.clickhouse.ClickhouseInternalProtos.ScanRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.ScanResponse>) responseObserver);
          break;
        case METHODID_GET_SHARD_LOCATIONS:
          serviceImpl.getShardLocations((tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.GetShardLocationsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TABLE:
          serviceImpl.describeTable((tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.clickhouse.ClickhouseInternalProtos.DescribeTableResponse>) responseObserver);
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

  private static final class ClickhouseInternalServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.clickhouse.v1.YdbClickhouseInternalV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ClickhouseInternalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClickhouseInternalServiceDescriptorSupplier())
              .addMethod(METHOD_SCAN)
              .addMethod(METHOD_GET_SHARD_LOCATIONS)
              .addMethod(METHOD_DESCRIBE_TABLE)
              .build();
        }
      }
    }
    return result;
  }
}
