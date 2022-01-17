package tech.ydb.logstore.v1;

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
    comments = "Source: ydb/public/api/grpc/draft/ydb_logstore_v1.proto")
public final class LogStoreServiceGrpc {

  private LogStoreServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.LogStore.V1.LogStoreService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> getCreateLogStoreMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateLogStore",
      requestType = tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> getCreateLogStoreMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest, tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> getCreateLogStoreMethod;
    if ((getCreateLogStoreMethod = LogStoreServiceGrpc.getCreateLogStoreMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getCreateLogStoreMethod = LogStoreServiceGrpc.getCreateLogStoreMethod) == null) {
          LogStoreServiceGrpc.getCreateLogStoreMethod = getCreateLogStoreMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest, tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateLogStore"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("CreateLogStore"))
              .build();
        }
      }
    }
    return getCreateLogStoreMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> getDescribeLogStoreMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeLogStore",
      requestType = tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> getDescribeLogStoreMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest, tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> getDescribeLogStoreMethod;
    if ((getDescribeLogStoreMethod = LogStoreServiceGrpc.getDescribeLogStoreMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getDescribeLogStoreMethod = LogStoreServiceGrpc.getDescribeLogStoreMethod) == null) {
          LogStoreServiceGrpc.getDescribeLogStoreMethod = getDescribeLogStoreMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest, tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeLogStore"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("DescribeLogStore"))
              .build();
        }
      }
    }
    return getDescribeLogStoreMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> getDropLogStoreMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropLogStore",
      requestType = tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest,
      tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> getDropLogStoreMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest, tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> getDropLogStoreMethod;
    if ((getDropLogStoreMethod = LogStoreServiceGrpc.getDropLogStoreMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getDropLogStoreMethod = LogStoreServiceGrpc.getDropLogStoreMethod) == null) {
          LogStoreServiceGrpc.getDropLogStoreMethod = getDropLogStoreMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest, tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropLogStore"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("DropLogStore"))
              .build();
        }
      }
    }
    return getDropLogStoreMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> getCreateLogTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateLogTable",
      requestType = tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> getCreateLogTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest, tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> getCreateLogTableMethod;
    if ((getCreateLogTableMethod = LogStoreServiceGrpc.getCreateLogTableMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getCreateLogTableMethod = LogStoreServiceGrpc.getCreateLogTableMethod) == null) {
          LogStoreServiceGrpc.getCreateLogTableMethod = getCreateLogTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest, tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateLogTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("CreateLogTable"))
              .build();
        }
      }
    }
    return getCreateLogTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> getDescribeLogTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeLogTable",
      requestType = tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> getDescribeLogTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest, tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> getDescribeLogTableMethod;
    if ((getDescribeLogTableMethod = LogStoreServiceGrpc.getDescribeLogTableMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getDescribeLogTableMethod = LogStoreServiceGrpc.getDescribeLogTableMethod) == null) {
          LogStoreServiceGrpc.getDescribeLogTableMethod = getDescribeLogTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest, tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeLogTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("DescribeLogTable"))
              .build();
        }
      }
    }
    return getDescribeLogTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> getDropLogTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropLogTable",
      requestType = tech.ydb.logstore.LogStoreProtos.DropLogTableRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.DropLogTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> getDropLogTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.DropLogTableRequest, tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> getDropLogTableMethod;
    if ((getDropLogTableMethod = LogStoreServiceGrpc.getDropLogTableMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getDropLogTableMethod = LogStoreServiceGrpc.getDropLogTableMethod) == null) {
          LogStoreServiceGrpc.getDropLogTableMethod = getDropLogTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.DropLogTableRequest, tech.ydb.logstore.LogStoreProtos.DropLogTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropLogTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DropLogTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.DropLogTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("DropLogTable"))
              .build();
        }
      }
    }
    return getDropLogTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> getAlterLogTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterLogTable",
      requestType = tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest.class,
      responseType = tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest,
      tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> getAlterLogTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest, tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> getAlterLogTableMethod;
    if ((getAlterLogTableMethod = LogStoreServiceGrpc.getAlterLogTableMethod) == null) {
      synchronized (LogStoreServiceGrpc.class) {
        if ((getAlterLogTableMethod = LogStoreServiceGrpc.getAlterLogTableMethod) == null) {
          LogStoreServiceGrpc.getAlterLogTableMethod = getAlterLogTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest, tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterLogTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new LogStoreServiceMethodDescriptorSupplier("AlterLogTable"))
              .build();
        }
      }
    }
    return getAlterLogTableMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LogStoreServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceStub>() {
        @java.lang.Override
        public LogStoreServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogStoreServiceStub(channel, callOptions);
        }
      };
    return LogStoreServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LogStoreServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceBlockingStub>() {
        @java.lang.Override
        public LogStoreServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogStoreServiceBlockingStub(channel, callOptions);
        }
      };
    return LogStoreServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LogStoreServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogStoreServiceFutureStub>() {
        @java.lang.Override
        public LogStoreServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogStoreServiceFutureStub(channel, callOptions);
        }
      };
    return LogStoreServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class LogStoreServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void createLogStore(tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateLogStoreMethod(), responseObserver);
    }

    /**
     */
    public void describeLogStore(tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeLogStoreMethod(), responseObserver);
    }

    /**
     */
    public void dropLogStore(tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropLogStoreMethod(), responseObserver);
    }

    /**
     */
    public void createLogTable(tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateLogTableMethod(), responseObserver);
    }

    /**
     */
    public void describeLogTable(tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeLogTableMethod(), responseObserver);
    }

    /**
     */
    public void dropLogTable(tech.ydb.logstore.LogStoreProtos.DropLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropLogTableMethod(), responseObserver);
    }

    /**
     */
    public void alterLogTable(tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterLogTableMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateLogStoreMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest,
                tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse>(
                  this, METHODID_CREATE_LOG_STORE)))
          .addMethod(
            getDescribeLogStoreMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest,
                tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse>(
                  this, METHODID_DESCRIBE_LOG_STORE)))
          .addMethod(
            getDropLogStoreMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest,
                tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse>(
                  this, METHODID_DROP_LOG_STORE)))
          .addMethod(
            getCreateLogTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest,
                tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse>(
                  this, METHODID_CREATE_LOG_TABLE)))
          .addMethod(
            getDescribeLogTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest,
                tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse>(
                  this, METHODID_DESCRIBE_LOG_TABLE)))
          .addMethod(
            getDropLogTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.DropLogTableRequest,
                tech.ydb.logstore.LogStoreProtos.DropLogTableResponse>(
                  this, METHODID_DROP_LOG_TABLE)))
          .addMethod(
            getAlterLogTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest,
                tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse>(
                  this, METHODID_ALTER_LOG_TABLE)))
          .build();
    }
  }

  /**
   */
  public static final class LogStoreServiceStub extends io.grpc.stub.AbstractAsyncStub<LogStoreServiceStub> {
    private LogStoreServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogStoreServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogStoreServiceStub(channel, callOptions);
    }

    /**
     */
    public void createLogStore(tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateLogStoreMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeLogStore(tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeLogStoreMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dropLogStore(tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropLogStoreMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createLogTable(tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateLogTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeLogTable(tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeLogTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void dropLogTable(tech.ydb.logstore.LogStoreProtos.DropLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropLogTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void alterLogTable(tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterLogTableMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class LogStoreServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<LogStoreServiceBlockingStub> {
    private LogStoreServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogStoreServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogStoreServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse createLogStore(tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateLogStoreMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse describeLogStore(tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeLogStoreMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse dropLogStore(tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropLogStoreMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse createLogTable(tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateLogTableMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse describeLogTable(tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeLogTableMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.DropLogTableResponse dropLogTable(tech.ydb.logstore.LogStoreProtos.DropLogTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropLogTableMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse alterLogTable(tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterLogTableMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class LogStoreServiceFutureStub extends io.grpc.stub.AbstractFutureStub<LogStoreServiceFutureStub> {
    private LogStoreServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogStoreServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogStoreServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse> createLogStore(
        tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateLogStoreMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse> describeLogStore(
        tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeLogStoreMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse> dropLogStore(
        tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropLogStoreMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse> createLogTable(
        tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateLogTableMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse> describeLogTable(
        tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeLogTableMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.DropLogTableResponse> dropLogTable(
        tech.ydb.logstore.LogStoreProtos.DropLogTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropLogTableMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse> alterLogTable(
        tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterLogTableMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_LOG_STORE = 0;
  private static final int METHODID_DESCRIBE_LOG_STORE = 1;
  private static final int METHODID_DROP_LOG_STORE = 2;
  private static final int METHODID_CREATE_LOG_TABLE = 3;
  private static final int METHODID_DESCRIBE_LOG_TABLE = 4;
  private static final int METHODID_DROP_LOG_TABLE = 5;
  private static final int METHODID_ALTER_LOG_TABLE = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final LogStoreServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(LogStoreServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_LOG_STORE:
          serviceImpl.createLogStore((tech.ydb.logstore.LogStoreProtos.CreateLogStoreRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogStoreResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_LOG_STORE:
          serviceImpl.describeLogStore((tech.ydb.logstore.LogStoreProtos.DescribeLogStoreRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogStoreResponse>) responseObserver);
          break;
        case METHODID_DROP_LOG_STORE:
          serviceImpl.dropLogStore((tech.ydb.logstore.LogStoreProtos.DropLogStoreRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogStoreResponse>) responseObserver);
          break;
        case METHODID_CREATE_LOG_TABLE:
          serviceImpl.createLogTable((tech.ydb.logstore.LogStoreProtos.CreateLogTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.CreateLogTableResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_LOG_TABLE:
          serviceImpl.describeLogTable((tech.ydb.logstore.LogStoreProtos.DescribeLogTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DescribeLogTableResponse>) responseObserver);
          break;
        case METHODID_DROP_LOG_TABLE:
          serviceImpl.dropLogTable((tech.ydb.logstore.LogStoreProtos.DropLogTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.DropLogTableResponse>) responseObserver);
          break;
        case METHODID_ALTER_LOG_TABLE:
          serviceImpl.alterLogTable((tech.ydb.logstore.LogStoreProtos.AlterLogTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.logstore.LogStoreProtos.AlterLogTableResponse>) responseObserver);
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

  private static abstract class LogStoreServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LogStoreServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.logstore.v1.YdbLogstoreV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LogStoreService");
    }
  }

  private static final class LogStoreServiceFileDescriptorSupplier
      extends LogStoreServiceBaseDescriptorSupplier {
    LogStoreServiceFileDescriptorSupplier() {}
  }

  private static final class LogStoreServiceMethodDescriptorSupplier
      extends LogStoreServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    LogStoreServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (LogStoreServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LogStoreServiceFileDescriptorSupplier())
              .addMethod(getCreateLogStoreMethod())
              .addMethod(getDescribeLogStoreMethod())
              .addMethod(getDropLogStoreMethod())
              .addMethod(getCreateLogTableMethod())
              .addMethod(getDescribeLogTableMethod())
              .addMethod(getDropLogTableMethod())
              .addMethod(getAlterLogTableMethod())
              .build();
        }
      }
    }
    return result;
  }
}
