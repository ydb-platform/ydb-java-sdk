package tech.ydb.s3_internal.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_s3_internal_v1.proto")
public final class S3InternalServiceGrpc {

  private S3InternalServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.S3Internal.V1.S3InternalService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest,
      tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse> METHOD_S3LISTING =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.S3Internal.V1.S3InternalService", "S3Listing"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static S3InternalServiceStub newStub(io.grpc.Channel channel) {
    return new S3InternalServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static S3InternalServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new S3InternalServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static S3InternalServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new S3InternalServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class S3InternalServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void s3Listing(tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_S3LISTING, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_S3LISTING,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest,
                tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse>(
                  this, METHODID_S3LISTING)))
          .build();
    }
  }

  /**
   */
  public static final class S3InternalServiceStub extends io.grpc.stub.AbstractStub<S3InternalServiceStub> {
    private S3InternalServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private S3InternalServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected S3InternalServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new S3InternalServiceStub(channel, callOptions);
    }

    /**
     */
    public void s3Listing(tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_S3LISTING, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class S3InternalServiceBlockingStub extends io.grpc.stub.AbstractStub<S3InternalServiceBlockingStub> {
    private S3InternalServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private S3InternalServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected S3InternalServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new S3InternalServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse s3Listing(tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_S3LISTING, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class S3InternalServiceFutureStub extends io.grpc.stub.AbstractStub<S3InternalServiceFutureStub> {
    private S3InternalServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private S3InternalServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected S3InternalServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new S3InternalServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse> s3Listing(
        tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_S3LISTING, getCallOptions()), request);
    }
  }

  private static final int METHODID_S3LISTING = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final S3InternalServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(S3InternalServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_S3LISTING:
          serviceImpl.s3Listing((tech.ydb.s3_internal.S3InternalProtos.S3ListingRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.s3_internal.S3InternalProtos.S3ListingResponse>) responseObserver);
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

  private static final class S3InternalServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.s3_internal.v1.YdbS3InternalV1.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (S3InternalServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new S3InternalServiceDescriptorSupplier())
              .addMethod(METHOD_S3LISTING)
              .build();
        }
      }
    }
    return result;
  }
}
