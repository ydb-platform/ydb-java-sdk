package tech.ydb.rate_limiter.v1;

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
 * <pre>
 * Control plane API
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: ydb/public/api/grpc/ydb_rate_limiter_v1.proto")
public final class RateLimiterServiceGrpc {

  private RateLimiterServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.RateLimiter.V1.RateLimiterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.CreateResourceRequest,
      tech.ydb.rate_limiter.CreateResourceResponse> getCreateResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateResource",
      requestType = tech.ydb.rate_limiter.CreateResourceRequest.class,
      responseType = tech.ydb.rate_limiter.CreateResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.CreateResourceRequest,
      tech.ydb.rate_limiter.CreateResourceResponse> getCreateResourceMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.CreateResourceRequest, tech.ydb.rate_limiter.CreateResourceResponse> getCreateResourceMethod;
    if ((getCreateResourceMethod = RateLimiterServiceGrpc.getCreateResourceMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getCreateResourceMethod = RateLimiterServiceGrpc.getCreateResourceMethod) == null) {
          RateLimiterServiceGrpc.getCreateResourceMethod = getCreateResourceMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.CreateResourceRequest, tech.ydb.rate_limiter.CreateResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.CreateResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.CreateResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("CreateResource"))
              .build();
        }
      }
    }
    return getCreateResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AlterResourceRequest,
      tech.ydb.rate_limiter.AlterResourceResponse> getAlterResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterResource",
      requestType = tech.ydb.rate_limiter.AlterResourceRequest.class,
      responseType = tech.ydb.rate_limiter.AlterResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AlterResourceRequest,
      tech.ydb.rate_limiter.AlterResourceResponse> getAlterResourceMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AlterResourceRequest, tech.ydb.rate_limiter.AlterResourceResponse> getAlterResourceMethod;
    if ((getAlterResourceMethod = RateLimiterServiceGrpc.getAlterResourceMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getAlterResourceMethod = RateLimiterServiceGrpc.getAlterResourceMethod) == null) {
          RateLimiterServiceGrpc.getAlterResourceMethod = getAlterResourceMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.AlterResourceRequest, tech.ydb.rate_limiter.AlterResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.AlterResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.AlterResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("AlterResource"))
              .build();
        }
      }
    }
    return getAlterResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DropResourceRequest,
      tech.ydb.rate_limiter.DropResourceResponse> getDropResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropResource",
      requestType = tech.ydb.rate_limiter.DropResourceRequest.class,
      responseType = tech.ydb.rate_limiter.DropResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DropResourceRequest,
      tech.ydb.rate_limiter.DropResourceResponse> getDropResourceMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DropResourceRequest, tech.ydb.rate_limiter.DropResourceResponse> getDropResourceMethod;
    if ((getDropResourceMethod = RateLimiterServiceGrpc.getDropResourceMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getDropResourceMethod = RateLimiterServiceGrpc.getDropResourceMethod) == null) {
          RateLimiterServiceGrpc.getDropResourceMethod = getDropResourceMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.DropResourceRequest, tech.ydb.rate_limiter.DropResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.DropResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.DropResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("DropResource"))
              .build();
        }
      }
    }
    return getDropResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.ListResourcesRequest,
      tech.ydb.rate_limiter.ListResourcesResponse> getListResourcesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListResources",
      requestType = tech.ydb.rate_limiter.ListResourcesRequest.class,
      responseType = tech.ydb.rate_limiter.ListResourcesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.ListResourcesRequest,
      tech.ydb.rate_limiter.ListResourcesResponse> getListResourcesMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.ListResourcesRequest, tech.ydb.rate_limiter.ListResourcesResponse> getListResourcesMethod;
    if ((getListResourcesMethod = RateLimiterServiceGrpc.getListResourcesMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getListResourcesMethod = RateLimiterServiceGrpc.getListResourcesMethod) == null) {
          RateLimiterServiceGrpc.getListResourcesMethod = getListResourcesMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.ListResourcesRequest, tech.ydb.rate_limiter.ListResourcesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListResources"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.ListResourcesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.ListResourcesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("ListResources"))
              .build();
        }
      }
    }
    return getListResourcesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DescribeResourceRequest,
      tech.ydb.rate_limiter.DescribeResourceResponse> getDescribeResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeResource",
      requestType = tech.ydb.rate_limiter.DescribeResourceRequest.class,
      responseType = tech.ydb.rate_limiter.DescribeResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DescribeResourceRequest,
      tech.ydb.rate_limiter.DescribeResourceResponse> getDescribeResourceMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.DescribeResourceRequest, tech.ydb.rate_limiter.DescribeResourceResponse> getDescribeResourceMethod;
    if ((getDescribeResourceMethod = RateLimiterServiceGrpc.getDescribeResourceMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getDescribeResourceMethod = RateLimiterServiceGrpc.getDescribeResourceMethod) == null) {
          RateLimiterServiceGrpc.getDescribeResourceMethod = getDescribeResourceMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.DescribeResourceRequest, tech.ydb.rate_limiter.DescribeResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.DescribeResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.DescribeResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("DescribeResource"))
              .build();
        }
      }
    }
    return getDescribeResourceMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AcquireResourceRequest,
      tech.ydb.rate_limiter.AcquireResourceResponse> getAcquireResourceMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquireResource",
      requestType = tech.ydb.rate_limiter.AcquireResourceRequest.class,
      responseType = tech.ydb.rate_limiter.AcquireResourceResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AcquireResourceRequest,
      tech.ydb.rate_limiter.AcquireResourceResponse> getAcquireResourceMethod() {
    io.grpc.MethodDescriptor<tech.ydb.rate_limiter.AcquireResourceRequest, tech.ydb.rate_limiter.AcquireResourceResponse> getAcquireResourceMethod;
    if ((getAcquireResourceMethod = RateLimiterServiceGrpc.getAcquireResourceMethod) == null) {
      synchronized (RateLimiterServiceGrpc.class) {
        if ((getAcquireResourceMethod = RateLimiterServiceGrpc.getAcquireResourceMethod) == null) {
          RateLimiterServiceGrpc.getAcquireResourceMethod = getAcquireResourceMethod =
              io.grpc.MethodDescriptor.<tech.ydb.rate_limiter.AcquireResourceRequest, tech.ydb.rate_limiter.AcquireResourceResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquireResource"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.AcquireResourceRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.rate_limiter.AcquireResourceResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RateLimiterServiceMethodDescriptorSupplier("AcquireResource"))
              .build();
        }
      }
    }
    return getAcquireResourceMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RateLimiterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceStub>() {
        @java.lang.Override
        public RateLimiterServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceStub(channel, callOptions);
        }
      };
    return RateLimiterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RateLimiterServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceBlockingStub>() {
        @java.lang.Override
        public RateLimiterServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceBlockingStub(channel, callOptions);
        }
      };
    return RateLimiterServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RateLimiterServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RateLimiterServiceFutureStub>() {
        @java.lang.Override
        public RateLimiterServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RateLimiterServiceFutureStub(channel, callOptions);
        }
      };
    return RateLimiterServiceFutureStub.newStub(factory, channel);
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
      asyncUnimplementedUnaryCall(getCreateResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public void alterResource(tech.ydb.rate_limiter.AlterResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AlterResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public void dropResource(tech.ydb.rate_limiter.DropResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DropResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public void listResources(tech.ydb.rate_limiter.ListResourcesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.ListResourcesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListResourcesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public void describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DescribeResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeResourceMethod(), responseObserver);
    }

    /**
     * <pre>
     * Take units for usage of a resource in coordination node.
     * </pre>
     */
    public void acquireResource(tech.ydb.rate_limiter.AcquireResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AcquireResourceResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAcquireResourceMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.CreateResourceRequest,
                tech.ydb.rate_limiter.CreateResourceResponse>(
                  this, METHODID_CREATE_RESOURCE)))
          .addMethod(
            getAlterResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.AlterResourceRequest,
                tech.ydb.rate_limiter.AlterResourceResponse>(
                  this, METHODID_ALTER_RESOURCE)))
          .addMethod(
            getDropResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.DropResourceRequest,
                tech.ydb.rate_limiter.DropResourceResponse>(
                  this, METHODID_DROP_RESOURCE)))
          .addMethod(
            getListResourcesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.ListResourcesRequest,
                tech.ydb.rate_limiter.ListResourcesResponse>(
                  this, METHODID_LIST_RESOURCES)))
          .addMethod(
            getDescribeResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.DescribeResourceRequest,
                tech.ydb.rate_limiter.DescribeResourceResponse>(
                  this, METHODID_DESCRIBE_RESOURCE)))
          .addMethod(
            getAcquireResourceMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.rate_limiter.AcquireResourceRequest,
                tech.ydb.rate_limiter.AcquireResourceResponse>(
                  this, METHODID_ACQUIRE_RESOURCE)))
          .build();
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceStub extends io.grpc.stub.AbstractAsyncStub<RateLimiterServiceStub> {
    private RateLimiterServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getCreateResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public void alterResource(tech.ydb.rate_limiter.AlterResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AlterResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public void dropResource(tech.ydb.rate_limiter.DropResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DropResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public void listResources(tech.ydb.rate_limiter.ListResourcesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.ListResourcesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListResourcesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public void describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.DescribeResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeResourceMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Take units for usage of a resource in coordination node.
     * </pre>
     */
    public void acquireResource(tech.ydb.rate_limiter.AcquireResourceRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AcquireResourceResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAcquireResourceMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<RateLimiterServiceBlockingStub> {
    private RateLimiterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RateLimiterServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create a new resource in existing coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.CreateResourceResponse createResource(tech.ydb.rate_limiter.CreateResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.AlterResourceResponse alterResource(tech.ydb.rate_limiter.AlterResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.DropResourceResponse dropResource(tech.ydb.rate_limiter.DropResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.ListResourcesResponse listResources(tech.ydb.rate_limiter.ListResourcesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListResourcesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.DescribeResourceResponse describeResource(tech.ydb.rate_limiter.DescribeResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeResourceMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Take units for usage of a resource in coordination node.
     * </pre>
     */
    public tech.ydb.rate_limiter.AcquireResourceResponse acquireResource(tech.ydb.rate_limiter.AcquireResourceRequest request) {
      return blockingUnaryCall(
          getChannel(), getAcquireResourceMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Control plane API
   * </pre>
   */
  public static final class RateLimiterServiceFutureStub extends io.grpc.stub.AbstractFutureStub<RateLimiterServiceFutureStub> {
    private RateLimiterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RateLimiterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getCreateResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Update a resource in coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.AlterResourceResponse> alterResource(
        tech.ydb.rate_limiter.AlterResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a resource from coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.DropResourceResponse> dropResource(
        tech.ydb.rate_limiter.DropResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * List resources in given coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.ListResourcesResponse> listResources(
        tech.ydb.rate_limiter.ListResourcesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListResourcesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe properties of resource in coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.DescribeResourceResponse> describeResource(
        tech.ydb.rate_limiter.DescribeResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeResourceMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Take units for usage of a resource in coordination node.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.rate_limiter.AcquireResourceResponse> acquireResource(
        tech.ydb.rate_limiter.AcquireResourceRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAcquireResourceMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_RESOURCE = 0;
  private static final int METHODID_ALTER_RESOURCE = 1;
  private static final int METHODID_DROP_RESOURCE = 2;
  private static final int METHODID_LIST_RESOURCES = 3;
  private static final int METHODID_DESCRIBE_RESOURCE = 4;
  private static final int METHODID_ACQUIRE_RESOURCE = 5;

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
        case METHODID_ACQUIRE_RESOURCE:
          serviceImpl.acquireResource((tech.ydb.rate_limiter.AcquireResourceRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.rate_limiter.AcquireResourceResponse>) responseObserver);
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

  private static abstract class RateLimiterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RateLimiterServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.rate_limiter.v1.RateLimiterGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RateLimiterService");
    }
  }

  private static final class RateLimiterServiceFileDescriptorSupplier
      extends RateLimiterServiceBaseDescriptorSupplier {
    RateLimiterServiceFileDescriptorSupplier() {}
  }

  private static final class RateLimiterServiceMethodDescriptorSupplier
      extends RateLimiterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    RateLimiterServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (RateLimiterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RateLimiterServiceFileDescriptorSupplier())
              .addMethod(getCreateResourceMethod())
              .addMethod(getAlterResourceMethod())
              .addMethod(getDropResourceMethod())
              .addMethod(getListResourcesMethod())
              .addMethod(getDescribeResourceMethod())
              .addMethod(getAcquireResourceMethod())
              .build();
        }
      }
    }
    return result;
  }
}
