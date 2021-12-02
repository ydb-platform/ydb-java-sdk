package com.yandex.query.v1;

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
    comments = "Source: kikimr/public/api/grpc/yq_v1.proto")
public final class YandexQueryServiceGrpc {

  private YandexQueryServiceGrpc() {}

  public static final String SERVICE_NAME = "YandexQuery.V1.YandexQueryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateQueryRequest,
      com.yandex.query.YandexQueryProtos.CreateQueryResponse> getCreateQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateQuery",
      requestType = com.yandex.query.YandexQueryProtos.CreateQueryRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.CreateQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateQueryRequest,
      com.yandex.query.YandexQueryProtos.CreateQueryResponse> getCreateQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateQueryRequest, com.yandex.query.YandexQueryProtos.CreateQueryResponse> getCreateQueryMethod;
    if ((getCreateQueryMethod = YandexQueryServiceGrpc.getCreateQueryMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getCreateQueryMethod = YandexQueryServiceGrpc.getCreateQueryMethod) == null) {
          YandexQueryServiceGrpc.getCreateQueryMethod = getCreateQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.CreateQueryRequest, com.yandex.query.YandexQueryProtos.CreateQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("CreateQuery"))
              .build();
        }
      }
    }
    return getCreateQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListQueriesRequest,
      com.yandex.query.YandexQueryProtos.ListQueriesResponse> getListQueriesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListQueries",
      requestType = com.yandex.query.YandexQueryProtos.ListQueriesRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ListQueriesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListQueriesRequest,
      com.yandex.query.YandexQueryProtos.ListQueriesResponse> getListQueriesMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListQueriesRequest, com.yandex.query.YandexQueryProtos.ListQueriesResponse> getListQueriesMethod;
    if ((getListQueriesMethod = YandexQueryServiceGrpc.getListQueriesMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getListQueriesMethod = YandexQueryServiceGrpc.getListQueriesMethod) == null) {
          YandexQueryServiceGrpc.getListQueriesMethod = getListQueriesMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ListQueriesRequest, com.yandex.query.YandexQueryProtos.ListQueriesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListQueries"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListQueriesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListQueriesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ListQueries"))
              .build();
        }
      }
    }
    return getListQueriesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeQueryRequest,
      com.yandex.query.YandexQueryProtos.DescribeQueryResponse> getDescribeQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeQuery",
      requestType = com.yandex.query.YandexQueryProtos.DescribeQueryRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DescribeQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeQueryRequest,
      com.yandex.query.YandexQueryProtos.DescribeQueryResponse> getDescribeQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeQueryRequest, com.yandex.query.YandexQueryProtos.DescribeQueryResponse> getDescribeQueryMethod;
    if ((getDescribeQueryMethod = YandexQueryServiceGrpc.getDescribeQueryMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDescribeQueryMethod = YandexQueryServiceGrpc.getDescribeQueryMethod) == null) {
          YandexQueryServiceGrpc.getDescribeQueryMethod = getDescribeQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DescribeQueryRequest, com.yandex.query.YandexQueryProtos.DescribeQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DescribeQuery"))
              .build();
        }
      }
    }
    return getDescribeQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetQueryStatusRequest,
      com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> getGetQueryStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetQueryStatus",
      requestType = com.yandex.query.YandexQueryProtos.GetQueryStatusRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.GetQueryStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetQueryStatusRequest,
      com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> getGetQueryStatusMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetQueryStatusRequest, com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> getGetQueryStatusMethod;
    if ((getGetQueryStatusMethod = YandexQueryServiceGrpc.getGetQueryStatusMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getGetQueryStatusMethod = YandexQueryServiceGrpc.getGetQueryStatusMethod) == null) {
          YandexQueryServiceGrpc.getGetQueryStatusMethod = getGetQueryStatusMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.GetQueryStatusRequest, com.yandex.query.YandexQueryProtos.GetQueryStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetQueryStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.GetQueryStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.GetQueryStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("GetQueryStatus"))
              .build();
        }
      }
    }
    return getGetQueryStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyQueryRequest,
      com.yandex.query.YandexQueryProtos.ModifyQueryResponse> getModifyQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyQuery",
      requestType = com.yandex.query.YandexQueryProtos.ModifyQueryRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ModifyQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyQueryRequest,
      com.yandex.query.YandexQueryProtos.ModifyQueryResponse> getModifyQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyQueryRequest, com.yandex.query.YandexQueryProtos.ModifyQueryResponse> getModifyQueryMethod;
    if ((getModifyQueryMethod = YandexQueryServiceGrpc.getModifyQueryMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getModifyQueryMethod = YandexQueryServiceGrpc.getModifyQueryMethod) == null) {
          YandexQueryServiceGrpc.getModifyQueryMethod = getModifyQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ModifyQueryRequest, com.yandex.query.YandexQueryProtos.ModifyQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ModifyQuery"))
              .build();
        }
      }
    }
    return getModifyQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteQueryRequest,
      com.yandex.query.YandexQueryProtos.DeleteQueryResponse> getDeleteQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteQuery",
      requestType = com.yandex.query.YandexQueryProtos.DeleteQueryRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DeleteQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteQueryRequest,
      com.yandex.query.YandexQueryProtos.DeleteQueryResponse> getDeleteQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteQueryRequest, com.yandex.query.YandexQueryProtos.DeleteQueryResponse> getDeleteQueryMethod;
    if ((getDeleteQueryMethod = YandexQueryServiceGrpc.getDeleteQueryMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDeleteQueryMethod = YandexQueryServiceGrpc.getDeleteQueryMethod) == null) {
          YandexQueryServiceGrpc.getDeleteQueryMethod = getDeleteQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DeleteQueryRequest, com.yandex.query.YandexQueryProtos.DeleteQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DeleteQuery"))
              .build();
        }
      }
    }
    return getDeleteQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlQueryRequest,
      com.yandex.query.YandexQueryProtos.ControlQueryResponse> getControlQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ControlQuery",
      requestType = com.yandex.query.YandexQueryProtos.ControlQueryRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ControlQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlQueryRequest,
      com.yandex.query.YandexQueryProtos.ControlQueryResponse> getControlQueryMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlQueryRequest, com.yandex.query.YandexQueryProtos.ControlQueryResponse> getControlQueryMethod;
    if ((getControlQueryMethod = YandexQueryServiceGrpc.getControlQueryMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getControlQueryMethod = YandexQueryServiceGrpc.getControlQueryMethod) == null) {
          YandexQueryServiceGrpc.getControlQueryMethod = getControlQueryMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ControlQueryRequest, com.yandex.query.YandexQueryProtos.ControlQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ControlQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ControlQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ControlQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ControlQuery"))
              .build();
        }
      }
    }
    return getControlQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetResultDataRequest,
      com.yandex.query.YandexQueryProtos.GetResultDataResponse> getGetResultDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetResultData",
      requestType = com.yandex.query.YandexQueryProtos.GetResultDataRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.GetResultDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetResultDataRequest,
      com.yandex.query.YandexQueryProtos.GetResultDataResponse> getGetResultDataMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.GetResultDataRequest, com.yandex.query.YandexQueryProtos.GetResultDataResponse> getGetResultDataMethod;
    if ((getGetResultDataMethod = YandexQueryServiceGrpc.getGetResultDataMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getGetResultDataMethod = YandexQueryServiceGrpc.getGetResultDataMethod) == null) {
          YandexQueryServiceGrpc.getGetResultDataMethod = getGetResultDataMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.GetResultDataRequest, com.yandex.query.YandexQueryProtos.GetResultDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetResultData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.GetResultDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.GetResultDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("GetResultData"))
              .build();
        }
      }
    }
    return getGetResultDataMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListJobsRequest,
      com.yandex.query.YandexQueryProtos.ListJobsResponse> getListJobsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListJobs",
      requestType = com.yandex.query.YandexQueryProtos.ListJobsRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ListJobsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListJobsRequest,
      com.yandex.query.YandexQueryProtos.ListJobsResponse> getListJobsMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListJobsRequest, com.yandex.query.YandexQueryProtos.ListJobsResponse> getListJobsMethod;
    if ((getListJobsMethod = YandexQueryServiceGrpc.getListJobsMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getListJobsMethod = YandexQueryServiceGrpc.getListJobsMethod) == null) {
          YandexQueryServiceGrpc.getListJobsMethod = getListJobsMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ListJobsRequest, com.yandex.query.YandexQueryProtos.ListJobsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListJobs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListJobsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListJobsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ListJobs"))
              .build();
        }
      }
    }
    return getListJobsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeJobRequest,
      com.yandex.query.YandexQueryProtos.DescribeJobResponse> getDescribeJobMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeJob",
      requestType = com.yandex.query.YandexQueryProtos.DescribeJobRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DescribeJobResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeJobRequest,
      com.yandex.query.YandexQueryProtos.DescribeJobResponse> getDescribeJobMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeJobRequest, com.yandex.query.YandexQueryProtos.DescribeJobResponse> getDescribeJobMethod;
    if ((getDescribeJobMethod = YandexQueryServiceGrpc.getDescribeJobMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDescribeJobMethod = YandexQueryServiceGrpc.getDescribeJobMethod) == null) {
          YandexQueryServiceGrpc.getDescribeJobMethod = getDescribeJobMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DescribeJobRequest, com.yandex.query.YandexQueryProtos.DescribeJobResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeJob"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeJobRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeJobResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DescribeJob"))
              .build();
        }
      }
    }
    return getDescribeJobMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateConnectionRequest,
      com.yandex.query.YandexQueryProtos.CreateConnectionResponse> getCreateConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateConnection",
      requestType = com.yandex.query.YandexQueryProtos.CreateConnectionRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.CreateConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateConnectionRequest,
      com.yandex.query.YandexQueryProtos.CreateConnectionResponse> getCreateConnectionMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateConnectionRequest, com.yandex.query.YandexQueryProtos.CreateConnectionResponse> getCreateConnectionMethod;
    if ((getCreateConnectionMethod = YandexQueryServiceGrpc.getCreateConnectionMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getCreateConnectionMethod = YandexQueryServiceGrpc.getCreateConnectionMethod) == null) {
          YandexQueryServiceGrpc.getCreateConnectionMethod = getCreateConnectionMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.CreateConnectionRequest, com.yandex.query.YandexQueryProtos.CreateConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("CreateConnection"))
              .build();
        }
      }
    }
    return getCreateConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListConnectionsRequest,
      com.yandex.query.YandexQueryProtos.ListConnectionsResponse> getListConnectionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListConnections",
      requestType = com.yandex.query.YandexQueryProtos.ListConnectionsRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ListConnectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListConnectionsRequest,
      com.yandex.query.YandexQueryProtos.ListConnectionsResponse> getListConnectionsMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListConnectionsRequest, com.yandex.query.YandexQueryProtos.ListConnectionsResponse> getListConnectionsMethod;
    if ((getListConnectionsMethod = YandexQueryServiceGrpc.getListConnectionsMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getListConnectionsMethod = YandexQueryServiceGrpc.getListConnectionsMethod) == null) {
          YandexQueryServiceGrpc.getListConnectionsMethod = getListConnectionsMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ListConnectionsRequest, com.yandex.query.YandexQueryProtos.ListConnectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListConnections"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListConnectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListConnectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ListConnections"))
              .build();
        }
      }
    }
    return getListConnectionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeConnectionRequest,
      com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> getDescribeConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeConnection",
      requestType = com.yandex.query.YandexQueryProtos.DescribeConnectionRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DescribeConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeConnectionRequest,
      com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> getDescribeConnectionMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeConnectionRequest, com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> getDescribeConnectionMethod;
    if ((getDescribeConnectionMethod = YandexQueryServiceGrpc.getDescribeConnectionMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDescribeConnectionMethod = YandexQueryServiceGrpc.getDescribeConnectionMethod) == null) {
          YandexQueryServiceGrpc.getDescribeConnectionMethod = getDescribeConnectionMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DescribeConnectionRequest, com.yandex.query.YandexQueryProtos.DescribeConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DescribeConnection"))
              .build();
        }
      }
    }
    return getDescribeConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyConnectionRequest,
      com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> getModifyConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyConnection",
      requestType = com.yandex.query.YandexQueryProtos.ModifyConnectionRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ModifyConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyConnectionRequest,
      com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> getModifyConnectionMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyConnectionRequest, com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> getModifyConnectionMethod;
    if ((getModifyConnectionMethod = YandexQueryServiceGrpc.getModifyConnectionMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getModifyConnectionMethod = YandexQueryServiceGrpc.getModifyConnectionMethod) == null) {
          YandexQueryServiceGrpc.getModifyConnectionMethod = getModifyConnectionMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ModifyConnectionRequest, com.yandex.query.YandexQueryProtos.ModifyConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ModifyConnection"))
              .build();
        }
      }
    }
    return getModifyConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteConnectionRequest,
      com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> getDeleteConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteConnection",
      requestType = com.yandex.query.YandexQueryProtos.DeleteConnectionRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DeleteConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteConnectionRequest,
      com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> getDeleteConnectionMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteConnectionRequest, com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> getDeleteConnectionMethod;
    if ((getDeleteConnectionMethod = YandexQueryServiceGrpc.getDeleteConnectionMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDeleteConnectionMethod = YandexQueryServiceGrpc.getDeleteConnectionMethod) == null) {
          YandexQueryServiceGrpc.getDeleteConnectionMethod = getDeleteConnectionMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DeleteConnectionRequest, com.yandex.query.YandexQueryProtos.DeleteConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DeleteConnection"))
              .build();
        }
      }
    }
    return getDeleteConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateBindingRequest,
      com.yandex.query.YandexQueryProtos.CreateBindingResponse> getCreateBindingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateBinding",
      requestType = com.yandex.query.YandexQueryProtos.CreateBindingRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.CreateBindingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateBindingRequest,
      com.yandex.query.YandexQueryProtos.CreateBindingResponse> getCreateBindingMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateBindingRequest, com.yandex.query.YandexQueryProtos.CreateBindingResponse> getCreateBindingMethod;
    if ((getCreateBindingMethod = YandexQueryServiceGrpc.getCreateBindingMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getCreateBindingMethod = YandexQueryServiceGrpc.getCreateBindingMethod) == null) {
          YandexQueryServiceGrpc.getCreateBindingMethod = getCreateBindingMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.CreateBindingRequest, com.yandex.query.YandexQueryProtos.CreateBindingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateBinding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateBindingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateBindingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("CreateBinding"))
              .build();
        }
      }
    }
    return getCreateBindingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListBindingsRequest,
      com.yandex.query.YandexQueryProtos.ListBindingsResponse> getListBindingsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListBindings",
      requestType = com.yandex.query.YandexQueryProtos.ListBindingsRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ListBindingsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListBindingsRequest,
      com.yandex.query.YandexQueryProtos.ListBindingsResponse> getListBindingsMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListBindingsRequest, com.yandex.query.YandexQueryProtos.ListBindingsResponse> getListBindingsMethod;
    if ((getListBindingsMethod = YandexQueryServiceGrpc.getListBindingsMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getListBindingsMethod = YandexQueryServiceGrpc.getListBindingsMethod) == null) {
          YandexQueryServiceGrpc.getListBindingsMethod = getListBindingsMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ListBindingsRequest, com.yandex.query.YandexQueryProtos.ListBindingsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListBindings"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListBindingsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListBindingsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ListBindings"))
              .build();
        }
      }
    }
    return getListBindingsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeBindingRequest,
      com.yandex.query.YandexQueryProtos.DescribeBindingResponse> getDescribeBindingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeBinding",
      requestType = com.yandex.query.YandexQueryProtos.DescribeBindingRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DescribeBindingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeBindingRequest,
      com.yandex.query.YandexQueryProtos.DescribeBindingResponse> getDescribeBindingMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeBindingRequest, com.yandex.query.YandexQueryProtos.DescribeBindingResponse> getDescribeBindingMethod;
    if ((getDescribeBindingMethod = YandexQueryServiceGrpc.getDescribeBindingMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDescribeBindingMethod = YandexQueryServiceGrpc.getDescribeBindingMethod) == null) {
          YandexQueryServiceGrpc.getDescribeBindingMethod = getDescribeBindingMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DescribeBindingRequest, com.yandex.query.YandexQueryProtos.DescribeBindingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeBinding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeBindingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeBindingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DescribeBinding"))
              .build();
        }
      }
    }
    return getDescribeBindingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyBindingRequest,
      com.yandex.query.YandexQueryProtos.ModifyBindingResponse> getModifyBindingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyBinding",
      requestType = com.yandex.query.YandexQueryProtos.ModifyBindingRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ModifyBindingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyBindingRequest,
      com.yandex.query.YandexQueryProtos.ModifyBindingResponse> getModifyBindingMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyBindingRequest, com.yandex.query.YandexQueryProtos.ModifyBindingResponse> getModifyBindingMethod;
    if ((getModifyBindingMethod = YandexQueryServiceGrpc.getModifyBindingMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getModifyBindingMethod = YandexQueryServiceGrpc.getModifyBindingMethod) == null) {
          YandexQueryServiceGrpc.getModifyBindingMethod = getModifyBindingMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ModifyBindingRequest, com.yandex.query.YandexQueryProtos.ModifyBindingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyBinding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyBindingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyBindingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ModifyBinding"))
              .build();
        }
      }
    }
    return getModifyBindingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteBindingRequest,
      com.yandex.query.YandexQueryProtos.DeleteBindingResponse> getDeleteBindingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteBinding",
      requestType = com.yandex.query.YandexQueryProtos.DeleteBindingRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DeleteBindingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteBindingRequest,
      com.yandex.query.YandexQueryProtos.DeleteBindingResponse> getDeleteBindingMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteBindingRequest, com.yandex.query.YandexQueryProtos.DeleteBindingResponse> getDeleteBindingMethod;
    if ((getDeleteBindingMethod = YandexQueryServiceGrpc.getDeleteBindingMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDeleteBindingMethod = YandexQueryServiceGrpc.getDeleteBindingMethod) == null) {
          YandexQueryServiceGrpc.getDeleteBindingMethod = getDeleteBindingMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DeleteBindingRequest, com.yandex.query.YandexQueryProtos.DeleteBindingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteBinding"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteBindingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteBindingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DeleteBinding"))
              .build();
        }
      }
    }
    return getDeleteBindingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static YandexQueryServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceStub>() {
        @java.lang.Override
        public YandexQueryServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YandexQueryServiceStub(channel, callOptions);
        }
      };
    return YandexQueryServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static YandexQueryServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceBlockingStub>() {
        @java.lang.Override
        public YandexQueryServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YandexQueryServiceBlockingStub(channel, callOptions);
        }
      };
    return YandexQueryServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static YandexQueryServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<YandexQueryServiceFutureStub>() {
        @java.lang.Override
        public YandexQueryServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new YandexQueryServiceFutureStub(channel, callOptions);
        }
      };
    return YandexQueryServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class YandexQueryServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Query
     * Query is the text of an SQL request, the results of the last run and the state after the last run (partitions offsets, consumer in YDS)
     * Create a query object with a given SQL
     * </pre>
     */
    public void createQuery(com.yandex.query.YandexQueryProtos.CreateQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a list of brief queries objects
     * </pre>
     */
    public void listQueries(com.yandex.query.YandexQueryProtos.ListQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListQueriesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListQueriesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get full information about the object of the query
     * </pre>
     */
    public void describeQuery(com.yandex.query.YandexQueryProtos.DescribeQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get status of the query
     * </pre>
     */
    public void getQueryStatus(com.yandex.query.YandexQueryProtos.GetQueryStatusRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetQueryStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the query (acl, name, ...)
     * </pre>
     */
    public void modifyQuery(com.yandex.query.YandexQueryProtos.ModifyQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Completely delete the query
     * </pre>
     */
    public void deleteQuery(com.yandex.query.YandexQueryProtos.DeleteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the state of the query lifecycle
     * </pre>
     */
    public void controlQuery(com.yandex.query.YandexQueryProtos.ControlQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getControlQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a results page
     * </pre>
     */
    public void getResultData(com.yandex.query.YandexQueryProtos.GetResultDataRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetResultDataResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetResultDataMethod(), responseObserver);
    }

    /**
     * <pre>
     * Job
     * Job - appears immediately after starting the request and contains the request metadata
     * Get a list of jobs
     * </pre>
     */
    public void listJobs(com.yandex.query.YandexQueryProtos.ListJobsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListJobsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListJobsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get information about the job
     * </pre>
     */
    public void describeJob(com.yandex.query.YandexQueryProtos.DescribeJobRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeJobResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeJobMethod(), responseObserver);
    }

    /**
     * <pre>
     * Connection
     * Connection - entity that describes connection points. This can be imagined as an analogue of a network address.
     * Create a connection object (ObjectStorage, YDB, YDS, ...)
     * </pre>
     */
    public void createConnection(com.yandex.query.YandexQueryProtos.CreateConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a list of connections objects
     * </pre>
     */
    public void listConnections(com.yandex.query.YandexQueryProtos.ListConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListConnectionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListConnectionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get information about the object of the connection
     * </pre>
     */
    public void describeConnection(com.yandex.query.YandexQueryProtos.DescribeConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the connection
     * </pre>
     */
    public void modifyConnection(com.yandex.query.YandexQueryProtos.ModifyConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Completely delete the connection
     * </pre>
     */
    public void deleteConnection(com.yandex.query.YandexQueryProtos.DeleteConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Binding
     * Binding - entity using which a schema is assigned to non-schematic data
     * Create a binding object - bind schema with ObjectStorage object or YDS stream
     * </pre>
     */
    public void createBinding(com.yandex.query.YandexQueryProtos.CreateBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateBindingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateBindingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a list of bindings objects
     * </pre>
     */
    public void listBindings(com.yandex.query.YandexQueryProtos.ListBindingsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListBindingsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListBindingsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get information about the object of the binding
     * </pre>
     */
    public void describeBinding(com.yandex.query.YandexQueryProtos.DescribeBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeBindingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeBindingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the binding
     * </pre>
     */
    public void modifyBinding(com.yandex.query.YandexQueryProtos.ModifyBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyBindingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyBindingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Completely delete the binding
     * </pre>
     */
    public void deleteBinding(com.yandex.query.YandexQueryProtos.DeleteBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteBindingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteBindingMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.CreateQueryRequest,
                com.yandex.query.YandexQueryProtos.CreateQueryResponse>(
                  this, METHODID_CREATE_QUERY)))
          .addMethod(
            getListQueriesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ListQueriesRequest,
                com.yandex.query.YandexQueryProtos.ListQueriesResponse>(
                  this, METHODID_LIST_QUERIES)))
          .addMethod(
            getDescribeQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DescribeQueryRequest,
                com.yandex.query.YandexQueryProtos.DescribeQueryResponse>(
                  this, METHODID_DESCRIBE_QUERY)))
          .addMethod(
            getGetQueryStatusMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.GetQueryStatusRequest,
                com.yandex.query.YandexQueryProtos.GetQueryStatusResponse>(
                  this, METHODID_GET_QUERY_STATUS)))
          .addMethod(
            getModifyQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ModifyQueryRequest,
                com.yandex.query.YandexQueryProtos.ModifyQueryResponse>(
                  this, METHODID_MODIFY_QUERY)))
          .addMethod(
            getDeleteQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DeleteQueryRequest,
                com.yandex.query.YandexQueryProtos.DeleteQueryResponse>(
                  this, METHODID_DELETE_QUERY)))
          .addMethod(
            getControlQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ControlQueryRequest,
                com.yandex.query.YandexQueryProtos.ControlQueryResponse>(
                  this, METHODID_CONTROL_QUERY)))
          .addMethod(
            getGetResultDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.GetResultDataRequest,
                com.yandex.query.YandexQueryProtos.GetResultDataResponse>(
                  this, METHODID_GET_RESULT_DATA)))
          .addMethod(
            getListJobsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ListJobsRequest,
                com.yandex.query.YandexQueryProtos.ListJobsResponse>(
                  this, METHODID_LIST_JOBS)))
          .addMethod(
            getDescribeJobMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DescribeJobRequest,
                com.yandex.query.YandexQueryProtos.DescribeJobResponse>(
                  this, METHODID_DESCRIBE_JOB)))
          .addMethod(
            getCreateConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.CreateConnectionRequest,
                com.yandex.query.YandexQueryProtos.CreateConnectionResponse>(
                  this, METHODID_CREATE_CONNECTION)))
          .addMethod(
            getListConnectionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ListConnectionsRequest,
                com.yandex.query.YandexQueryProtos.ListConnectionsResponse>(
                  this, METHODID_LIST_CONNECTIONS)))
          .addMethod(
            getDescribeConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DescribeConnectionRequest,
                com.yandex.query.YandexQueryProtos.DescribeConnectionResponse>(
                  this, METHODID_DESCRIBE_CONNECTION)))
          .addMethod(
            getModifyConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ModifyConnectionRequest,
                com.yandex.query.YandexQueryProtos.ModifyConnectionResponse>(
                  this, METHODID_MODIFY_CONNECTION)))
          .addMethod(
            getDeleteConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DeleteConnectionRequest,
                com.yandex.query.YandexQueryProtos.DeleteConnectionResponse>(
                  this, METHODID_DELETE_CONNECTION)))
          .addMethod(
            getCreateBindingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.CreateBindingRequest,
                com.yandex.query.YandexQueryProtos.CreateBindingResponse>(
                  this, METHODID_CREATE_BINDING)))
          .addMethod(
            getListBindingsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ListBindingsRequest,
                com.yandex.query.YandexQueryProtos.ListBindingsResponse>(
                  this, METHODID_LIST_BINDINGS)))
          .addMethod(
            getDescribeBindingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DescribeBindingRequest,
                com.yandex.query.YandexQueryProtos.DescribeBindingResponse>(
                  this, METHODID_DESCRIBE_BINDING)))
          .addMethod(
            getModifyBindingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ModifyBindingRequest,
                com.yandex.query.YandexQueryProtos.ModifyBindingResponse>(
                  this, METHODID_MODIFY_BINDING)))
          .addMethod(
            getDeleteBindingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DeleteBindingRequest,
                com.yandex.query.YandexQueryProtos.DeleteBindingResponse>(
                  this, METHODID_DELETE_BINDING)))
          .build();
    }
  }

  /**
   */
  public static final class YandexQueryServiceStub extends io.grpc.stub.AbstractAsyncStub<YandexQueryServiceStub> {
    private YandexQueryServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YandexQueryServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YandexQueryServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Query
     * Query is the text of an SQL request, the results of the last run and the state after the last run (partitions offsets, consumer in YDS)
     * Create a query object with a given SQL
     * </pre>
     */
    public void createQuery(com.yandex.query.YandexQueryProtos.CreateQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a list of brief queries objects
     * </pre>
     */
    public void listQueries(com.yandex.query.YandexQueryProtos.ListQueriesRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListQueriesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListQueriesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get full information about the object of the query
     * </pre>
     */
    public void describeQuery(com.yandex.query.YandexQueryProtos.DescribeQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get status of the query
     * </pre>
     */
    public void getQueryStatus(com.yandex.query.YandexQueryProtos.GetQueryStatusRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetQueryStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the query (acl, name, ...)
     * </pre>
     */
    public void modifyQuery(com.yandex.query.YandexQueryProtos.ModifyQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Completely delete the query
     * </pre>
     */
    public void deleteQuery(com.yandex.query.YandexQueryProtos.DeleteQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the state of the query lifecycle
     * </pre>
     */
    public void controlQuery(com.yandex.query.YandexQueryProtos.ControlQueryRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getControlQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a results page
     * </pre>
     */
    public void getResultData(com.yandex.query.YandexQueryProtos.GetResultDataRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetResultDataResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetResultDataMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Job
     * Job - appears immediately after starting the request and contains the request metadata
     * Get a list of jobs
     * </pre>
     */
    public void listJobs(com.yandex.query.YandexQueryProtos.ListJobsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListJobsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListJobsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get information about the job
     * </pre>
     */
    public void describeJob(com.yandex.query.YandexQueryProtos.DescribeJobRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeJobResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeJobMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Connection
     * Connection - entity that describes connection points. This can be imagined as an analogue of a network address.
     * Create a connection object (ObjectStorage, YDB, YDS, ...)
     * </pre>
     */
    public void createConnection(com.yandex.query.YandexQueryProtos.CreateConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a list of connections objects
     * </pre>
     */
    public void listConnections(com.yandex.query.YandexQueryProtos.ListConnectionsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListConnectionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListConnectionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get information about the object of the connection
     * </pre>
     */
    public void describeConnection(com.yandex.query.YandexQueryProtos.DescribeConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the connection
     * </pre>
     */
    public void modifyConnection(com.yandex.query.YandexQueryProtos.ModifyConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Completely delete the connection
     * </pre>
     */
    public void deleteConnection(com.yandex.query.YandexQueryProtos.DeleteConnectionRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Binding
     * Binding - entity using which a schema is assigned to non-schematic data
     * Create a binding object - bind schema with ObjectStorage object or YDS stream
     * </pre>
     */
    public void createBinding(com.yandex.query.YandexQueryProtos.CreateBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateBindingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateBindingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a list of bindings objects
     * </pre>
     */
    public void listBindings(com.yandex.query.YandexQueryProtos.ListBindingsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListBindingsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListBindingsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get information about the object of the binding
     * </pre>
     */
    public void describeBinding(com.yandex.query.YandexQueryProtos.DescribeBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeBindingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeBindingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the binding
     * </pre>
     */
    public void modifyBinding(com.yandex.query.YandexQueryProtos.ModifyBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyBindingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyBindingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Completely delete the binding
     * </pre>
     */
    public void deleteBinding(com.yandex.query.YandexQueryProtos.DeleteBindingRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteBindingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteBindingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class YandexQueryServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<YandexQueryServiceBlockingStub> {
    private YandexQueryServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YandexQueryServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YandexQueryServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Query
     * Query is the text of an SQL request, the results of the last run and the state after the last run (partitions offsets, consumer in YDS)
     * Create a query object with a given SQL
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.CreateQueryResponse createQuery(com.yandex.query.YandexQueryProtos.CreateQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of brief queries objects
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ListQueriesResponse listQueries(com.yandex.query.YandexQueryProtos.ListQueriesRequest request) {
      return blockingUnaryCall(
          getChannel(), getListQueriesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get full information about the object of the query
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DescribeQueryResponse describeQuery(com.yandex.query.YandexQueryProtos.DescribeQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get status of the query
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.GetQueryStatusResponse getQueryStatus(com.yandex.query.YandexQueryProtos.GetQueryStatusRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetQueryStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the attributes of the query (acl, name, ...)
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ModifyQueryResponse modifyQuery(com.yandex.query.YandexQueryProtos.ModifyQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Completely delete the query
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DeleteQueryResponse deleteQuery(com.yandex.query.YandexQueryProtos.DeleteQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the state of the query lifecycle
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ControlQueryResponse controlQuery(com.yandex.query.YandexQueryProtos.ControlQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getControlQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a results page
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.GetResultDataResponse getResultData(com.yandex.query.YandexQueryProtos.GetResultDataRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetResultDataMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Job
     * Job - appears immediately after starting the request and contains the request metadata
     * Get a list of jobs
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ListJobsResponse listJobs(com.yandex.query.YandexQueryProtos.ListJobsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListJobsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get information about the job
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DescribeJobResponse describeJob(com.yandex.query.YandexQueryProtos.DescribeJobRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeJobMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Connection
     * Connection - entity that describes connection points. This can be imagined as an analogue of a network address.
     * Create a connection object (ObjectStorage, YDB, YDS, ...)
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.CreateConnectionResponse createConnection(com.yandex.query.YandexQueryProtos.CreateConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of connections objects
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ListConnectionsResponse listConnections(com.yandex.query.YandexQueryProtos.ListConnectionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListConnectionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get information about the object of the connection
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DescribeConnectionResponse describeConnection(com.yandex.query.YandexQueryProtos.DescribeConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the attributes of the connection
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ModifyConnectionResponse modifyConnection(com.yandex.query.YandexQueryProtos.ModifyConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Completely delete the connection
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DeleteConnectionResponse deleteConnection(com.yandex.query.YandexQueryProtos.DeleteConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Binding
     * Binding - entity using which a schema is assigned to non-schematic data
     * Create a binding object - bind schema with ObjectStorage object or YDS stream
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.CreateBindingResponse createBinding(com.yandex.query.YandexQueryProtos.CreateBindingRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateBindingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of bindings objects
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ListBindingsResponse listBindings(com.yandex.query.YandexQueryProtos.ListBindingsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListBindingsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get information about the object of the binding
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DescribeBindingResponse describeBinding(com.yandex.query.YandexQueryProtos.DescribeBindingRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeBindingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the attributes of the binding
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ModifyBindingResponse modifyBinding(com.yandex.query.YandexQueryProtos.ModifyBindingRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyBindingMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Completely delete the binding
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DeleteBindingResponse deleteBinding(com.yandex.query.YandexQueryProtos.DeleteBindingRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteBindingMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class YandexQueryServiceFutureStub extends io.grpc.stub.AbstractFutureStub<YandexQueryServiceFutureStub> {
    private YandexQueryServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected YandexQueryServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new YandexQueryServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Query
     * Query is the text of an SQL request, the results of the last run and the state after the last run (partitions offsets, consumer in YDS)
     * Create a query object with a given SQL
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.CreateQueryResponse> createQuery(
        com.yandex.query.YandexQueryProtos.CreateQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a list of brief queries objects
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ListQueriesResponse> listQueries(
        com.yandex.query.YandexQueryProtos.ListQueriesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListQueriesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get full information about the object of the query
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DescribeQueryResponse> describeQuery(
        com.yandex.query.YandexQueryProtos.DescribeQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get status of the query
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.GetQueryStatusResponse> getQueryStatus(
        com.yandex.query.YandexQueryProtos.GetQueryStatusRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetQueryStatusMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the attributes of the query (acl, name, ...)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ModifyQueryResponse> modifyQuery(
        com.yandex.query.YandexQueryProtos.ModifyQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Completely delete the query
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DeleteQueryResponse> deleteQuery(
        com.yandex.query.YandexQueryProtos.DeleteQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the state of the query lifecycle
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ControlQueryResponse> controlQuery(
        com.yandex.query.YandexQueryProtos.ControlQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getControlQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a results page
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.GetResultDataResponse> getResultData(
        com.yandex.query.YandexQueryProtos.GetResultDataRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetResultDataMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Job
     * Job - appears immediately after starting the request and contains the request metadata
     * Get a list of jobs
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ListJobsResponse> listJobs(
        com.yandex.query.YandexQueryProtos.ListJobsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListJobsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get information about the job
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DescribeJobResponse> describeJob(
        com.yandex.query.YandexQueryProtos.DescribeJobRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeJobMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Connection
     * Connection - entity that describes connection points. This can be imagined as an analogue of a network address.
     * Create a connection object (ObjectStorage, YDB, YDS, ...)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.CreateConnectionResponse> createConnection(
        com.yandex.query.YandexQueryProtos.CreateConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a list of connections objects
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ListConnectionsResponse> listConnections(
        com.yandex.query.YandexQueryProtos.ListConnectionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListConnectionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get information about the object of the connection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DescribeConnectionResponse> describeConnection(
        com.yandex.query.YandexQueryProtos.DescribeConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the attributes of the connection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ModifyConnectionResponse> modifyConnection(
        com.yandex.query.YandexQueryProtos.ModifyConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Completely delete the connection
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DeleteConnectionResponse> deleteConnection(
        com.yandex.query.YandexQueryProtos.DeleteConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Binding
     * Binding - entity using which a schema is assigned to non-schematic data
     * Create a binding object - bind schema with ObjectStorage object or YDS stream
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.CreateBindingResponse> createBinding(
        com.yandex.query.YandexQueryProtos.CreateBindingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateBindingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a list of bindings objects
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ListBindingsResponse> listBindings(
        com.yandex.query.YandexQueryProtos.ListBindingsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListBindingsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get information about the object of the binding
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DescribeBindingResponse> describeBinding(
        com.yandex.query.YandexQueryProtos.DescribeBindingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeBindingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the attributes of the binding
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ModifyBindingResponse> modifyBinding(
        com.yandex.query.YandexQueryProtos.ModifyBindingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyBindingMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Completely delete the binding
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DeleteBindingResponse> deleteBinding(
        com.yandex.query.YandexQueryProtos.DeleteBindingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteBindingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_QUERY = 0;
  private static final int METHODID_LIST_QUERIES = 1;
  private static final int METHODID_DESCRIBE_QUERY = 2;
  private static final int METHODID_GET_QUERY_STATUS = 3;
  private static final int METHODID_MODIFY_QUERY = 4;
  private static final int METHODID_DELETE_QUERY = 5;
  private static final int METHODID_CONTROL_QUERY = 6;
  private static final int METHODID_GET_RESULT_DATA = 7;
  private static final int METHODID_LIST_JOBS = 8;
  private static final int METHODID_DESCRIBE_JOB = 9;
  private static final int METHODID_CREATE_CONNECTION = 10;
  private static final int METHODID_LIST_CONNECTIONS = 11;
  private static final int METHODID_DESCRIBE_CONNECTION = 12;
  private static final int METHODID_MODIFY_CONNECTION = 13;
  private static final int METHODID_DELETE_CONNECTION = 14;
  private static final int METHODID_CREATE_BINDING = 15;
  private static final int METHODID_LIST_BINDINGS = 16;
  private static final int METHODID_DESCRIBE_BINDING = 17;
  private static final int METHODID_MODIFY_BINDING = 18;
  private static final int METHODID_DELETE_BINDING = 19;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final YandexQueryServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(YandexQueryServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_QUERY:
          serviceImpl.createQuery((com.yandex.query.YandexQueryProtos.CreateQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateQueryResponse>) responseObserver);
          break;
        case METHODID_LIST_QUERIES:
          serviceImpl.listQueries((com.yandex.query.YandexQueryProtos.ListQueriesRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListQueriesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_QUERY:
          serviceImpl.describeQuery((com.yandex.query.YandexQueryProtos.DescribeQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeQueryResponse>) responseObserver);
          break;
        case METHODID_GET_QUERY_STATUS:
          serviceImpl.getQueryStatus((com.yandex.query.YandexQueryProtos.GetQueryStatusRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetQueryStatusResponse>) responseObserver);
          break;
        case METHODID_MODIFY_QUERY:
          serviceImpl.modifyQuery((com.yandex.query.YandexQueryProtos.ModifyQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyQueryResponse>) responseObserver);
          break;
        case METHODID_DELETE_QUERY:
          serviceImpl.deleteQuery((com.yandex.query.YandexQueryProtos.DeleteQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteQueryResponse>) responseObserver);
          break;
        case METHODID_CONTROL_QUERY:
          serviceImpl.controlQuery((com.yandex.query.YandexQueryProtos.ControlQueryRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlQueryResponse>) responseObserver);
          break;
        case METHODID_GET_RESULT_DATA:
          serviceImpl.getResultData((com.yandex.query.YandexQueryProtos.GetResultDataRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetResultDataResponse>) responseObserver);
          break;
        case METHODID_LIST_JOBS:
          serviceImpl.listJobs((com.yandex.query.YandexQueryProtos.ListJobsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListJobsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_JOB:
          serviceImpl.describeJob((com.yandex.query.YandexQueryProtos.DescribeJobRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeJobResponse>) responseObserver);
          break;
        case METHODID_CREATE_CONNECTION:
          serviceImpl.createConnection((com.yandex.query.YandexQueryProtos.CreateConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateConnectionResponse>) responseObserver);
          break;
        case METHODID_LIST_CONNECTIONS:
          serviceImpl.listConnections((com.yandex.query.YandexQueryProtos.ListConnectionsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListConnectionsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_CONNECTION:
          serviceImpl.describeConnection((com.yandex.query.YandexQueryProtos.DescribeConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeConnectionResponse>) responseObserver);
          break;
        case METHODID_MODIFY_CONNECTION:
          serviceImpl.modifyConnection((com.yandex.query.YandexQueryProtos.ModifyConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyConnectionResponse>) responseObserver);
          break;
        case METHODID_DELETE_CONNECTION:
          serviceImpl.deleteConnection((com.yandex.query.YandexQueryProtos.DeleteConnectionRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteConnectionResponse>) responseObserver);
          break;
        case METHODID_CREATE_BINDING:
          serviceImpl.createBinding((com.yandex.query.YandexQueryProtos.CreateBindingRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateBindingResponse>) responseObserver);
          break;
        case METHODID_LIST_BINDINGS:
          serviceImpl.listBindings((com.yandex.query.YandexQueryProtos.ListBindingsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListBindingsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_BINDING:
          serviceImpl.describeBinding((com.yandex.query.YandexQueryProtos.DescribeBindingRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeBindingResponse>) responseObserver);
          break;
        case METHODID_MODIFY_BINDING:
          serviceImpl.modifyBinding((com.yandex.query.YandexQueryProtos.ModifyBindingRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyBindingResponse>) responseObserver);
          break;
        case METHODID_DELETE_BINDING:
          serviceImpl.deleteBinding((com.yandex.query.YandexQueryProtos.DeleteBindingRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteBindingResponse>) responseObserver);
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

  private static abstract class YandexQueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    YandexQueryServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.yandex.query.v1.YqV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("YandexQueryService");
    }
  }

  private static final class YandexQueryServiceFileDescriptorSupplier
      extends YandexQueryServiceBaseDescriptorSupplier {
    YandexQueryServiceFileDescriptorSupplier() {}
  }

  private static final class YandexQueryServiceMethodDescriptorSupplier
      extends YandexQueryServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    YandexQueryServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (YandexQueryServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new YandexQueryServiceFileDescriptorSupplier())
              .addMethod(getCreateQueryMethod())
              .addMethod(getListQueriesMethod())
              .addMethod(getDescribeQueryMethod())
              .addMethod(getGetQueryStatusMethod())
              .addMethod(getModifyQueryMethod())
              .addMethod(getDeleteQueryMethod())
              .addMethod(getControlQueryMethod())
              .addMethod(getGetResultDataMethod())
              .addMethod(getListJobsMethod())
              .addMethod(getDescribeJobMethod())
              .addMethod(getCreateConnectionMethod())
              .addMethod(getListConnectionsMethod())
              .addMethod(getDescribeConnectionMethod())
              .addMethod(getModifyConnectionMethod())
              .addMethod(getDeleteConnectionMethod())
              .addMethod(getCreateBindingMethod())
              .addMethod(getListBindingsMethod())
              .addMethod(getDescribeBindingMethod())
              .addMethod(getModifyBindingMethod())
              .addMethod(getDeleteBindingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
