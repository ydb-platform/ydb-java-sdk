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
    comments = "Source: ydb/public/api/grpc/draft/ydb_persqueue_v1.proto")
public final class PersQueueServiceGrpc {

  private PersQueueServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.PersQueue.V1.PersQueueService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamingWrite",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> getStreamingWriteMethod;
    if ((getStreamingWriteMethod = PersQueueServiceGrpc.getStreamingWriteMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getStreamingWriteMethod = PersQueueServiceGrpc.getStreamingWriteMethod) == null) {
          PersQueueServiceGrpc.getStreamingWriteMethod = getStreamingWriteMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage, tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamingWrite"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("StreamingWrite"))
              .build();
        }
      }
    }
    return getStreamingWriteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage> getMigrationStreamingReadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MigrationStreamingRead",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage,
      tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage> getMigrationStreamingReadMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage, tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage> getMigrationStreamingReadMethod;
    if ((getMigrationStreamingReadMethod = PersQueueServiceGrpc.getMigrationStreamingReadMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getMigrationStreamingReadMethod = PersQueueServiceGrpc.getMigrationStreamingReadMethod) == null) {
          PersQueueServiceGrpc.getMigrationStreamingReadMethod = getMigrationStreamingReadMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage, tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MigrationStreamingRead"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("MigrationStreamingRead"))
              .build();
        }
      }
    }
    return getMigrationStreamingReadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
      tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetReadSessionsInfo",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
      tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest, tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getGetReadSessionsInfoMethod;
    if ((getGetReadSessionsInfoMethod = PersQueueServiceGrpc.getGetReadSessionsInfoMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getGetReadSessionsInfoMethod = PersQueueServiceGrpc.getGetReadSessionsInfoMethod) == null) {
          PersQueueServiceGrpc.getGetReadSessionsInfoMethod = getGetReadSessionsInfoMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest, tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetReadSessionsInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("GetReadSessionsInfo"))
              .build();
        }
      }
    }
    return getGetReadSessionsInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> getDescribeTopicMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeTopic",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> getDescribeTopicMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> getDescribeTopicMethod;
    if ((getDescribeTopicMethod = PersQueueServiceGrpc.getDescribeTopicMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getDescribeTopicMethod = PersQueueServiceGrpc.getDescribeTopicMethod) == null) {
          PersQueueServiceGrpc.getDescribeTopicMethod = getDescribeTopicMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeTopic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("DescribeTopic"))
              .build();
        }
      }
    }
    return getDescribeTopicMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> getDropTopicMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropTopic",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> getDropTopicMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> getDropTopicMethod;
    if ((getDropTopicMethod = PersQueueServiceGrpc.getDropTopicMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getDropTopicMethod = PersQueueServiceGrpc.getDropTopicMethod) == null) {
          PersQueueServiceGrpc.getDropTopicMethod = getDropTopicMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropTopic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("DropTopic"))
              .build();
        }
      }
    }
    return getDropTopicMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> getCreateTopicMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateTopic",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> getCreateTopicMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> getCreateTopicMethod;
    if ((getCreateTopicMethod = PersQueueServiceGrpc.getCreateTopicMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getCreateTopicMethod = PersQueueServiceGrpc.getCreateTopicMethod) == null) {
          PersQueueServiceGrpc.getCreateTopicMethod = getCreateTopicMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateTopic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("CreateTopic"))
              .build();
        }
      }
    }
    return getCreateTopicMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> getAlterTopicMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterTopic",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest,
      tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> getAlterTopicMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> getAlterTopicMethod;
    if ((getAlterTopicMethod = PersQueueServiceGrpc.getAlterTopicMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getAlterTopicMethod = PersQueueServiceGrpc.getAlterTopicMethod) == null) {
          PersQueueServiceGrpc.getAlterTopicMethod = getAlterTopicMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest, tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterTopic"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("AlterTopic"))
              .build();
        }
      }
    }
    return getAlterTopicMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest,
      tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> getAddReadRuleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddReadRule",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest,
      tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> getAddReadRuleMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest, tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> getAddReadRuleMethod;
    if ((getAddReadRuleMethod = PersQueueServiceGrpc.getAddReadRuleMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getAddReadRuleMethod = PersQueueServiceGrpc.getAddReadRuleMethod) == null) {
          PersQueueServiceGrpc.getAddReadRuleMethod = getAddReadRuleMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest, tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddReadRule"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("AddReadRule"))
              .build();
        }
      }
    }
    return getAddReadRuleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest,
      tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> getRemoveReadRuleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveReadRule",
      requestType = tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest.class,
      responseType = tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest,
      tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> getRemoveReadRuleMethod() {
    io.grpc.MethodDescriptor<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest, tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> getRemoveReadRuleMethod;
    if ((getRemoveReadRuleMethod = PersQueueServiceGrpc.getRemoveReadRuleMethod) == null) {
      synchronized (PersQueueServiceGrpc.class) {
        if ((getRemoveReadRuleMethod = PersQueueServiceGrpc.getRemoveReadRuleMethod) == null) {
          PersQueueServiceGrpc.getRemoveReadRuleMethod = getRemoveReadRuleMethod =
              io.grpc.MethodDescriptor.<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest, tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveReadRule"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse.getDefaultInstance()))
              .setSchemaDescriptor(new PersQueueServiceMethodDescriptorSupplier("RemoveReadRule"))
              .build();
        }
      }
    }
    return getRemoveReadRuleMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PersQueueServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceStub>() {
        @java.lang.Override
        public PersQueueServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceStub(channel, callOptions);
        }
      };
    return PersQueueServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PersQueueServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceBlockingStub>() {
        @java.lang.Override
        public PersQueueServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceBlockingStub(channel, callOptions);
        }
      };
    return PersQueueServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PersQueueServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<PersQueueServiceFutureStub>() {
        @java.lang.Override
        public PersQueueServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new PersQueueServiceFutureStub(channel, callOptions);
        }
      };
    return PersQueueServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class PersQueueServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage> streamingWrite(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> responseObserver) {
      return asyncUnimplementedStreamingCall(getStreamingWriteMethod(), responseObserver);
    }

    /**
     * <pre>
     **
     * Creates Read Session
     * Pipeline:
     * client                  server
     *         Init(Topics, ClientId, ...)
     *        ----------------&gt;
     *         Init(SessionId)
     *        &lt;----------------
     *         read1
     *        ----------------&gt;
     *         read2
     *        ----------------&gt;
     *         assign(Topic1, Cluster, Partition1, ...) - assigns and releases are optional
     *        &lt;----------------
     *         assign(Topic2, Clutster, Partition2, ...)
     *        &lt;----------------
     *         start_read(Topic1, Partition1, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         release(Topic1, Partition1, ...)
     *        &lt;----------------
     *         released(Topic1, Partition1, ...) - only after released server will give this parittion to other session.
     *        ----------------&gt;
     *         start_read(Topic2, Partition2, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         read data(data, ...)
     *        &lt;----------------
     *         commit(cookie1)
     *        ----------------&gt;
     *         committed(cookie1)
     *        &lt;----------------
     *         issue(description, ...)
     *        &lt;----------------
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage> migrationStreamingRead(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage> responseObserver) {
      return asyncUnimplementedStreamingCall(getMigrationStreamingReadMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetReadSessionsInfoMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe topic command.
     * </pre>
     */
    public void describeTopic(tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeTopicMethod(), responseObserver);
    }

    /**
     * <pre>
     * Drop topic command.
     * </pre>
     */
    public void dropTopic(tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropTopicMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create topic command.
     * </pre>
     */
    public void createTopic(tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTopicMethod(), responseObserver);
    }

    /**
     * <pre>
     * Alter topic command.
     * </pre>
     */
    public void alterTopic(tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterTopicMethod(), responseObserver);
    }

    /**
     * <pre>
     * Add read rule command.
     * </pre>
     */
    public void addReadRule(tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAddReadRuleMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove read rule command.
     * </pre>
     */
    public void removeReadRule(tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveReadRuleMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getStreamingWriteMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage,
                tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>(
                  this, METHODID_STREAMING_WRITE)))
          .addMethod(
            getMigrationStreamingReadMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage,
                tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage>(
                  this, METHODID_MIGRATION_STREAMING_READ)))
          .addMethod(
            getGetReadSessionsInfoMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest,
                tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>(
                  this, METHODID_GET_READ_SESSIONS_INFO)))
          .addMethod(
            getDescribeTopicMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest,
                tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse>(
                  this, METHODID_DESCRIBE_TOPIC)))
          .addMethod(
            getDropTopicMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest,
                tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse>(
                  this, METHODID_DROP_TOPIC)))
          .addMethod(
            getCreateTopicMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest,
                tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse>(
                  this, METHODID_CREATE_TOPIC)))
          .addMethod(
            getAlterTopicMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest,
                tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse>(
                  this, METHODID_ALTER_TOPIC)))
          .addMethod(
            getAddReadRuleMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest,
                tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse>(
                  this, METHODID_ADD_READ_RULE)))
          .addMethod(
            getRemoveReadRuleMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest,
                tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse>(
                  this, METHODID_REMOVE_READ_RULE)))
          .build();
    }
  }

  /**
   */
  public static final class PersQueueServiceStub extends io.grpc.stub.AbstractAsyncStub<PersQueueServiceStub> {
    private PersQueueServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceStub(channel, callOptions);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteClientMessage> streamingWrite(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getStreamingWriteMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     **
     * Creates Read Session
     * Pipeline:
     * client                  server
     *         Init(Topics, ClientId, ...)
     *        ----------------&gt;
     *         Init(SessionId)
     *        &lt;----------------
     *         read1
     *        ----------------&gt;
     *         read2
     *        ----------------&gt;
     *         assign(Topic1, Cluster, Partition1, ...) - assigns and releases are optional
     *        &lt;----------------
     *         assign(Topic2, Clutster, Partition2, ...)
     *        &lt;----------------
     *         start_read(Topic1, Partition1, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         release(Topic1, Partition1, ...)
     *        &lt;----------------
     *         released(Topic1, Partition1, ...) - only after released server will give this parittion to other session.
     *        ----------------&gt;
     *         start_read(Topic2, Partition2, ...) - client must respond to assign request with this message. Only after this client will start recieving messages from this partition
     *        ----------------&gt;
     *         read data(data, ...)
     *        &lt;----------------
     *         commit(cookie1)
     *        ----------------&gt;
     *         committed(cookie1)
     *        &lt;----------------
     *         issue(description, ...)
     *        &lt;----------------
     * </pre>
     */
    public io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadClientMessage> migrationStreamingRead(
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getMigrationStreamingReadMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public void getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetReadSessionsInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe topic command.
     * </pre>
     */
    public void describeTopic(tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeTopicMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Drop topic command.
     * </pre>
     */
    public void dropTopic(tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropTopicMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create topic command.
     * </pre>
     */
    public void createTopic(tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTopicMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Alter topic command.
     * </pre>
     */
    public void alterTopic(tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterTopicMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Add read rule command.
     * </pre>
     */
    public void addReadRule(tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddReadRuleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Remove read rule command.
     * </pre>
     */
    public void removeReadRule(tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveReadRuleMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PersQueueServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<PersQueueServiceBlockingStub> {
    private PersQueueServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse getReadSessionsInfo(tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetReadSessionsInfoMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe topic command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse describeTopic(tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeTopicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Drop topic command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse dropTopic(tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropTopicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create topic command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse createTopic(tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateTopicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Alter topic command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse alterTopic(tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterTopicMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Add read rule command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse addReadRule(tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest request) {
      return blockingUnaryCall(
          getChannel(), getAddReadRuleMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove read rule command.
     * </pre>
     */
    public tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse removeReadRule(tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveReadRuleMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PersQueueServiceFutureStub extends io.grpc.stub.AbstractFutureStub<PersQueueServiceFutureStub> {
    private PersQueueServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PersQueueServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PersQueueServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Get information about reading
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse> getReadSessionsInfo(
        tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetReadSessionsInfoMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe topic command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse> describeTopic(
        tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeTopicMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Drop topic command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse> dropTopic(
        tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropTopicMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create topic command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse> createTopic(
        tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTopicMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Alter topic command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse> alterTopic(
        tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterTopicMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Add read rule command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse> addReadRule(
        tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAddReadRuleMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove read rule command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse> removeReadRule(
        tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveReadRuleMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_READ_SESSIONS_INFO = 0;
  private static final int METHODID_DESCRIBE_TOPIC = 1;
  private static final int METHODID_DROP_TOPIC = 2;
  private static final int METHODID_CREATE_TOPIC = 3;
  private static final int METHODID_ALTER_TOPIC = 4;
  private static final int METHODID_ADD_READ_RULE = 5;
  private static final int METHODID_REMOVE_READ_RULE = 6;
  private static final int METHODID_STREAMING_WRITE = 7;
  private static final int METHODID_MIGRATION_STREAMING_READ = 8;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PersQueueServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PersQueueServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_READ_SESSIONS_INFO:
          serviceImpl.getReadSessionsInfo((tech.ydb.persqueue.YdbPersqueueV1.ReadInfoRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.ReadInfoResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TOPIC:
          serviceImpl.describeTopic((tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DescribeTopicResponse>) responseObserver);
          break;
        case METHODID_DROP_TOPIC:
          serviceImpl.dropTopic((tech.ydb.persqueue.YdbPersqueueV1.DropTopicRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.DropTopicResponse>) responseObserver);
          break;
        case METHODID_CREATE_TOPIC:
          serviceImpl.createTopic((tech.ydb.persqueue.YdbPersqueueV1.CreateTopicRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.CreateTopicResponse>) responseObserver);
          break;
        case METHODID_ALTER_TOPIC:
          serviceImpl.alterTopic((tech.ydb.persqueue.YdbPersqueueV1.AlterTopicRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AlterTopicResponse>) responseObserver);
          break;
        case METHODID_ADD_READ_RULE:
          serviceImpl.addReadRule((tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.AddReadRuleResponse>) responseObserver);
          break;
        case METHODID_REMOVE_READ_RULE:
          serviceImpl.removeReadRule((tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.RemoveReadRuleResponse>) responseObserver);
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
        case METHODID_STREAMING_WRITE:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.streamingWrite(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.StreamingWriteServerMessage>) responseObserver);
        case METHODID_MIGRATION_STREAMING_READ:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.migrationStreamingRead(
              (io.grpc.stub.StreamObserver<tech.ydb.persqueue.YdbPersqueueV1.MigrationStreamingReadServerMessage>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PersQueueServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PersQueueServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.persqueue.v1.YdbPersqueueV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PersQueueService");
    }
  }

  private static final class PersQueueServiceFileDescriptorSupplier
      extends PersQueueServiceBaseDescriptorSupplier {
    PersQueueServiceFileDescriptorSupplier() {}
  }

  private static final class PersQueueServiceMethodDescriptorSupplier
      extends PersQueueServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PersQueueServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PersQueueServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PersQueueServiceFileDescriptorSupplier())
              .addMethod(getStreamingWriteMethod())
              .addMethod(getMigrationStreamingReadMethod())
              .addMethod(getGetReadSessionsInfoMethod())
              .addMethod(getDescribeTopicMethod())
              .addMethod(getDropTopicMethod())
              .addMethod(getCreateTopicMethod())
              .addMethod(getAlterTopicMethod())
              .addMethod(getAddReadRuleMethod())
              .addMethod(getRemoveReadRuleMethod())
              .build();
        }
      }
    }
    return result;
  }
}
