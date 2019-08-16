package tech.ydb.persqueue.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_persqueue_v1.proto")
public final class ClusterDiscoveryServiceGrpc {

  private ClusterDiscoveryServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.PersQueue.V1.ClusterDiscoveryService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest,
      tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse> METHOD_DISCOVER_CLUSTERS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.PersQueue.V1.ClusterDiscoveryService", "DiscoverClusters"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ClusterDiscoveryServiceStub newStub(io.grpc.Channel channel) {
    return new ClusterDiscoveryServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ClusterDiscoveryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ClusterDiscoveryServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static ClusterDiscoveryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ClusterDiscoveryServiceFutureStub(channel);
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
      asyncUnimplementedUnaryCall(METHOD_DISCOVER_CLUSTERS, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_DISCOVER_CLUSTERS,
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
  public static final class ClusterDiscoveryServiceStub extends io.grpc.stub.AbstractStub<ClusterDiscoveryServiceStub> {
    private ClusterDiscoveryServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterDiscoveryServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_DISCOVER_CLUSTERS, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class ClusterDiscoveryServiceBlockingStub extends io.grpc.stub.AbstractStub<ClusterDiscoveryServiceBlockingStub> {
    private ClusterDiscoveryServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterDiscoveryServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ClusterDiscoveryServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get PQ clusters which are eligible for the specified Write or Read Sessions
     * </pre>
     */
    public tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersResponse discoverClusters(tech.ydb.persqueue.cluster_discovery.YdbPersqueueClusterDiscovery.DiscoverClustersRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DISCOVER_CLUSTERS, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class ClusterDiscoveryServiceFutureStub extends io.grpc.stub.AbstractStub<ClusterDiscoveryServiceFutureStub> {
    private ClusterDiscoveryServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ClusterDiscoveryServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ClusterDiscoveryServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(METHOD_DISCOVER_CLUSTERS, getCallOptions()), request);
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

  private static final class ClusterDiscoveryServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.persqueue.v1.YdbPersqueueV1.getDescriptor();
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
              .setSchemaDescriptor(new ClusterDiscoveryServiceDescriptorSupplier())
              .addMethod(METHOD_DISCOVER_CLUSTERS)
              .build();
        }
      }
    }
    return result;
  }
}
