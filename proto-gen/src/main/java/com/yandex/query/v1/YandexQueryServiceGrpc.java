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
    comments = "Source: kikimr/public/api/grpc/draft/yq_v1.proto")
public final class YandexQueryServiceGrpc {

  private YandexQueryServiceGrpc() {}

  public static final String SERVICE_NAME = "YandexQuery.V1.YandexQueryService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateOperationRequest,
      com.yandex.query.YandexQueryProtos.CreateOperationResponse> getCreateOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateOperation",
      requestType = com.yandex.query.YandexQueryProtos.CreateOperationRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.CreateOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateOperationRequest,
      com.yandex.query.YandexQueryProtos.CreateOperationResponse> getCreateOperationMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.CreateOperationRequest, com.yandex.query.YandexQueryProtos.CreateOperationResponse> getCreateOperationMethod;
    if ((getCreateOperationMethod = YandexQueryServiceGrpc.getCreateOperationMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getCreateOperationMethod = YandexQueryServiceGrpc.getCreateOperationMethod) == null) {
          YandexQueryServiceGrpc.getCreateOperationMethod = getCreateOperationMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.CreateOperationRequest, com.yandex.query.YandexQueryProtos.CreateOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.CreateOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("CreateOperation"))
              .build();
        }
      }
    }
    return getCreateOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListOperationsRequest,
      com.yandex.query.YandexQueryProtos.ListOperationsResponse> getListOperationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListOperations",
      requestType = com.yandex.query.YandexQueryProtos.ListOperationsRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ListOperationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListOperationsRequest,
      com.yandex.query.YandexQueryProtos.ListOperationsResponse> getListOperationsMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ListOperationsRequest, com.yandex.query.YandexQueryProtos.ListOperationsResponse> getListOperationsMethod;
    if ((getListOperationsMethod = YandexQueryServiceGrpc.getListOperationsMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getListOperationsMethod = YandexQueryServiceGrpc.getListOperationsMethod) == null) {
          YandexQueryServiceGrpc.getListOperationsMethod = getListOperationsMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ListOperationsRequest, com.yandex.query.YandexQueryProtos.ListOperationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListOperations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListOperationsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ListOperationsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ListOperations"))
              .build();
        }
      }
    }
    return getListOperationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeOperationRequest,
      com.yandex.query.YandexQueryProtos.DescribeOperationResponse> getDescribeOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeOperation",
      requestType = com.yandex.query.YandexQueryProtos.DescribeOperationRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DescribeOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeOperationRequest,
      com.yandex.query.YandexQueryProtos.DescribeOperationResponse> getDescribeOperationMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DescribeOperationRequest, com.yandex.query.YandexQueryProtos.DescribeOperationResponse> getDescribeOperationMethod;
    if ((getDescribeOperationMethod = YandexQueryServiceGrpc.getDescribeOperationMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDescribeOperationMethod = YandexQueryServiceGrpc.getDescribeOperationMethod) == null) {
          YandexQueryServiceGrpc.getDescribeOperationMethod = getDescribeOperationMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DescribeOperationRequest, com.yandex.query.YandexQueryProtos.DescribeOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DescribeOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DescribeOperation"))
              .build();
        }
      }
    }
    return getDescribeOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyOperationRequest,
      com.yandex.query.YandexQueryProtos.ModifyOperationResponse> getModifyOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ModifyOperation",
      requestType = com.yandex.query.YandexQueryProtos.ModifyOperationRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ModifyOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyOperationRequest,
      com.yandex.query.YandexQueryProtos.ModifyOperationResponse> getModifyOperationMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ModifyOperationRequest, com.yandex.query.YandexQueryProtos.ModifyOperationResponse> getModifyOperationMethod;
    if ((getModifyOperationMethod = YandexQueryServiceGrpc.getModifyOperationMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getModifyOperationMethod = YandexQueryServiceGrpc.getModifyOperationMethod) == null) {
          YandexQueryServiceGrpc.getModifyOperationMethod = getModifyOperationMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ModifyOperationRequest, com.yandex.query.YandexQueryProtos.ModifyOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ModifyOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ModifyOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ModifyOperation"))
              .build();
        }
      }
    }
    return getModifyOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteOperationRequest,
      com.yandex.query.YandexQueryProtos.DeleteOperationResponse> getDeleteOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteOperation",
      requestType = com.yandex.query.YandexQueryProtos.DeleteOperationRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.DeleteOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteOperationRequest,
      com.yandex.query.YandexQueryProtos.DeleteOperationResponse> getDeleteOperationMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.DeleteOperationRequest, com.yandex.query.YandexQueryProtos.DeleteOperationResponse> getDeleteOperationMethod;
    if ((getDeleteOperationMethod = YandexQueryServiceGrpc.getDeleteOperationMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getDeleteOperationMethod = YandexQueryServiceGrpc.getDeleteOperationMethod) == null) {
          YandexQueryServiceGrpc.getDeleteOperationMethod = getDeleteOperationMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.DeleteOperationRequest, com.yandex.query.YandexQueryProtos.DeleteOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.DeleteOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("DeleteOperation"))
              .build();
        }
      }
    }
    return getDeleteOperationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlOperationRequest,
      com.yandex.query.YandexQueryProtos.ControlOperationResponse> getControlOperationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ControlOperation",
      requestType = com.yandex.query.YandexQueryProtos.ControlOperationRequest.class,
      responseType = com.yandex.query.YandexQueryProtos.ControlOperationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlOperationRequest,
      com.yandex.query.YandexQueryProtos.ControlOperationResponse> getControlOperationMethod() {
    io.grpc.MethodDescriptor<com.yandex.query.YandexQueryProtos.ControlOperationRequest, com.yandex.query.YandexQueryProtos.ControlOperationResponse> getControlOperationMethod;
    if ((getControlOperationMethod = YandexQueryServiceGrpc.getControlOperationMethod) == null) {
      synchronized (YandexQueryServiceGrpc.class) {
        if ((getControlOperationMethod = YandexQueryServiceGrpc.getControlOperationMethod) == null) {
          YandexQueryServiceGrpc.getControlOperationMethod = getControlOperationMethod =
              io.grpc.MethodDescriptor.<com.yandex.query.YandexQueryProtos.ControlOperationRequest, com.yandex.query.YandexQueryProtos.ControlOperationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ControlOperation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ControlOperationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.yandex.query.YandexQueryProtos.ControlOperationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new YandexQueryServiceMethodDescriptorSupplier("ControlOperation"))
              .build();
        }
      }
    }
    return getControlOperationMethod;
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
     * Operations
     * Create an operation object with a given SQL
     * </pre>
     */
    public void createOperation(com.yandex.query.YandexQueryProtos.CreateOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a list of brief operations objects
     * </pre>
     */
    public void listOperations(com.yandex.query.YandexQueryProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListOperationsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getListOperationsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get full information about the object of the operation
     * </pre>
     */
    public void describeOperation(com.yandex.query.YandexQueryProtos.DescribeOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the operation (acl, name, ...)
     * </pre>
     */
    public void modifyOperation(com.yandex.query.YandexQueryProtos.ModifyOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getModifyOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Completely delete the operation
     * </pre>
     */
    public void deleteOperation(com.yandex.query.YandexQueryProtos.DeleteOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteOperationMethod(), responseObserver);
    }

    /**
     * <pre>
     * Change the state of the operation lifecycle
     * </pre>
     */
    public void controlOperation(com.yandex.query.YandexQueryProtos.ControlOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlOperationResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getControlOperationMethod(), responseObserver);
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
     * Connections
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
     * Bindings
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
            getCreateOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.CreateOperationRequest,
                com.yandex.query.YandexQueryProtos.CreateOperationResponse>(
                  this, METHODID_CREATE_OPERATION)))
          .addMethod(
            getListOperationsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ListOperationsRequest,
                com.yandex.query.YandexQueryProtos.ListOperationsResponse>(
                  this, METHODID_LIST_OPERATIONS)))
          .addMethod(
            getDescribeOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DescribeOperationRequest,
                com.yandex.query.YandexQueryProtos.DescribeOperationResponse>(
                  this, METHODID_DESCRIBE_OPERATION)))
          .addMethod(
            getModifyOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ModifyOperationRequest,
                com.yandex.query.YandexQueryProtos.ModifyOperationResponse>(
                  this, METHODID_MODIFY_OPERATION)))
          .addMethod(
            getDeleteOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.DeleteOperationRequest,
                com.yandex.query.YandexQueryProtos.DeleteOperationResponse>(
                  this, METHODID_DELETE_OPERATION)))
          .addMethod(
            getControlOperationMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.ControlOperationRequest,
                com.yandex.query.YandexQueryProtos.ControlOperationResponse>(
                  this, METHODID_CONTROL_OPERATION)))
          .addMethod(
            getGetResultDataMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.yandex.query.YandexQueryProtos.GetResultDataRequest,
                com.yandex.query.YandexQueryProtos.GetResultDataResponse>(
                  this, METHODID_GET_RESULT_DATA)))
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
     * Operations
     * Create an operation object with a given SQL
     * </pre>
     */
    public void createOperation(com.yandex.query.YandexQueryProtos.CreateOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a list of brief operations objects
     * </pre>
     */
    public void listOperations(com.yandex.query.YandexQueryProtos.ListOperationsRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListOperationsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getListOperationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get full information about the object of the operation
     * </pre>
     */
    public void describeOperation(com.yandex.query.YandexQueryProtos.DescribeOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the attributes of the operation (acl, name, ...)
     * </pre>
     */
    public void modifyOperation(com.yandex.query.YandexQueryProtos.ModifyOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getModifyOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Completely delete the operation
     * </pre>
     */
    public void deleteOperation(com.yandex.query.YandexQueryProtos.DeleteOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteOperationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Change the state of the operation lifecycle
     * </pre>
     */
    public void controlOperation(com.yandex.query.YandexQueryProtos.ControlOperationRequest request,
        io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlOperationResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getControlOperationMethod(), getCallOptions()), request, responseObserver);
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
     * Connections
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
     * Bindings
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
     * Operations
     * Create an operation object with a given SQL
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.CreateOperationResponse createOperation(com.yandex.query.YandexQueryProtos.CreateOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a list of brief operations objects
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ListOperationsResponse listOperations(com.yandex.query.YandexQueryProtos.ListOperationsRequest request) {
      return blockingUnaryCall(
          getChannel(), getListOperationsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get full information about the object of the operation
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DescribeOperationResponse describeOperation(com.yandex.query.YandexQueryProtos.DescribeOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the attributes of the operation (acl, name, ...)
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ModifyOperationResponse modifyOperation(com.yandex.query.YandexQueryProtos.ModifyOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getModifyOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Completely delete the operation
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.DeleteOperationResponse deleteOperation(com.yandex.query.YandexQueryProtos.DeleteOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteOperationMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Change the state of the operation lifecycle
     * </pre>
     */
    public com.yandex.query.YandexQueryProtos.ControlOperationResponse controlOperation(com.yandex.query.YandexQueryProtos.ControlOperationRequest request) {
      return blockingUnaryCall(
          getChannel(), getControlOperationMethod(), getCallOptions(), request);
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
     * Connections
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
     * Bindings
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
     * Operations
     * Create an operation object with a given SQL
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.CreateOperationResponse> createOperation(
        com.yandex.query.YandexQueryProtos.CreateOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a list of brief operations objects
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ListOperationsResponse> listOperations(
        com.yandex.query.YandexQueryProtos.ListOperationsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getListOperationsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get full information about the object of the operation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DescribeOperationResponse> describeOperation(
        com.yandex.query.YandexQueryProtos.DescribeOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the attributes of the operation (acl, name, ...)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ModifyOperationResponse> modifyOperation(
        com.yandex.query.YandexQueryProtos.ModifyOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getModifyOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Completely delete the operation
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.DeleteOperationResponse> deleteOperation(
        com.yandex.query.YandexQueryProtos.DeleteOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteOperationMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Change the state of the operation lifecycle
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.yandex.query.YandexQueryProtos.ControlOperationResponse> controlOperation(
        com.yandex.query.YandexQueryProtos.ControlOperationRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getControlOperationMethod(), getCallOptions()), request);
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
     * Connections
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
     * Bindings
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

  private static final int METHODID_CREATE_OPERATION = 0;
  private static final int METHODID_LIST_OPERATIONS = 1;
  private static final int METHODID_DESCRIBE_OPERATION = 2;
  private static final int METHODID_MODIFY_OPERATION = 3;
  private static final int METHODID_DELETE_OPERATION = 4;
  private static final int METHODID_CONTROL_OPERATION = 5;
  private static final int METHODID_GET_RESULT_DATA = 6;
  private static final int METHODID_CREATE_CONNECTION = 7;
  private static final int METHODID_LIST_CONNECTIONS = 8;
  private static final int METHODID_DESCRIBE_CONNECTION = 9;
  private static final int METHODID_MODIFY_CONNECTION = 10;
  private static final int METHODID_DELETE_CONNECTION = 11;
  private static final int METHODID_CREATE_BINDING = 12;
  private static final int METHODID_LIST_BINDINGS = 13;
  private static final int METHODID_DESCRIBE_BINDING = 14;
  private static final int METHODID_MODIFY_BINDING = 15;
  private static final int METHODID_DELETE_BINDING = 16;

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
        case METHODID_CREATE_OPERATION:
          serviceImpl.createOperation((com.yandex.query.YandexQueryProtos.CreateOperationRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.CreateOperationResponse>) responseObserver);
          break;
        case METHODID_LIST_OPERATIONS:
          serviceImpl.listOperations((com.yandex.query.YandexQueryProtos.ListOperationsRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ListOperationsResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_OPERATION:
          serviceImpl.describeOperation((com.yandex.query.YandexQueryProtos.DescribeOperationRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DescribeOperationResponse>) responseObserver);
          break;
        case METHODID_MODIFY_OPERATION:
          serviceImpl.modifyOperation((com.yandex.query.YandexQueryProtos.ModifyOperationRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ModifyOperationResponse>) responseObserver);
          break;
        case METHODID_DELETE_OPERATION:
          serviceImpl.deleteOperation((com.yandex.query.YandexQueryProtos.DeleteOperationRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.DeleteOperationResponse>) responseObserver);
          break;
        case METHODID_CONTROL_OPERATION:
          serviceImpl.controlOperation((com.yandex.query.YandexQueryProtos.ControlOperationRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.ControlOperationResponse>) responseObserver);
          break;
        case METHODID_GET_RESULT_DATA:
          serviceImpl.getResultData((com.yandex.query.YandexQueryProtos.GetResultDataRequest) request,
              (io.grpc.stub.StreamObserver<com.yandex.query.YandexQueryProtos.GetResultDataResponse>) responseObserver);
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
              .addMethod(getCreateOperationMethod())
              .addMethod(getListOperationsMethod())
              .addMethod(getDescribeOperationMethod())
              .addMethod(getModifyOperationMethod())
              .addMethod(getDeleteOperationMethod())
              .addMethod(getControlOperationMethod())
              .addMethod(getGetResultDataMethod())
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
