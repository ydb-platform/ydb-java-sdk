package tech.ydb.rate_limiter.v1;

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
 * <pre>
 * Control plane API
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: kikimr/public/api/grpc/ydb_rate_limiter_v1.proto")
public final class RateLimiterServiceGrpc {

  private RateLimiterServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.RateLimiter.V1.RateLimiterService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.rate_limiter.CreateResourceRequest,
      tech.ydb.rate_limiter.CreateResourceResponse> METHOD_CREATE_RESOURCE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.RateLimiter.V1.RateLimiterService", "CreateResource"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.CreateResourceRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.CreateResourceResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AlterResourceRequest,
      tech.ydb.rate_limiter.AlterResourceResponse> METHOD_ALTER_RESOURCE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.RateLimiter.V1.RateLimiterService", "AlterResource"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.AlterResourceRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.AlterResourceResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DropResourceRequest,
      tech.ydb.rate_limiter.DropResourceResponse> METHOD_DROP_RESOURCE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.RateLimiter.V1.RateLimiterService", "DropResource"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.DropResourceRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.DropResourceResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.rate_limiter.ListResourcesRequest,
      tech.ydb.rate_limiter.ListResourcesResponse> METHOD_LIST_RESOURCES =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.RateLimiter.V1.RateLimiterService", "ListResources"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.ListResourcesRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.ListResourcesResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DescribeResourceRequest,
      tech.ydb.rate_limiter.DescribeResourceResponse> METHOD_DESCRIBE_RESOURCE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.RateLimiter.V1.RateLimiterService", "DescribeResource"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.DescribeResourceRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.rate_limiter.DescribeResourceResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RateLimiterServiceStub newStub(io.grpc.Channel channel) {
    return new RateLimiterServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimiterServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new RateLimiterServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimiterServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new RateLimiterServiceFutureStub(channel);
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static abstract class RateLimiterServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Create a new resource in existing coordination node.
     * </pre>
     */
    public void createResource(tech.ydb.rate_limiter.CreateResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.CreateResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_RESOURCE, responseObserver);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public void alterResource(tech.ydb.rate_limiter.AlterResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AlterResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ALTER_RESOURCE, responseObserver);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public void dropResource(tech.ydb.rate_limiter.DropResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DropResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DROP_RESOURCE, responseObserver);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public void listResources(tech.ydb.rate_limiter.ListResourcesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.ListResourcesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_LIST_RESOURCES, responseObserver);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public void describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DescribeResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_RESOURCE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_RESOURCE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.CreateResourceRequest,
                tech.ydb.rate_limiter.CreateResourceResponse>(
                  this, METHODID_CREATE_RESOURCE)))
          .addMethod(
            METHOD_ALTER_RESOURCE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.AlterResourceRequest,
                tech.ydb.rate_limiter.AlterResourceResponse>(
                  this, METHODID_ALTER_RESOURCE)))
          .addMethod(
            METHOD_DROP_RESOURCE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.DropResourceRequest,
                tech.ydb.rate_limiter.DropResourceResponse>(
                  this, METHODID_DROP_RESOURCE)))
          .addMethod(
            METHOD_LIST_RESOURCES,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.ListResourcesRequest,
                tech.ydb.rate_limiter.ListResourcesResponse>(
                  this, METHODID_LIST_RESOURCES)))
          .addMethod(
            METHOD_DESCRIBE_RESOURCE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.DescribeResourceRequest,
                tech.ydb.rate_limiter.DescribeResourceResponse>(
                  this, METHODID_DESCRIBE_RESOURCE)))
          .build();
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceStub extends io.grpc.stub.AbstractStub<RateLimiterServiceStub> {
    private RateLimiterServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RateLimiterServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new resource in existing coordination node.
     * </pre>
     */
    public void createResource(tech.ydb.rate_limiter.CreateResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.CreateResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_RESOURCE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public void alterResource(tech.ydb.rate_limiter.AlterResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AlterResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ALTER_RESOURCE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public void dropResource(tech.ydb.rate_limiter.DropResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DropResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DROP_RESOURCE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public void listResources(tech.ydb.rate_limiter.ListResourcesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.ListResourcesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_LIST_RESOURCES, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public void describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DescribeResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_RESOURCE, getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceBlockingStub extends io.grpc.stub.AbstractStub<RateLimiterServiceBlockingStub> {
    private RateLimiterServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RateLimiterServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new resource in existing coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.CreateResourceResponse createResource(tech.ydb.rate_limiter.CreateResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_RESOURCE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.AlterResourceResponse alterResource(tech.ydb.rate_limiter.AlterResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ALTER_RESOURCE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.DropResourceResponse dropResource(tech.ydb.rate_limiter.DropResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DROP_RESOURCE, getCallOptions(), request);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.ListResourcesResponse listResources(tech.ydb.rate_limiter.ListResourcesRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_LIST_RESOURCES, getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.DescribeResourceResponse describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_RESOURCE, getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceFutureStub extends io.grpc.stub.AbstractStub<RateLimiterServiceFutureStub> {
    private RateLimiterServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private RateLimiterServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new resource in existing coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.CreateResourceResponse> createResource(
        tech.ydb.rate_limiter.CreateResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_RESOURCE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.AlterResourceResponse> alterResource(
        tech.ydb.rate_limiter.AlterResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ALTER_RESOURCE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.DropResourceResponse> dropResource(
        tech.ydb.rate_limiter.DropResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DROP_RESOURCE, getCallOptions()), request);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.ListResourcesResponse> listResources(
        tech.ydb.rate_limiter.ListResourcesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_LIST_RESOURCES, getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.DescribeResourceResponse> describeResource(
        tech.ydb.rate_limiter.DescribeResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_RESOURCE, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_RESOURCE = 0;
  private static final int METHODID_ALTER_RESOURCE = 1;
  private static final int METHODID_DROP_RESOURCE = 2;
  private static final int METHODID_LIST_RESOURCES = 3;
  private static final int METHODID_DESCRIBE_RESOURCE = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final RateLimiterServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(RateLimiterServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_RESOURCE:
          serviceImpl.createResource((tech.ydb.rate_limiter.CreateResourceRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.CreateResourceResponse>) responseObserver);
          break;
        case METHODID_ALTER_RESOURCE:
          serviceImpl.alterResource((tech.ydb.rate_limiter.AlterResourceRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AlterResourceResponse>) responseObserver);
          break;
        case METHODID_DROP_RESOURCE:
          serviceImpl.dropResource((tech.ydb.rate_limiter.DropResourceRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DropResourceResponse>) responseObserver);
          break;
        case METHODID_LIST_RESOURCES:
          serviceImpl.listResources((tech.ydb.rate_limiter.ListResourcesRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.ListResourcesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_RESOURCE:
          serviceImpl.describeResource((tech.ydb.rate_limiter.DescribeResourceRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DescribeResourceResponse>) responseObserver);
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

  private static final class RateLimiterServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.rate_limiter.v1.RateLimiterGrpc.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RateLimiterServiceDescriptorSupplier())
              .addMethod(METHOD_CREATE_RESOURCE)
              .addMethod(METHOD_ALTER_RESOURCE)
              .addMethod(METHOD_DROP_RESOURCE)
              .addMethod(METHOD_LIST_RESOURCES)
              .addMethod(METHOD_DESCRIBE_RESOURCE)
              .build();
        }
      }
    }
    return result;
  }
}
