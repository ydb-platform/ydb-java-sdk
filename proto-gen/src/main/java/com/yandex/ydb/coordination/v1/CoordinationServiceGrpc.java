package tech.ydb.coordination.v1;

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
    comments = "Source: ydb/public/api/grpc/ydb_coordination_v1.proto")
public final class CoordinationServiceGrpc {

  private CoordinationServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Coordination.V1.CoordinationService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.coordination.SessionRequest,
      tech.ydb.coordination.SessionResponse> getSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Session",
      requestType = tech.ydb.coordination.SessionRequest.class,
      responseType = tech.ydb.coordination.SessionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.coordination.SessionRequest,
      tech.ydb.coordination.SessionResponse> getSessionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.coordination.SessionRequest, tech.ydb.coordination.SessionResponse> getSessionMethod;
    if ((getSessionMethod = CoordinationServiceGrpc.getSessionMethod) == null) {
      synchronized (CoordinationServiceGrpc.class) {
        if ((getSessionMethod = CoordinationServiceGrpc.getSessionMethod) == null) {
          CoordinationServiceGrpc.getSessionMethod = getSessionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.coordination.SessionRequest, tech.ydb.coordination.SessionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Session"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.SessionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.SessionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CoordinationServiceMethodDescriptorSupplier("Session"))
              .build();
        }
      }
    }
    return getSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.coordination.CreateNodeRequest,
      tech.ydb.coordination.CreateNodeResponse> getCreateNodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateNode",
      requestType = tech.ydb.coordination.CreateNodeRequest.class,
      responseType = tech.ydb.coordination.CreateNodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.coordination.CreateNodeRequest,
      tech.ydb.coordination.CreateNodeResponse> getCreateNodeMethod() {
    io.grpc.MethodDescriptor<tech.ydb.coordination.CreateNodeRequest, tech.ydb.coordination.CreateNodeResponse> getCreateNodeMethod;
    if ((getCreateNodeMethod = CoordinationServiceGrpc.getCreateNodeMethod) == null) {
      synchronized (CoordinationServiceGrpc.class) {
        if ((getCreateNodeMethod = CoordinationServiceGrpc.getCreateNodeMethod) == null) {
          CoordinationServiceGrpc.getCreateNodeMethod = getCreateNodeMethod =
              io.grpc.MethodDescriptor.<tech.ydb.coordination.CreateNodeRequest, tech.ydb.coordination.CreateNodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateNode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.CreateNodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.CreateNodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CoordinationServiceMethodDescriptorSupplier("CreateNode"))
              .build();
        }
      }
    }
    return getCreateNodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.coordination.AlterNodeRequest,
      tech.ydb.coordination.AlterNodeResponse> getAlterNodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterNode",
      requestType = tech.ydb.coordination.AlterNodeRequest.class,
      responseType = tech.ydb.coordination.AlterNodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.coordination.AlterNodeRequest,
      tech.ydb.coordination.AlterNodeResponse> getAlterNodeMethod() {
    io.grpc.MethodDescriptor<tech.ydb.coordination.AlterNodeRequest, tech.ydb.coordination.AlterNodeResponse> getAlterNodeMethod;
    if ((getAlterNodeMethod = CoordinationServiceGrpc.getAlterNodeMethod) == null) {
      synchronized (CoordinationServiceGrpc.class) {
        if ((getAlterNodeMethod = CoordinationServiceGrpc.getAlterNodeMethod) == null) {
          CoordinationServiceGrpc.getAlterNodeMethod = getAlterNodeMethod =
              io.grpc.MethodDescriptor.<tech.ydb.coordination.AlterNodeRequest, tech.ydb.coordination.AlterNodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterNode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.AlterNodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.AlterNodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CoordinationServiceMethodDescriptorSupplier("AlterNode"))
              .build();
        }
      }
    }
    return getAlterNodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.coordination.DropNodeRequest,
      tech.ydb.coordination.DropNodeResponse> getDropNodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropNode",
      requestType = tech.ydb.coordination.DropNodeRequest.class,
      responseType = tech.ydb.coordination.DropNodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.coordination.DropNodeRequest,
      tech.ydb.coordination.DropNodeResponse> getDropNodeMethod() {
    io.grpc.MethodDescriptor<tech.ydb.coordination.DropNodeRequest, tech.ydb.coordination.DropNodeResponse> getDropNodeMethod;
    if ((getDropNodeMethod = CoordinationServiceGrpc.getDropNodeMethod) == null) {
      synchronized (CoordinationServiceGrpc.class) {
        if ((getDropNodeMethod = CoordinationServiceGrpc.getDropNodeMethod) == null) {
          CoordinationServiceGrpc.getDropNodeMethod = getDropNodeMethod =
              io.grpc.MethodDescriptor.<tech.ydb.coordination.DropNodeRequest, tech.ydb.coordination.DropNodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropNode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.DropNodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.DropNodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CoordinationServiceMethodDescriptorSupplier("DropNode"))
              .build();
        }
      }
    }
    return getDropNodeMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.coordination.DescribeNodeRequest,
      tech.ydb.coordination.DescribeNodeResponse> getDescribeNodeMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeNode",
      requestType = tech.ydb.coordination.DescribeNodeRequest.class,
      responseType = tech.ydb.coordination.DescribeNodeResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.coordination.DescribeNodeRequest,
      tech.ydb.coordination.DescribeNodeResponse> getDescribeNodeMethod() {
    io.grpc.MethodDescriptor<tech.ydb.coordination.DescribeNodeRequest, tech.ydb.coordination.DescribeNodeResponse> getDescribeNodeMethod;
    if ((getDescribeNodeMethod = CoordinationServiceGrpc.getDescribeNodeMethod) == null) {
      synchronized (CoordinationServiceGrpc.class) {
        if ((getDescribeNodeMethod = CoordinationServiceGrpc.getDescribeNodeMethod) == null) {
          CoordinationServiceGrpc.getDescribeNodeMethod = getDescribeNodeMethod =
              io.grpc.MethodDescriptor.<tech.ydb.coordination.DescribeNodeRequest, tech.ydb.coordination.DescribeNodeResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeNode"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.DescribeNodeRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.coordination.DescribeNodeResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CoordinationServiceMethodDescriptorSupplier("DescribeNode"))
              .build();
        }
      }
    }
    return getDescribeNodeMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CoordinationServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceStub>() {
        @java.lang.Override
        public CoordinationServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CoordinationServiceStub(channel, callOptions);
        }
      };
    return CoordinationServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CoordinationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceBlockingStub>() {
        @java.lang.Override
        public CoordinationServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CoordinationServiceBlockingStub(channel, callOptions);
        }
      };
    return CoordinationServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CoordinationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CoordinationServiceFutureStub>() {
        @java.lang.Override
        public CoordinationServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CoordinationServiceFutureStub(channel, callOptions);
        }
      };
    return CoordinationServiceFutureStub.newStub(factory, channel);
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
      return asyncUnimplementedStreamingCall(getSessionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public void createNode(tech.ydb.coordination.CreateNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.CreateNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateNodeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public void alterNode(tech.ydb.coordination.AlterNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.AlterNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterNodeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public void dropNode(tech.ydb.coordination.DropNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DropNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropNodeMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public void describeNode(tech.ydb.coordination.DescribeNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DescribeNodeResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeNodeMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSessionMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.coordination.SessionRequest,
                tech.ydb.coordination.SessionResponse>(
                  this, METHODID_SESSION)))
          .addMethod(
            getCreateNodeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.CreateNodeRequest,
                tech.ydb.coordination.CreateNodeResponse>(
                  this, METHODID_CREATE_NODE)))
          .addMethod(
            getAlterNodeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.AlterNodeRequest,
                tech.ydb.coordination.AlterNodeResponse>(
                  this, METHODID_ALTER_NODE)))
          .addMethod(
            getDropNodeMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.coordination.DropNodeRequest,
                tech.ydb.coordination.DropNodeResponse>(
                  this, METHODID_DROP_NODE)))
          .addMethod(
            getDescribeNodeMethod(),
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
  public static final class CoordinationServiceStub extends io.grpc.stub.AbstractAsyncStub<CoordinationServiceStub> {
    private CoordinationServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getSessionMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public void createNode(tech.ydb.coordination.CreateNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.CreateNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateNodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public void alterNode(tech.ydb.coordination.AlterNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.AlterNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterNodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public void dropNode(tech.ydb.coordination.DropNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DropNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropNodeMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public void describeNode(tech.ydb.coordination.DescribeNodeRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.coordination.DescribeNodeResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeNodeMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class CoordinationServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<CoordinationServiceBlockingStub> {
    private CoordinationServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CoordinationServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Creates a new coordination node
     * </pre>
     */
    public tech.ydb.coordination.CreateNodeResponse createNode(tech.ydb.coordination.CreateNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateNodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public tech.ydb.coordination.AlterNodeResponse alterNode(tech.ydb.coordination.AlterNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterNodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public tech.ydb.coordination.DropNodeResponse dropNode(tech.ydb.coordination.DropNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropNodeMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public tech.ydb.coordination.DescribeNodeResponse describeNode(tech.ydb.coordination.DescribeNodeRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeNodeMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class CoordinationServiceFutureStub extends io.grpc.stub.AbstractFutureStub<CoordinationServiceFutureStub> {
    private CoordinationServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CoordinationServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
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
          getChannel().newCall(getCreateNodeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Modifies settings of a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.AlterNodeResponse> alterNode(
        tech.ydb.coordination.AlterNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterNodeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Drops a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.DropNodeResponse> dropNode(
        tech.ydb.coordination.DropNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropNodeMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describes a coordination node
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.coordination.DescribeNodeResponse> describeNode(
        tech.ydb.coordination.DescribeNodeRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeNodeMethod(), getCallOptions()), request);
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

  private static abstract class CoordinationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CoordinationServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.coordination.v1.CoordinationGrpc.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CoordinationService");
    }
  }

  private static final class CoordinationServiceFileDescriptorSupplier
      extends CoordinationServiceBaseDescriptorSupplier {
    CoordinationServiceFileDescriptorSupplier() {}
  }

  private static final class CoordinationServiceMethodDescriptorSupplier
      extends CoordinationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    CoordinationServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (CoordinationServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CoordinationServiceFileDescriptorSupplier())
              .addMethod(getSessionMethod())
              .addMethod(getCreateNodeMethod())
              .addMethod(getAlterNodeMethod())
              .addMethod(getDropNodeMethod())
              .addMethod(getDescribeNodeMethod())
              .build();
        }
      }
    }
    return result;
  }
}
