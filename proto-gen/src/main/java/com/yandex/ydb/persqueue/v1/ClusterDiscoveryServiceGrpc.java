package tech.ydb.persqueue.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_persqueue_v1.proto")
public final class ClusterDiscoveryServiceGrpc {

  private ClusterDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.PersQueue.V1.ClusterDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest,
      tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> getDiscoverClustersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DiscoverClusters",
      requestType = tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest.class,
      responseType = tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest,
      tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> getDiscoverClustersMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest, tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> getDiscoverClustersMethod;
    if ((getDiscoverClustersMethod = ClusterDiscoveryServiceGrpc.getDiscoverClustersMethod) == null) {
      synchronized (ClusterDiscoveryServiceGrpc.class) {
        if ((getDiscoverClustersMethod = ClusterDiscoveryServiceGrpc.getDiscoverClustersMethod) == null) {
          ClusterDiscoveryServiceGrpc.getDiscoverClustersMethod = getDiscoverClustersMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest, tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DiscoverClusters"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ClusterDiscoveryServiceMethodDescriptorSupplier("DiscoverClusters"))
              .build();
        }
      }
    }
    return getDiscoverClustersMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClusterDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceStub>() {
        @java.lang.Override
        public ClusterDiscoveryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterDiscoveryServiceStub(channel, callOptions);
        }
      };
    return ClusterDiscoveryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClusterDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceBlockingStub>() {
        @java.lang.Override
        public ClusterDiscoveryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterDiscoveryServiceBlockingStub(channel, callOptions);
        }
      };
    return ClusterDiscoveryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ClusterDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ClusterDiscoveryServiceFutureStub>() {
        @java.lang.Override
        public ClusterDiscoveryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ClusterDiscoveryServiceFutureStub(channel, callOptions);
        }
      };
    return ClusterDiscoveryServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class ClusterDiscoveryServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Get PQ clusters which are eligible for the specified Write or Read Sessions
     * </pre>
     */
    public void discoverClusters(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDiscoverClustersMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getDiscoverClustersMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest,
                tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse>(
                  this, METHODID_DISCOVER_CLUSTERS)))
          .build();
    }
  }

  /**
   */
  public static final class ClusterDiscoveryServiceStub extends io.grpc.stub.AbstractAsyncStub<ClusterDiscoveryServiceStub> {
    private ClusterDiscoveryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterDiscoveryServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get PQ clusters which are eligible for the specified Write or Read Sessions
     * </pre>
     */
    public void discoverClusters(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDiscoverClustersMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ClusterDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<ClusterDiscoveryServiceBlockingStub> {
    private ClusterDiscoveryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterDiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get PQ clusters which are eligible for the specified Write or Read Sessions
     * </pre>
     */
    public tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse discoverClusters(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest request) {
      return blockingUnaryCall(
          getChannel(), getDiscoverClustersMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ClusterDiscoveryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<ClusterDiscoveryServiceFutureStub> {
    private ClusterDiscoveryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ClusterDiscoveryServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get PQ clusters which are eligible for the specified Write or Read Sessions
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> discoverClusters(
        tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDiscoverClustersMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_DISCOVER_CLUSTERS = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ClusterDiscoveryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ClusterDiscoveryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_DISCOVER_CLUSTERS:
          serviceImpl.discoverClusters((tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse>) responseObserver);
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

  private static abstract class ClusterDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ClusterDiscoveryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.persqueue.v1.YdbPersqueueV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ClusterDiscoveryService");
    }
  }

  private static final class ClusterDiscoveryServiceFileDescriptorSupplier
      extends ClusterDiscoveryServiceBaseDescriptorSupplier {
    ClusterDiscoveryServiceFileDescriptorSupplier() {}
  }

  private static final class ClusterDiscoveryServiceMethodDescriptorSupplier
      extends ClusterDiscoveryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ClusterDiscoveryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (ClusterDiscoveryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ClusterDiscoveryServiceFileDescriptorSupplier())
              .addMethod(getDiscoverClustersMethod())
              .build();
        }
      }
    }
    return result;
  }
}
