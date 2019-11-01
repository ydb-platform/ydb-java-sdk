package tech.ydb.coordination.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_coordination_v1.proto")
public final class CoordinationServiceGrpc {

  private CoordinationServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Coordination.V1.CoordinationService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.coordination.SessionRequest,
      tech.ydb.coordination.SessionResponse> METHOD_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
          generateFullMethodName(
              "Ydb.Coordination.V1.CoordinationService", "Session"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.SessionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.SessionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.coordination.CreateNodeRequest,
      tech.ydb.coordination.CreateNodeResponse> METHOD_CREATE_NODE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Coordination.V1.CoordinationService", "CreateNode"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.CreateNodeRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.CreateNodeResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.coordination.AlterNodeRequest,
      tech.ydb.coordination.AlterNodeResponse> METHOD_ALTER_NODE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Coordination.V1.CoordinationService", "AlterNode"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.AlterNodeRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.AlterNodeResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.coordination.DropNodeRequest,
      tech.ydb.coordination.DropNodeResponse> METHOD_DROP_NODE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Coordination.V1.CoordinationService", "DropNode"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.DropNodeRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.DropNodeResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<tech.ydb.coordination.DescribeNodeRequest,
      tech.ydb.coordination.DescribeNodeResponse> METHOD_DESCRIBE_NODE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Coordination.V1.CoordinationService", "DescribeNode"),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.DescribeNodeRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(tech.ydb.coordination.DescribeNodeResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CoordinationServiceStub newStub(io.grpc.Channel channel) {
    return new CoordinationServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CoordinationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new CoordinationServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static CoordinationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new CoordinationServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class CoordinationServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     **
     * Bidirectional stream used to establish a session with a coordination node
     * Relevant APIs for managing semaphores, distributed locking, creating or
     * restoring a previously established session are described using nested
     * messages in SessionRequest and SessionResponse. Session is established
     * with a specific coordination node (previously created using CreateNode
     * below) and semaphores are local to that coordination node.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.coordination.SessionRequest> session(
        io.grpc.stub.StreamObserver<tech.ydb.coordination.SessionResponse> responseObserver) {
      return asyncUnimplementedStreamingCall(METHOD_SESSION, responseObserver);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public void createNode(tech.ydb.coordination.CreateNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.CreateNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_NODE, responseObserver);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public void alterNode(tech.ydb.coordination.AlterNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.AlterNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ALTER_NODE, responseObserver);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public void dropNode(tech.ydb.coordination.DropNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DropNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DROP_NODE, responseObserver);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public void describeNode(tech.ydb.coordination.DescribeNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DescribeNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_NODE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_SESSION,
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.coordination.SessionRequest,
                tech.ydb.coordination.SessionResponse>(
                  this, METHODID_SESSION)))
          .addMethod(
            METHOD_CREATE_NODE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.CreateNodeRequest,
                tech.ydb.coordination.CreateNodeResponse>(
                  this, METHODID_CREATE_NODE)))
          .addMethod(
            METHOD_ALTER_NODE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.AlterNodeRequest,
                tech.ydb.coordination.AlterNodeResponse>(
                  this, METHODID_ALTER_NODE)))
          .addMethod(
            METHOD_DROP_NODE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.DropNodeRequest,
                tech.ydb.coordination.DropNodeResponse>(
                  this, METHODID_DROP_NODE)))
          .addMethod(
            METHOD_DESCRIBE_NODE,
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.DescribeNodeRequest,
                tech.ydb.coordination.DescribeNodeResponse>(
                  this, METHODID_DESCRIBE_NODE)))
          .build();
    }
  }

  /**
   */
  public static final class CoordinationServiceStub extends io.grpc.stub.AbstractStub<CoordinationServiceStub> {
    private CoordinationServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CoordinationServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CoordinationServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     **
     * Bidirectional stream used to establish a session with a coordination node
     * Relevant APIs for managing semaphores, distributed locking, creating or
     * restoring a previously established session are described using nested
     * messages in SessionRequest and SessionResponse. Session is established
     * with a specific coordination node (previously created using CreateNode
     * below) and semaphores are local to that coordination node.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.coordination.SessionRequest> session(
        io.grpc.stub.StreamObserver<tech.ydb.coordination.SessionResponse> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(METHOD_SESSION, getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public void createNode(tech.ydb.coordination.CreateNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.CreateNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_NODE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public void alterNode(tech.ydb.coordination.AlterNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.AlterNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ALTER_NODE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public void dropNode(tech.ydb.coordination.DropNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DropNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DROP_NODE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public void describeNode(tech.ydb.coordination.DescribeNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DescribeNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_NODE, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CoordinationServiceBlockingStub extends io.grpc.stub.AbstractStub<CoordinationServiceBlockingStub> {
    private CoordinationServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CoordinationServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CoordinationServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public tech.ydb.coordination.CreateNodeResponse createNode(tech.ydb.coordination.CreateNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_NODE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public tech.ydb.coordination.AlterNodeResponse alterNode(tech.ydb.coordination.AlterNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ALTER_NODE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public tech.ydb.coordination.DropNodeResponse dropNode(tech.ydb.coordination.DropNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DROP_NODE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public tech.ydb.coordination.DescribeNodeResponse describeNode(tech.ydb.coordination.DescribeNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_NODE, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CoordinationServiceFutureStub extends io.grpc.stub.AbstractStub<CoordinationServiceFutureStub> {
    private CoordinationServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private CoordinationServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new CoordinationServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.CreateNodeResponse> createNode(
        tech.ydb.coordination.CreateNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_NODE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.AlterNodeResponse> alterNode(
        tech.ydb.coordination.AlterNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ALTER_NODE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.DropNodeResponse> dropNode(
        tech.ydb.coordination.DropNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DROP_NODE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.DescribeNodeResponse> describeNode(
        tech.ydb.coordination.DescribeNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_NODE, getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_NODE = 0;
  private static final int METHODID_ALTER_NODE = 1;
  private static final int METHODID_DROP_NODE = 2;
  private static final int METHODID_DESCRIBE_NODE = 3;
  private static final int METHODID_SESSION = 4;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final CoordinationServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(CoordinationServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_NODE:
          serviceImpl.createNode((tech.ydb.coordination.CreateNodeRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.coordination.CreateNodeResponse>) responseObserver);
          break;
        case METHODID_ALTER_NODE:
          serviceImpl.alterNode((tech.ydb.coordination.AlterNodeRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.coordination.AlterNodeResponse>) responseObserver);
          break;
        case METHODID_DROP_NODE:
          serviceImpl.dropNode((tech.ydb.coordination.DropNodeRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.coordination.DropNodeResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_NODE:
          serviceImpl.describeNode((tech.ydb.coordination.DescribeNodeRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.coordination.DescribeNodeResponse>) responseObserver);
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
        case METHODID_SESSION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.session(
              (io.grpc.stub.StreamObserver<tech.ydb.coordination.SessionResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static final class CoordinationServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.coordination.v1.CoordinationGrpc.getDescriptor();
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (CoordinationServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CoordinationServiceDescriptorSupplier())
              .addMethod(METHOD_SESSION)
              .addMethod(METHOD_CREATE_NODE)
              .addMethod(METHOD_ALTER_NODE)
              .addMethod(METHOD_DROP_NODE)
              .addMethod(METHOD_DESCRIBE_NODE)
              .build();
        }
      }
    }
    return result;
  }
}
