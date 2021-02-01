package tech.ydb.datastreams.v1;

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
    comments = "Source: kikimr/public/api/grpc/draft/ydb_datastreams_v1.proto")
public final class DataStreamsServiceGrpc {

  private DataStreamsServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.DataStreams.V1.DataStreamsService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> getCreateStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> getCreateStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest, tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> getCreateStreamMethod;
    if ((getCreateStreamMethod = DataStreamsServiceGrpc.getCreateStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getCreateStreamMethod = DataStreamsServiceGrpc.getCreateStreamMethod) == null) {
          DataStreamsServiceGrpc.getCreateStreamMethod = getCreateStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest, tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("CreateStream"))
              .build();
        }
      }
    }
    return getCreateStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest,
      tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> getListStreamsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListStreams",
      requestType = tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest,
      tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> getListStreamsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest, tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> getListStreamsMethod;
    if ((getListStreamsMethod = DataStreamsServiceGrpc.getListStreamsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getListStreamsMethod = DataStreamsServiceGrpc.getListStreamsMethod) == null) {
          DataStreamsServiceGrpc.getListStreamsMethod = getListStreamsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest, tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListStreams"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("ListStreams"))
              .build();
        }
      }
    }
    return getListStreamsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> getDeleteStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> getDeleteStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest, tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> getDeleteStreamMethod;
    if ((getDeleteStreamMethod = DataStreamsServiceGrpc.getDeleteStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDeleteStreamMethod = DataStreamsServiceGrpc.getDeleteStreamMethod) == null) {
          DataStreamsServiceGrpc.getDeleteStreamMethod = getDeleteStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest, tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DeleteStream"))
              .build();
        }
      }
    }
    return getDeleteStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> getDescribeStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> getDescribeStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> getDescribeStreamMethod;
    if ((getDescribeStreamMethod = DataStreamsServiceGrpc.getDescribeStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDescribeStreamMethod = DataStreamsServiceGrpc.getDescribeStreamMethod) == null) {
          DataStreamsServiceGrpc.getDescribeStreamMethod = getDescribeStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DescribeStream"))
              .build();
        }
      }
    }
    return getDescribeStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListShardsRequest,
      tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> getListShardsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListShards",
      requestType = tech.ydb.datastreams.v1.Datastreams.ListShardsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.ListShardsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListShardsRequest,
      tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> getListShardsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListShardsRequest, tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> getListShardsMethod;
    if ((getListShardsMethod = DataStreamsServiceGrpc.getListShardsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getListShardsMethod = DataStreamsServiceGrpc.getListShardsMethod) == null) {
          DataStreamsServiceGrpc.getListShardsMethod = getListShardsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.ListShardsRequest, tech.ydb.datastreams.v1.Datastreams.ListShardsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListShards"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListShardsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListShardsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("ListShards"))
              .build();
        }
      }
    }
    return getListShardsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordRequest,
      tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> getPutRecordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PutRecord",
      requestType = tech.ydb.datastreams.v1.Datastreams.PutRecordRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.PutRecordResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordRequest,
      tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> getPutRecordMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordRequest, tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> getPutRecordMethod;
    if ((getPutRecordMethod = DataStreamsServiceGrpc.getPutRecordMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getPutRecordMethod = DataStreamsServiceGrpc.getPutRecordMethod) == null) {
          DataStreamsServiceGrpc.getPutRecordMethod = getPutRecordMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.PutRecordRequest, tech.ydb.datastreams.v1.Datastreams.PutRecordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PutRecord"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.PutRecordRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.PutRecordResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("PutRecord"))
              .build();
        }
      }
    }
    return getPutRecordMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest,
      tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> getPutRecordsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PutRecords",
      requestType = tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest,
      tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> getPutRecordsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest, tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> getPutRecordsMethod;
    if ((getPutRecordsMethod = DataStreamsServiceGrpc.getPutRecordsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getPutRecordsMethod = DataStreamsServiceGrpc.getPutRecordsMethod) == null) {
          DataStreamsServiceGrpc.getPutRecordsMethod = getPutRecordsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest, tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PutRecords"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("PutRecords"))
              .build();
        }
      }
    }
    return getPutRecordsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest,
      tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> getGetRecordsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRecords",
      requestType = tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest,
      tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> getGetRecordsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest, tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> getGetRecordsMethod;
    if ((getGetRecordsMethod = DataStreamsServiceGrpc.getGetRecordsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getGetRecordsMethod = DataStreamsServiceGrpc.getGetRecordsMethod) == null) {
          DataStreamsServiceGrpc.getGetRecordsMethod = getGetRecordsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest, tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRecords"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("GetRecords"))
              .build();
        }
      }
    }
    return getGetRecordsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest,
      tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> getGetShardIteratorMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetShardIterator",
      requestType = tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest,
      tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> getGetShardIteratorMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest, tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> getGetShardIteratorMethod;
    if ((getGetShardIteratorMethod = DataStreamsServiceGrpc.getGetShardIteratorMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getGetShardIteratorMethod = DataStreamsServiceGrpc.getGetShardIteratorMethod) == null) {
          DataStreamsServiceGrpc.getGetShardIteratorMethod = getGetShardIteratorMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest, tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetShardIterator"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("GetShardIterator"))
              .build();
        }
      }
    }
    return getGetShardIteratorMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest,
      tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> getSubscribeToShardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SubscribeToShard",
      requestType = tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest,
      tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> getSubscribeToShardMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest, tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> getSubscribeToShardMethod;
    if ((getSubscribeToShardMethod = DataStreamsServiceGrpc.getSubscribeToShardMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getSubscribeToShardMethod = DataStreamsServiceGrpc.getSubscribeToShardMethod) == null) {
          DataStreamsServiceGrpc.getSubscribeToShardMethod = getSubscribeToShardMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest, tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubscribeToShard"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("SubscribeToShard"))
              .build();
        }
      }
    }
    return getSubscribeToShardMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> getDescribeLimitsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeLimits",
      requestType = tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> getDescribeLimitsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest, tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> getDescribeLimitsMethod;
    if ((getDescribeLimitsMethod = DataStreamsServiceGrpc.getDescribeLimitsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDescribeLimitsMethod = DataStreamsServiceGrpc.getDescribeLimitsMethod) == null) {
          DataStreamsServiceGrpc.getDescribeLimitsMethod = getDescribeLimitsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest, tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeLimits"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DescribeLimits"))
              .build();
        }
      }
    }
    return getDescribeLimitsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> getDescribeStreamSummaryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeStreamSummary",
      requestType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> getDescribeStreamSummaryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> getDescribeStreamSummaryMethod;
    if ((getDescribeStreamSummaryMethod = DataStreamsServiceGrpc.getDescribeStreamSummaryMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDescribeStreamSummaryMethod = DataStreamsServiceGrpc.getDescribeStreamSummaryMethod) == null) {
          DataStreamsServiceGrpc.getDescribeStreamSummaryMethod = getDescribeStreamSummaryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeStreamSummary"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DescribeStreamSummary"))
              .build();
        }
      }
    }
    return getDescribeStreamSummaryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest,
      tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> getDecreaseStreamRetentionPeriodMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DecreaseStreamRetentionPeriod",
      requestType = tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest,
      tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> getDecreaseStreamRetentionPeriodMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest, tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> getDecreaseStreamRetentionPeriodMethod;
    if ((getDecreaseStreamRetentionPeriodMethod = DataStreamsServiceGrpc.getDecreaseStreamRetentionPeriodMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDecreaseStreamRetentionPeriodMethod = DataStreamsServiceGrpc.getDecreaseStreamRetentionPeriodMethod) == null) {
          DataStreamsServiceGrpc.getDecreaseStreamRetentionPeriodMethod = getDecreaseStreamRetentionPeriodMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest, tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DecreaseStreamRetentionPeriod"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DecreaseStreamRetentionPeriod"))
              .build();
        }
      }
    }
    return getDecreaseStreamRetentionPeriodMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest,
      tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> getIncreaseStreamRetentionPeriodMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IncreaseStreamRetentionPeriod",
      requestType = tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest,
      tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> getIncreaseStreamRetentionPeriodMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest, tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> getIncreaseStreamRetentionPeriodMethod;
    if ((getIncreaseStreamRetentionPeriodMethod = DataStreamsServiceGrpc.getIncreaseStreamRetentionPeriodMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getIncreaseStreamRetentionPeriodMethod = DataStreamsServiceGrpc.getIncreaseStreamRetentionPeriodMethod) == null) {
          DataStreamsServiceGrpc.getIncreaseStreamRetentionPeriodMethod = getIncreaseStreamRetentionPeriodMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest, tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IncreaseStreamRetentionPeriod"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("IncreaseStreamRetentionPeriod"))
              .build();
        }
      }
    }
    return getIncreaseStreamRetentionPeriodMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest,
      tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> getUpdateShardCountMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateShardCount",
      requestType = tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest,
      tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> getUpdateShardCountMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest, tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> getUpdateShardCountMethod;
    if ((getUpdateShardCountMethod = DataStreamsServiceGrpc.getUpdateShardCountMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getUpdateShardCountMethod = DataStreamsServiceGrpc.getUpdateShardCountMethod) == null) {
          DataStreamsServiceGrpc.getUpdateShardCountMethod = getUpdateShardCountMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest, tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateShardCount"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("UpdateShardCount"))
              .build();
        }
      }
    }
    return getUpdateShardCountMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> getRegisterStreamConsumerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RegisterStreamConsumer",
      requestType = tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> getRegisterStreamConsumerMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> getRegisterStreamConsumerMethod;
    if ((getRegisterStreamConsumerMethod = DataStreamsServiceGrpc.getRegisterStreamConsumerMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getRegisterStreamConsumerMethod = DataStreamsServiceGrpc.getRegisterStreamConsumerMethod) == null) {
          DataStreamsServiceGrpc.getRegisterStreamConsumerMethod = getRegisterStreamConsumerMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterStreamConsumer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("RegisterStreamConsumer"))
              .build();
        }
      }
    }
    return getRegisterStreamConsumerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> getDeregisterStreamConsumerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeregisterStreamConsumer",
      requestType = tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> getDeregisterStreamConsumerMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> getDeregisterStreamConsumerMethod;
    if ((getDeregisterStreamConsumerMethod = DataStreamsServiceGrpc.getDeregisterStreamConsumerMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDeregisterStreamConsumerMethod = DataStreamsServiceGrpc.getDeregisterStreamConsumerMethod) == null) {
          DataStreamsServiceGrpc.getDeregisterStreamConsumerMethod = getDeregisterStreamConsumerMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeregisterStreamConsumer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DeregisterStreamConsumer"))
              .build();
        }
      }
    }
    return getDeregisterStreamConsumerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> getDescribeStreamConsumerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeStreamConsumer",
      requestType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest,
      tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> getDescribeStreamConsumerMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> getDescribeStreamConsumerMethod;
    if ((getDescribeStreamConsumerMethod = DataStreamsServiceGrpc.getDescribeStreamConsumerMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDescribeStreamConsumerMethod = DataStreamsServiceGrpc.getDescribeStreamConsumerMethod) == null) {
          DataStreamsServiceGrpc.getDescribeStreamConsumerMethod = getDescribeStreamConsumerMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest, tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeStreamConsumer"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DescribeStreamConsumer"))
              .build();
        }
      }
    }
    return getDescribeStreamConsumerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest,
      tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> getListStreamConsumersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListStreamConsumers",
      requestType = tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest,
      tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> getListStreamConsumersMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest, tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> getListStreamConsumersMethod;
    if ((getListStreamConsumersMethod = DataStreamsServiceGrpc.getListStreamConsumersMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getListStreamConsumersMethod = DataStreamsServiceGrpc.getListStreamConsumersMethod) == null) {
          DataStreamsServiceGrpc.getListStreamConsumersMethod = getListStreamConsumersMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest, tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListStreamConsumers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("ListStreamConsumers"))
              .build();
        }
      }
    }
    return getListStreamConsumersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> getAddTagsToStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddTagsToStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> getAddTagsToStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest, tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> getAddTagsToStreamMethod;
    if ((getAddTagsToStreamMethod = DataStreamsServiceGrpc.getAddTagsToStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getAddTagsToStreamMethod = DataStreamsServiceGrpc.getAddTagsToStreamMethod) == null) {
          DataStreamsServiceGrpc.getAddTagsToStreamMethod = getAddTagsToStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest, tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddTagsToStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("AddTagsToStream"))
              .build();
        }
      }
    }
    return getAddTagsToStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest,
      tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> getDisableEnhancedMonitoringMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DisableEnhancedMonitoring",
      requestType = tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest,
      tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> getDisableEnhancedMonitoringMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest, tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> getDisableEnhancedMonitoringMethod;
    if ((getDisableEnhancedMonitoringMethod = DataStreamsServiceGrpc.getDisableEnhancedMonitoringMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getDisableEnhancedMonitoringMethod = DataStreamsServiceGrpc.getDisableEnhancedMonitoringMethod) == null) {
          DataStreamsServiceGrpc.getDisableEnhancedMonitoringMethod = getDisableEnhancedMonitoringMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest, tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DisableEnhancedMonitoring"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("DisableEnhancedMonitoring"))
              .build();
        }
      }
    }
    return getDisableEnhancedMonitoringMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest,
      tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> getEnableEnhancedMonitoringMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EnableEnhancedMonitoring",
      requestType = tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest,
      tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> getEnableEnhancedMonitoringMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest, tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> getEnableEnhancedMonitoringMethod;
    if ((getEnableEnhancedMonitoringMethod = DataStreamsServiceGrpc.getEnableEnhancedMonitoringMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getEnableEnhancedMonitoringMethod = DataStreamsServiceGrpc.getEnableEnhancedMonitoringMethod) == null) {
          DataStreamsServiceGrpc.getEnableEnhancedMonitoringMethod = getEnableEnhancedMonitoringMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest, tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EnableEnhancedMonitoring"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("EnableEnhancedMonitoring"))
              .build();
        }
      }
    }
    return getEnableEnhancedMonitoringMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> getListTagsForStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListTagsForStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> getListTagsForStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest, tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> getListTagsForStreamMethod;
    if ((getListTagsForStreamMethod = DataStreamsServiceGrpc.getListTagsForStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getListTagsForStreamMethod = DataStreamsServiceGrpc.getListTagsForStreamMethod) == null) {
          DataStreamsServiceGrpc.getListTagsForStreamMethod = getListTagsForStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest, tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListTagsForStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("ListTagsForStream"))
              .build();
        }
      }
    }
    return getListTagsForStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest,
      tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> getMergeShardsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MergeShards",
      requestType = tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest,
      tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> getMergeShardsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest, tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> getMergeShardsMethod;
    if ((getMergeShardsMethod = DataStreamsServiceGrpc.getMergeShardsMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getMergeShardsMethod = DataStreamsServiceGrpc.getMergeShardsMethod) == null) {
          DataStreamsServiceGrpc.getMergeShardsMethod = getMergeShardsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest, tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MergeShards"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("MergeShards"))
              .build();
        }
      }
    }
    return getMergeShardsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> getRemoveTagsFromStreamMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveTagsFromStream",
      requestType = tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest,
      tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> getRemoveTagsFromStreamMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest, tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> getRemoveTagsFromStreamMethod;
    if ((getRemoveTagsFromStreamMethod = DataStreamsServiceGrpc.getRemoveTagsFromStreamMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getRemoveTagsFromStreamMethod = DataStreamsServiceGrpc.getRemoveTagsFromStreamMethod) == null) {
          DataStreamsServiceGrpc.getRemoveTagsFromStreamMethod = getRemoveTagsFromStreamMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest, tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveTagsFromStream"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("RemoveTagsFromStream"))
              .build();
        }
      }
    }
    return getRemoveTagsFromStreamMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SplitShardRequest,
      tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> getSplitShardMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SplitShard",
      requestType = tech.ydb.datastreams.v1.Datastreams.SplitShardRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.SplitShardResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SplitShardRequest,
      tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> getSplitShardMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.SplitShardRequest, tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> getSplitShardMethod;
    if ((getSplitShardMethod = DataStreamsServiceGrpc.getSplitShardMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getSplitShardMethod = DataStreamsServiceGrpc.getSplitShardMethod) == null) {
          DataStreamsServiceGrpc.getSplitShardMethod = getSplitShardMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.SplitShardRequest, tech.ydb.datastreams.v1.Datastreams.SplitShardResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SplitShard"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.SplitShardRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.SplitShardResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("SplitShard"))
              .build();
        }
      }
    }
    return getSplitShardMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest,
      tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> getStartStreamEncryptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StartStreamEncryption",
      requestType = tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest,
      tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> getStartStreamEncryptionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest, tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> getStartStreamEncryptionMethod;
    if ((getStartStreamEncryptionMethod = DataStreamsServiceGrpc.getStartStreamEncryptionMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getStartStreamEncryptionMethod = DataStreamsServiceGrpc.getStartStreamEncryptionMethod) == null) {
          DataStreamsServiceGrpc.getStartStreamEncryptionMethod = getStartStreamEncryptionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest, tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StartStreamEncryption"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("StartStreamEncryption"))
              .build();
        }
      }
    }
    return getStartStreamEncryptionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest,
      tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> getStopStreamEncryptionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StopStreamEncryption",
      requestType = tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest.class,
      responseType = tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest,
      tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> getStopStreamEncryptionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest, tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> getStopStreamEncryptionMethod;
    if ((getStopStreamEncryptionMethod = DataStreamsServiceGrpc.getStopStreamEncryptionMethod) == null) {
      synchronized (DataStreamsServiceGrpc.class) {
        if ((getStopStreamEncryptionMethod = DataStreamsServiceGrpc.getStopStreamEncryptionMethod) == null) {
          DataStreamsServiceGrpc.getStopStreamEncryptionMethod = getStopStreamEncryptionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest, tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StopStreamEncryption"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new DataStreamsServiceMethodDescriptorSupplier("StopStreamEncryption"))
              .build();
        }
      }
    }
    return getStopStreamEncryptionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DataStreamsServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceStub>() {
        @java.lang.Override
        public DataStreamsServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataStreamsServiceStub(channel, callOptions);
        }
      };
    return DataStreamsServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DataStreamsServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceBlockingStub>() {
        @java.lang.Override
        public DataStreamsServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataStreamsServiceBlockingStub(channel, callOptions);
        }
      };
    return DataStreamsServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DataStreamsServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<DataStreamsServiceFutureStub>() {
        @java.lang.Override
        public DataStreamsServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new DataStreamsServiceFutureStub(channel, callOptions);
        }
      };
    return DataStreamsServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class DataStreamsServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Basic stream manipulation methods
     * </pre>
     */
    public void createStream(tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateStreamMethod(), responseObserver);
    }

    /**
     */
    public void listStreams(tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListStreamsMethod(), responseObserver);
    }

    /**
     */
    public void deleteStream(tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteStreamMethod(), responseObserver);
    }

    /**
     */
    public void describeStream(tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeStreamMethod(), responseObserver);
    }

    /**
     */
    public void listShards(tech.ydb.datastreams.v1.Datastreams.ListShardsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListShardsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Data methods
     * </pre>
     */
    public void putRecord(tech.ydb.datastreams.v1.Datastreams.PutRecordRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPutRecordMethod(), responseObserver);
    }

    /**
     */
    public void putRecords(tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPutRecordsMethod(), responseObserver);
    }

    /**
     */
    public void getRecords(tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetRecordsMethod(), responseObserver);
    }

    /**
     */
    public void getShardIterator(tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetShardIteratorMethod(), responseObserver);
    }

    /**
     */
    public void subscribeToShard(tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSubscribeToShardMethod(), responseObserver);
    }

    /**
     */
    public void describeLimits(tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeLimitsMethod(), responseObserver);
    }

    /**
     */
    public void describeStreamSummary(tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeStreamSummaryMethod(), responseObserver);
    }

    /**
     */
    public void decreaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDecreaseStreamRetentionPeriodMethod(), responseObserver);
    }

    /**
     */
    public void increaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getIncreaseStreamRetentionPeriodMethod(), responseObserver);
    }

    /**
     */
    public void updateShardCount(tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getUpdateShardCountMethod(), responseObserver);
    }

    /**
     * <pre>
     * stream consumer methods
     * </pre>
     */
    public void registerStreamConsumer(tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRegisterStreamConsumerMethod(), responseObserver);
    }

    /**
     */
    public void deregisterStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeregisterStreamConsumerMethod(), responseObserver);
    }

    /**
     */
    public void describeStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeStreamConsumerMethod(), responseObserver);
    }

    /**
     */
    public void listStreamConsumers(tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListStreamConsumersMethod(), responseObserver);
    }

    /**
     * <pre>
     * Least priority methods from API.
     * </pre>
     */
    public void addTagsToStream(tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAddTagsToStreamMethod(), responseObserver);
    }

    /**
     */
    public void disableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDisableEnhancedMonitoringMethod(), responseObserver);
    }

    /**
     */
    public void enableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getEnableEnhancedMonitoringMethod(), responseObserver);
    }

    /**
     */
    public void listTagsForStream(tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListTagsForStreamMethod(), responseObserver);
    }

    /**
     */
    public void mergeShards(tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getMergeShardsMethod(), responseObserver);
    }

    /**
     */
    public void removeTagsFromStream(tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRemoveTagsFromStreamMethod(), responseObserver);
    }

    /**
     */
    public void splitShard(tech.ydb.datastreams.v1.Datastreams.SplitShardRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSplitShardMethod(), responseObserver);
    }

    /**
     */
    public void startStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getStartStreamEncryptionMethod(), responseObserver);
    }

    /**
     */
    public void stopStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getStopStreamEncryptionMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse>(
                  this, METHODID_CREATE_STREAM)))
          .addMethod(
            getListStreamsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest,
                tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse>(
                  this, METHODID_LIST_STREAMS)))
          .addMethod(
            getDeleteStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse>(
                  this, METHODID_DELETE_STREAM)))
          .addMethod(
            getDescribeStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse>(
                  this, METHODID_DESCRIBE_STREAM)))
          .addMethod(
            getListShardsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.ListShardsRequest,
                tech.ydb.datastreams.v1.Datastreams.ListShardsResponse>(
                  this, METHODID_LIST_SHARDS)))
          .addMethod(
            getPutRecordMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.PutRecordRequest,
                tech.ydb.datastreams.v1.Datastreams.PutRecordResponse>(
                  this, METHODID_PUT_RECORD)))
          .addMethod(
            getPutRecordsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest,
                tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse>(
                  this, METHODID_PUT_RECORDS)))
          .addMethod(
            getGetRecordsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest,
                tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse>(
                  this, METHODID_GET_RECORDS)))
          .addMethod(
            getGetShardIteratorMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest,
                tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse>(
                  this, METHODID_GET_SHARD_ITERATOR)))
          .addMethod(
            getSubscribeToShardMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest,
                tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse>(
                  this, METHODID_SUBSCRIBE_TO_SHARD)))
          .addMethod(
            getDescribeLimitsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest,
                tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse>(
                  this, METHODID_DESCRIBE_LIMITS)))
          .addMethod(
            getDescribeStreamSummaryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest,
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse>(
                  this, METHODID_DESCRIBE_STREAM_SUMMARY)))
          .addMethod(
            getDecreaseStreamRetentionPeriodMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest,
                tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse>(
                  this, METHODID_DECREASE_STREAM_RETENTION_PERIOD)))
          .addMethod(
            getIncreaseStreamRetentionPeriodMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest,
                tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse>(
                  this, METHODID_INCREASE_STREAM_RETENTION_PERIOD)))
          .addMethod(
            getUpdateShardCountMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest,
                tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse>(
                  this, METHODID_UPDATE_SHARD_COUNT)))
          .addMethod(
            getRegisterStreamConsumerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest,
                tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse>(
                  this, METHODID_REGISTER_STREAM_CONSUMER)))
          .addMethod(
            getDeregisterStreamConsumerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest,
                tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse>(
                  this, METHODID_DEREGISTER_STREAM_CONSUMER)))
          .addMethod(
            getDescribeStreamConsumerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest,
                tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse>(
                  this, METHODID_DESCRIBE_STREAM_CONSUMER)))
          .addMethod(
            getListStreamConsumersMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest,
                tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse>(
                  this, METHODID_LIST_STREAM_CONSUMERS)))
          .addMethod(
            getAddTagsToStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse>(
                  this, METHODID_ADD_TAGS_TO_STREAM)))
          .addMethod(
            getDisableEnhancedMonitoringMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest,
                tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse>(
                  this, METHODID_DISABLE_ENHANCED_MONITORING)))
          .addMethod(
            getEnableEnhancedMonitoringMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest,
                tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse>(
                  this, METHODID_ENABLE_ENHANCED_MONITORING)))
          .addMethod(
            getListTagsForStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse>(
                  this, METHODID_LIST_TAGS_FOR_STREAM)))
          .addMethod(
            getMergeShardsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest,
                tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse>(
                  this, METHODID_MERGE_SHARDS)))
          .addMethod(
            getRemoveTagsFromStreamMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest,
                tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse>(
                  this, METHODID_REMOVE_TAGS_FROM_STREAM)))
          .addMethod(
            getSplitShardMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.SplitShardRequest,
                tech.ydb.datastreams.v1.Datastreams.SplitShardResponse>(
                  this, METHODID_SPLIT_SHARD)))
          .addMethod(
            getStartStreamEncryptionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest,
                tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse>(
                  this, METHODID_START_STREAM_ENCRYPTION)))
          .addMethod(
            getStopStreamEncryptionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest,
                tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse>(
                  this, METHODID_STOP_STREAM_ENCRYPTION)))
          .build();
    }
  }

  /**
   */
  public static final class DataStreamsServiceStub extends io.grpc.stub.AbstractAsyncStub<DataStreamsServiceStub> {
    private DataStreamsServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DataStreamsServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataStreamsServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Basic stream manipulation methods
     * </pre>
     */
    public void createStream(tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listStreams(tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListStreamsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteStream(tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeStream(tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listShards(tech.ydb.datastreams.v1.Datastreams.ListShardsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListShardsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Data methods
     * </pre>
     */
    public void putRecord(tech.ydb.datastreams.v1.Datastreams.PutRecordRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPutRecordMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void putRecords(tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPutRecordsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getRecords(tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetRecordsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getShardIterator(tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetShardIteratorMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void subscribeToShard(tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getSubscribeToShardMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeLimits(tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeLimitsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeStreamSummary(tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeStreamSummaryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void decreaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDecreaseStreamRetentionPeriodMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void increaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getIncreaseStreamRetentionPeriodMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateShardCount(tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getUpdateShardCountMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * stream consumer methods
     * </pre>
     */
    public void registerStreamConsumer(tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRegisterStreamConsumerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deregisterStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeregisterStreamConsumerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void describeStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeStreamConsumerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listStreamConsumers(tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListStreamConsumersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Least priority methods from API.
     * </pre>
     */
    public void addTagsToStream(tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAddTagsToStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void disableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisableEnhancedMonitoringMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void enableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getEnableEnhancedMonitoringMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listTagsForStream(tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListTagsForStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void mergeShards(tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getMergeShardsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void removeTagsFromStream(tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRemoveTagsFromStreamMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void splitShard(tech.ydb.datastreams.v1.Datastreams.SplitShardRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSplitShardMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void startStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStartStreamEncryptionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void stopStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getStopStreamEncryptionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class DataStreamsServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<DataStreamsServiceBlockingStub> {
    private DataStreamsServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DataStreamsServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataStreamsServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Basic stream manipulation methods
     * </pre>
     */
    public tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse createStream(tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse listStreams(tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListStreamsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse deleteStream(tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse describeStream(tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.ListShardsResponse listShards(tech.ydb.datastreams.v1.Datastreams.ListShardsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListShardsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Data methods
     * </pre>
     */
    public tech.ydb.datastreams.v1.Datastreams.PutRecordResponse putRecord(tech.ydb.datastreams.v1.Datastreams.PutRecordRequest request) {
      return blockingUnaryCall(
          getChannel(), getPutRecordMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse putRecords(tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest request) {
      return blockingUnaryCall(
          getChannel(), getPutRecordsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse getRecords(tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetRecordsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse getShardIterator(tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetShardIteratorMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse> subscribeToShard(
        tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getSubscribeToShardMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse describeLimits(tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeLimitsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse describeStreamSummary(tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeStreamSummaryMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse decreaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest request) {
      return blockingUnaryCall(
          getChannel(), getDecreaseStreamRetentionPeriodMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse increaseStreamRetentionPeriod(tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest request) {
      return blockingUnaryCall(
          getChannel(), getIncreaseStreamRetentionPeriodMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse updateShardCount(tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest request) {
      return blockingUnaryCall(
          getChannel(), getUpdateShardCountMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * stream consumer methods
     * </pre>
     */
    public tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse registerStreamConsumer(tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest request) {
      return blockingUnaryCall(
          getChannel(), getRegisterStreamConsumerMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse deregisterStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeregisterStreamConsumerMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse describeStreamConsumer(tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeStreamConsumerMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse listStreamConsumers(tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest request) {
      return blockingUnaryCall(
          getChannel(), getListStreamConsumersMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Least priority methods from API.
     * </pre>
     */
    public tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse addTagsToStream(tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getAddTagsToStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse disableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest request) {
      return blockingUnaryCall(
          getChannel(), getDisableEnhancedMonitoringMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse enableEnhancedMonitoring(tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest request) {
      return blockingUnaryCall(
          getChannel(), getEnableEnhancedMonitoringMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse listTagsForStream(tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getListTagsForStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse mergeShards(tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest request) {
      return blockingUnaryCall(
          getChannel(), getMergeShardsMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse removeTagsFromStream(tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest request) {
      return blockingUnaryCall(
          getChannel(), getRemoveTagsFromStreamMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.SplitShardResponse splitShard(tech.ydb.datastreams.v1.Datastreams.SplitShardRequest request) {
      return blockingUnaryCall(
          getChannel(), getSplitShardMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse startStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest request) {
      return blockingUnaryCall(
          getChannel(), getStartStreamEncryptionMethod(), getCallOptions(), request);
    }

    /**
     */
    public tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse stopStreamEncryption(tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest request) {
      return blockingUnaryCall(
          getChannel(), getStopStreamEncryptionMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class DataStreamsServiceFutureStub extends io.grpc.stub.AbstractFutureStub<DataStreamsServiceFutureStub> {
    private DataStreamsServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DataStreamsServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DataStreamsServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Basic stream manipulation methods
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse> createStream(
        tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse> listStreams(
        tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListStreamsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse> deleteStream(
        tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse> describeStream(
        tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.ListShardsResponse> listShards(
        tech.ydb.datastreams.v1.Datastreams.ListShardsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListShardsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Data methods
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.PutRecordResponse> putRecord(
        tech.ydb.datastreams.v1.Datastreams.PutRecordRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPutRecordMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse> putRecords(
        tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPutRecordsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse> getRecords(
        tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetRecordsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse> getShardIterator(
        tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetShardIteratorMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse> describeLimits(
        tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeLimitsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse> describeStreamSummary(
        tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeStreamSummaryMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
        tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDecreaseStreamRetentionPeriodMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
        tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getIncreaseStreamRetentionPeriodMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse> updateShardCount(
        tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getUpdateShardCountMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * stream consumer methods
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse> registerStreamConsumer(
        tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRegisterStreamConsumerMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse> deregisterStreamConsumer(
        tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeregisterStreamConsumerMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse> describeStreamConsumer(
        tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeStreamConsumerMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse> listStreamConsumers(
        tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListStreamConsumersMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Least priority methods from API.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse> addTagsToStream(
        tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAddTagsToStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(
        tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDisableEnhancedMonitoringMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(
        tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getEnableEnhancedMonitoringMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse> listTagsForStream(
        tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListTagsForStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse> mergeShards(
        tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getMergeShardsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse> removeTagsFromStream(
        tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRemoveTagsFromStreamMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.SplitShardResponse> splitShard(
        tech.ydb.datastreams.v1.Datastreams.SplitShardRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSplitShardMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse> startStreamEncryption(
        tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getStartStreamEncryptionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse> stopStreamEncryption(
        tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getStopStreamEncryptionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_STREAM = 0;
  private static final int METHODID_LIST_STREAMS = 1;
  private static final int METHODID_DELETE_STREAM = 2;
  private static final int METHODID_DESCRIBE_STREAM = 3;
  private static final int METHODID_LIST_SHARDS = 4;
  private static final int METHODID_PUT_RECORD = 5;
  private static final int METHODID_PUT_RECORDS = 6;
  private static final int METHODID_GET_RECORDS = 7;
  private static final int METHODID_GET_SHARD_ITERATOR = 8;
  private static final int METHODID_SUBSCRIBE_TO_SHARD = 9;
  private static final int METHODID_DESCRIBE_LIMITS = 10;
  private static final int METHODID_DESCRIBE_STREAM_SUMMARY = 11;
  private static final int METHODID_DECREASE_STREAM_RETENTION_PERIOD = 12;
  private static final int METHODID_INCREASE_STREAM_RETENTION_PERIOD = 13;
  private static final int METHODID_UPDATE_SHARD_COUNT = 14;
  private static final int METHODID_REGISTER_STREAM_CONSUMER = 15;
  private static final int METHODID_DEREGISTER_STREAM_CONSUMER = 16;
  private static final int METHODID_DESCRIBE_STREAM_CONSUMER = 17;
  private static final int METHODID_LIST_STREAM_CONSUMERS = 18;
  private static final int METHODID_ADD_TAGS_TO_STREAM = 19;
  private static final int METHODID_DISABLE_ENHANCED_MONITORING = 20;
  private static final int METHODID_ENABLE_ENHANCED_MONITORING = 21;
  private static final int METHODID_LIST_TAGS_FOR_STREAM = 22;
  private static final int METHODID_MERGE_SHARDS = 23;
  private static final int METHODID_REMOVE_TAGS_FROM_STREAM = 24;
  private static final int METHODID_SPLIT_SHARD = 25;
  private static final int METHODID_START_STREAM_ENCRYPTION = 26;
  private static final int METHODID_STOP_STREAM_ENCRYPTION = 27;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DataStreamsServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DataStreamsServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_STREAM:
          serviceImpl.createStream((tech.ydb.datastreams.v1.Datastreams.CreateStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.CreateStreamResponse>) responseObserver);
          break;
        case METHODID_LIST_STREAMS:
          serviceImpl.listStreams((tech.ydb.datastreams.v1.Datastreams.ListStreamsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamsResponse>) responseObserver);
          break;
        case METHODID_DELETE_STREAM:
          serviceImpl.deleteStream((tech.ydb.datastreams.v1.Datastreams.DeleteStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeleteStreamResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_STREAM:
          serviceImpl.describeStream((tech.ydb.datastreams.v1.Datastreams.DescribeStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamResponse>) responseObserver);
          break;
        case METHODID_LIST_SHARDS:
          serviceImpl.listShards((tech.ydb.datastreams.v1.Datastreams.ListShardsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListShardsResponse>) responseObserver);
          break;
        case METHODID_PUT_RECORD:
          serviceImpl.putRecord((tech.ydb.datastreams.v1.Datastreams.PutRecordRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordResponse>) responseObserver);
          break;
        case METHODID_PUT_RECORDS:
          serviceImpl.putRecords((tech.ydb.datastreams.v1.Datastreams.PutRecordsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.PutRecordsResponse>) responseObserver);
          break;
        case METHODID_GET_RECORDS:
          serviceImpl.getRecords((tech.ydb.datastreams.v1.Datastreams.GetRecordsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetRecordsResponse>) responseObserver);
          break;
        case METHODID_GET_SHARD_ITERATOR:
          serviceImpl.getShardIterator((tech.ydb.datastreams.v1.Datastreams.GetShardIteratorRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.GetShardIteratorResponse>) responseObserver);
          break;
        case METHODID_SUBSCRIBE_TO_SHARD:
          serviceImpl.subscribeToShard((tech.ydb.datastreams.v1.Datastreams.SubscribeToShardRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SubscribeToShardResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_LIMITS:
          serviceImpl.describeLimits((tech.ydb.datastreams.v1.Datastreams.DescribeLimitsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeLimitsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_STREAM_SUMMARY:
          serviceImpl.describeStreamSummary((tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamSummaryResponse>) responseObserver);
          break;
        case METHODID_DECREASE_STREAM_RETENTION_PERIOD:
          serviceImpl.decreaseStreamRetentionPeriod((tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DecreaseStreamRetentionPeriodResponse>) responseObserver);
          break;
        case METHODID_INCREASE_STREAM_RETENTION_PERIOD:
          serviceImpl.increaseStreamRetentionPeriod((tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.IncreaseStreamRetentionPeriodResponse>) responseObserver);
          break;
        case METHODID_UPDATE_SHARD_COUNT:
          serviceImpl.updateShardCount((tech.ydb.datastreams.v1.Datastreams.UpdateShardCountRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.UpdateShardCountResponse>) responseObserver);
          break;
        case METHODID_REGISTER_STREAM_CONSUMER:
          serviceImpl.registerStreamConsumer((tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RegisterStreamConsumerResponse>) responseObserver);
          break;
        case METHODID_DEREGISTER_STREAM_CONSUMER:
          serviceImpl.deregisterStreamConsumer((tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DeregisterStreamConsumerResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_STREAM_CONSUMER:
          serviceImpl.describeStreamConsumer((tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DescribeStreamConsumerResponse>) responseObserver);
          break;
        case METHODID_LIST_STREAM_CONSUMERS:
          serviceImpl.listStreamConsumers((tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListStreamConsumersResponse>) responseObserver);
          break;
        case METHODID_ADD_TAGS_TO_STREAM:
          serviceImpl.addTagsToStream((tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.AddTagsToStreamResponse>) responseObserver);
          break;
        case METHODID_DISABLE_ENHANCED_MONITORING:
          serviceImpl.disableEnhancedMonitoring((tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.DisableEnhancedMonitoringResponse>) responseObserver);
          break;
        case METHODID_ENABLE_ENHANCED_MONITORING:
          serviceImpl.enableEnhancedMonitoring((tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.EnableEnhancedMonitoringResponse>) responseObserver);
          break;
        case METHODID_LIST_TAGS_FOR_STREAM:
          serviceImpl.listTagsForStream((tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.ListTagsForStreamResponse>) responseObserver);
          break;
        case METHODID_MERGE_SHARDS:
          serviceImpl.mergeShards((tech.ydb.datastreams.v1.Datastreams.MergeShardsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.MergeShardsResponse>) responseObserver);
          break;
        case METHODID_REMOVE_TAGS_FROM_STREAM:
          serviceImpl.removeTagsFromStream((tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.RemoveTagsFromStreamResponse>) responseObserver);
          break;
        case METHODID_SPLIT_SHARD:
          serviceImpl.splitShard((tech.ydb.datastreams.v1.Datastreams.SplitShardRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.SplitShardResponse>) responseObserver);
          break;
        case METHODID_START_STREAM_ENCRYPTION:
          serviceImpl.startStreamEncryption((tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StartStreamEncryptionResponse>) responseObserver);
          break;
        case METHODID_STOP_STREAM_ENCRYPTION:
          serviceImpl.stopStreamEncryption((tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.datastreams.v1.Datastreams.StopStreamEncryptionResponse>) responseObserver);
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

  private static abstract class DataStreamsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DataStreamsServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.datastreams.v1.YdbDatastreamsV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DataStreamsService");
    }
  }

  private static final class DataStreamsServiceFileDescriptorSupplier
      extends DataStreamsServiceBaseDescriptorSupplier {
    DataStreamsServiceFileDescriptorSupplier() {}
  }

  private static final class DataStreamsServiceMethodDescriptorSupplier
      extends DataStreamsServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DataStreamsServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (DataStreamsServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DataStreamsServiceFileDescriptorSupplier())
              .addMethod(getCreateStreamMethod())
              .addMethod(getListStreamsMethod())
              .addMethod(getDeleteStreamMethod())
              .addMethod(getDescribeStreamMethod())
              .addMethod(getListShardsMethod())
              .addMethod(getPutRecordMethod())
              .addMethod(getPutRecordsMethod())
              .addMethod(getGetRecordsMethod())
              .addMethod(getGetShardIteratorMethod())
              .addMethod(getSubscribeToShardMethod())
              .addMethod(getDescribeLimitsMethod())
              .addMethod(getDescribeStreamSummaryMethod())
              .addMethod(getDecreaseStreamRetentionPeriodMethod())
              .addMethod(getIncreaseStreamRetentionPeriodMethod())
              .addMethod(getUpdateShardCountMethod())
              .addMethod(getRegisterStreamConsumerMethod())
              .addMethod(getDeregisterStreamConsumerMethod())
              .addMethod(getDescribeStreamConsumerMethod())
              .addMethod(getListStreamConsumersMethod())
              .addMethod(getAddTagsToStreamMethod())
              .addMethod(getDisableEnhancedMonitoringMethod())
              .addMethod(getEnableEnhancedMonitoringMethod())
              .addMethod(getListTagsForStreamMethod())
              .addMethod(getMergeShardsMethod())
              .addMethod(getRemoveTagsFromStreamMethod())
              .addMethod(getSplitShardMethod())
              .addMethod(getStartStreamEncryptionMethod())
              .addMethod(getStopStreamEncryptionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
