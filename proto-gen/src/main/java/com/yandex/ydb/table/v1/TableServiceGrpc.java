package tech.ydb.table.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_table_v1.proto")
public final class TableServiceGrpc {

  private TableServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Table.V1.TableService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateSessionRequest,
      tech.ydb.table.YdbTable.CreateSessionResponse> getCreateSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateSession",
      requestType = tech.ydb.table.YdbTable.CreateSessionRequest.class,
      responseType = tech.ydb.table.YdbTable.CreateSessionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateSessionRequest,
      tech.ydb.table.YdbTable.CreateSessionResponse> getCreateSessionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateSessionRequest, tech.ydb.table.YdbTable.CreateSessionResponse> getCreateSessionMethod;
    if ((getCreateSessionMethod = TableServiceGrpc.getCreateSessionMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getCreateSessionMethod = TableServiceGrpc.getCreateSessionMethod) == null) {
          TableServiceGrpc.getCreateSessionMethod = getCreateSessionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.CreateSessionRequest, tech.ydb.table.YdbTable.CreateSessionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateSession"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CreateSessionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CreateSessionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("CreateSession"))
              .build();
        }
      }
    }
    return getCreateSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DeleteSessionRequest,
      tech.ydb.table.YdbTable.DeleteSessionResponse> getDeleteSessionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteSession",
      requestType = tech.ydb.table.YdbTable.DeleteSessionRequest.class,
      responseType = tech.ydb.table.YdbTable.DeleteSessionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DeleteSessionRequest,
      tech.ydb.table.YdbTable.DeleteSessionResponse> getDeleteSessionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DeleteSessionRequest, tech.ydb.table.YdbTable.DeleteSessionResponse> getDeleteSessionMethod;
    if ((getDeleteSessionMethod = TableServiceGrpc.getDeleteSessionMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getDeleteSessionMethod = TableServiceGrpc.getDeleteSessionMethod) == null) {
          TableServiceGrpc.getDeleteSessionMethod = getDeleteSessionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.DeleteSessionRequest, tech.ydb.table.YdbTable.DeleteSessionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteSession"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DeleteSessionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DeleteSessionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("DeleteSession"))
              .build();
        }
      }
    }
    return getDeleteSessionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.KeepAliveRequest,
      tech.ydb.table.YdbTable.KeepAliveResponse> getKeepAliveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "KeepAlive",
      requestType = tech.ydb.table.YdbTable.KeepAliveRequest.class,
      responseType = tech.ydb.table.YdbTable.KeepAliveResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.KeepAliveRequest,
      tech.ydb.table.YdbTable.KeepAliveResponse> getKeepAliveMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.KeepAliveRequest, tech.ydb.table.YdbTable.KeepAliveResponse> getKeepAliveMethod;
    if ((getKeepAliveMethod = TableServiceGrpc.getKeepAliveMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getKeepAliveMethod = TableServiceGrpc.getKeepAliveMethod) == null) {
          TableServiceGrpc.getKeepAliveMethod = getKeepAliveMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.KeepAliveRequest, tech.ydb.table.YdbTable.KeepAliveResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "KeepAlive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.KeepAliveRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.KeepAliveResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("KeepAlive"))
              .build();
        }
      }
    }
    return getKeepAliveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateTableRequest,
      tech.ydb.table.YdbTable.CreateTableResponse> getCreateTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateTable",
      requestType = tech.ydb.table.YdbTable.CreateTableRequest.class,
      responseType = tech.ydb.table.YdbTable.CreateTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateTableRequest,
      tech.ydb.table.YdbTable.CreateTableResponse> getCreateTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CreateTableRequest, tech.ydb.table.YdbTable.CreateTableResponse> getCreateTableMethod;
    if ((getCreateTableMethod = TableServiceGrpc.getCreateTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getCreateTableMethod = TableServiceGrpc.getCreateTableMethod) == null) {
          TableServiceGrpc.getCreateTableMethod = getCreateTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.CreateTableRequest, tech.ydb.table.YdbTable.CreateTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CreateTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CreateTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("CreateTable"))
              .build();
        }
      }
    }
    return getCreateTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DropTableRequest,
      tech.ydb.table.YdbTable.DropTableResponse> getDropTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DropTable",
      requestType = tech.ydb.table.YdbTable.DropTableRequest.class,
      responseType = tech.ydb.table.YdbTable.DropTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DropTableRequest,
      tech.ydb.table.YdbTable.DropTableResponse> getDropTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DropTableRequest, tech.ydb.table.YdbTable.DropTableResponse> getDropTableMethod;
    if ((getDropTableMethod = TableServiceGrpc.getDropTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getDropTableMethod = TableServiceGrpc.getDropTableMethod) == null) {
          TableServiceGrpc.getDropTableMethod = getDropTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.DropTableRequest, tech.ydb.table.YdbTable.DropTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DropTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DropTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DropTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("DropTable"))
              .build();
        }
      }
    }
    return getDropTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.AlterTableRequest,
      tech.ydb.table.YdbTable.AlterTableResponse> getAlterTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AlterTable",
      requestType = tech.ydb.table.YdbTable.AlterTableRequest.class,
      responseType = tech.ydb.table.YdbTable.AlterTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.AlterTableRequest,
      tech.ydb.table.YdbTable.AlterTableResponse> getAlterTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.AlterTableRequest, tech.ydb.table.YdbTable.AlterTableResponse> getAlterTableMethod;
    if ((getAlterTableMethod = TableServiceGrpc.getAlterTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getAlterTableMethod = TableServiceGrpc.getAlterTableMethod) == null) {
          TableServiceGrpc.getAlterTableMethod = getAlterTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.AlterTableRequest, tech.ydb.table.YdbTable.AlterTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AlterTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.AlterTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.AlterTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("AlterTable"))
              .build();
        }
      }
    }
    return getAlterTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTableRequest,
      tech.ydb.table.YdbTable.CopyTableResponse> getCopyTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CopyTable",
      requestType = tech.ydb.table.YdbTable.CopyTableRequest.class,
      responseType = tech.ydb.table.YdbTable.CopyTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTableRequest,
      tech.ydb.table.YdbTable.CopyTableResponse> getCopyTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTableRequest, tech.ydb.table.YdbTable.CopyTableResponse> getCopyTableMethod;
    if ((getCopyTableMethod = TableServiceGrpc.getCopyTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getCopyTableMethod = TableServiceGrpc.getCopyTableMethod) == null) {
          TableServiceGrpc.getCopyTableMethod = getCopyTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.CopyTableRequest, tech.ydb.table.YdbTable.CopyTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CopyTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CopyTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CopyTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("CopyTable"))
              .build();
        }
      }
    }
    return getCopyTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTablesRequest,
      tech.ydb.table.YdbTable.CopyTablesResponse> getCopyTablesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CopyTables",
      requestType = tech.ydb.table.YdbTable.CopyTablesRequest.class,
      responseType = tech.ydb.table.YdbTable.CopyTablesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTablesRequest,
      tech.ydb.table.YdbTable.CopyTablesResponse> getCopyTablesMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CopyTablesRequest, tech.ydb.table.YdbTable.CopyTablesResponse> getCopyTablesMethod;
    if ((getCopyTablesMethod = TableServiceGrpc.getCopyTablesMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getCopyTablesMethod = TableServiceGrpc.getCopyTablesMethod) == null) {
          TableServiceGrpc.getCopyTablesMethod = getCopyTablesMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.CopyTablesRequest, tech.ydb.table.YdbTable.CopyTablesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CopyTables"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CopyTablesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CopyTablesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("CopyTables"))
              .build();
        }
      }
    }
    return getCopyTablesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableRequest,
      tech.ydb.table.YdbTable.DescribeTableResponse> getDescribeTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeTable",
      requestType = tech.ydb.table.YdbTable.DescribeTableRequest.class,
      responseType = tech.ydb.table.YdbTable.DescribeTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableRequest,
      tech.ydb.table.YdbTable.DescribeTableResponse> getDescribeTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableRequest, tech.ydb.table.YdbTable.DescribeTableResponse> getDescribeTableMethod;
    if ((getDescribeTableMethod = TableServiceGrpc.getDescribeTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getDescribeTableMethod = TableServiceGrpc.getDescribeTableMethod) == null) {
          TableServiceGrpc.getDescribeTableMethod = getDescribeTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.DescribeTableRequest, tech.ydb.table.YdbTable.DescribeTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DescribeTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DescribeTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("DescribeTable"))
              .build();
        }
      }
    }
    return getDescribeTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExplainDataQueryRequest,
      tech.ydb.table.YdbTable.ExplainDataQueryResponse> getExplainDataQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExplainDataQuery",
      requestType = tech.ydb.table.YdbTable.ExplainDataQueryRequest.class,
      responseType = tech.ydb.table.YdbTable.ExplainDataQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExplainDataQueryRequest,
      tech.ydb.table.YdbTable.ExplainDataQueryResponse> getExplainDataQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExplainDataQueryRequest, tech.ydb.table.YdbTable.ExplainDataQueryResponse> getExplainDataQueryMethod;
    if ((getExplainDataQueryMethod = TableServiceGrpc.getExplainDataQueryMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getExplainDataQueryMethod = TableServiceGrpc.getExplainDataQueryMethod) == null) {
          TableServiceGrpc.getExplainDataQueryMethod = getExplainDataQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.ExplainDataQueryRequest, tech.ydb.table.YdbTable.ExplainDataQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExplainDataQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExplainDataQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExplainDataQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("ExplainDataQuery"))
              .build();
        }
      }
    }
    return getExplainDataQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.PrepareDataQueryRequest,
      tech.ydb.table.YdbTable.PrepareDataQueryResponse> getPrepareDataQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PrepareDataQuery",
      requestType = tech.ydb.table.YdbTable.PrepareDataQueryRequest.class,
      responseType = tech.ydb.table.YdbTable.PrepareDataQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.PrepareDataQueryRequest,
      tech.ydb.table.YdbTable.PrepareDataQueryResponse> getPrepareDataQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.PrepareDataQueryRequest, tech.ydb.table.YdbTable.PrepareDataQueryResponse> getPrepareDataQueryMethod;
    if ((getPrepareDataQueryMethod = TableServiceGrpc.getPrepareDataQueryMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getPrepareDataQueryMethod = TableServiceGrpc.getPrepareDataQueryMethod) == null) {
          TableServiceGrpc.getPrepareDataQueryMethod = getPrepareDataQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.PrepareDataQueryRequest, tech.ydb.table.YdbTable.PrepareDataQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PrepareDataQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.PrepareDataQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.PrepareDataQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("PrepareDataQuery"))
              .build();
        }
      }
    }
    return getPrepareDataQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteDataQueryRequest,
      tech.ydb.table.YdbTable.ExecuteDataQueryResponse> getExecuteDataQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteDataQuery",
      requestType = tech.ydb.table.YdbTable.ExecuteDataQueryRequest.class,
      responseType = tech.ydb.table.YdbTable.ExecuteDataQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteDataQueryRequest,
      tech.ydb.table.YdbTable.ExecuteDataQueryResponse> getExecuteDataQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteDataQueryRequest, tech.ydb.table.YdbTable.ExecuteDataQueryResponse> getExecuteDataQueryMethod;
    if ((getExecuteDataQueryMethod = TableServiceGrpc.getExecuteDataQueryMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getExecuteDataQueryMethod = TableServiceGrpc.getExecuteDataQueryMethod) == null) {
          TableServiceGrpc.getExecuteDataQueryMethod = getExecuteDataQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.ExecuteDataQueryRequest, tech.ydb.table.YdbTable.ExecuteDataQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteDataQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteDataQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteDataQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("ExecuteDataQuery"))
              .build();
        }
      }
    }
    return getExecuteDataQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest,
      tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> getExecuteSchemeQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ExecuteSchemeQuery",
      requestType = tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest.class,
      responseType = tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest,
      tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> getExecuteSchemeQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest, tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> getExecuteSchemeQueryMethod;
    if ((getExecuteSchemeQueryMethod = TableServiceGrpc.getExecuteSchemeQueryMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getExecuteSchemeQueryMethod = TableServiceGrpc.getExecuteSchemeQueryMethod) == null) {
          TableServiceGrpc.getExecuteSchemeQueryMethod = getExecuteSchemeQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest, tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ExecuteSchemeQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("ExecuteSchemeQuery"))
              .build();
        }
      }
    }
    return getExecuteSchemeQueryMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BeginTransactionRequest,
      tech.ydb.table.YdbTable.BeginTransactionResponse> getBeginTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BeginTransaction",
      requestType = tech.ydb.table.YdbTable.BeginTransactionRequest.class,
      responseType = tech.ydb.table.YdbTable.BeginTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BeginTransactionRequest,
      tech.ydb.table.YdbTable.BeginTransactionResponse> getBeginTransactionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BeginTransactionRequest, tech.ydb.table.YdbTable.BeginTransactionResponse> getBeginTransactionMethod;
    if ((getBeginTransactionMethod = TableServiceGrpc.getBeginTransactionMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getBeginTransactionMethod = TableServiceGrpc.getBeginTransactionMethod) == null) {
          TableServiceGrpc.getBeginTransactionMethod = getBeginTransactionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.BeginTransactionRequest, tech.ydb.table.YdbTable.BeginTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BeginTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.BeginTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.BeginTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("BeginTransaction"))
              .build();
        }
      }
    }
    return getBeginTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CommitTransactionRequest,
      tech.ydb.table.YdbTable.CommitTransactionResponse> getCommitTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CommitTransaction",
      requestType = tech.ydb.table.YdbTable.CommitTransactionRequest.class,
      responseType = tech.ydb.table.YdbTable.CommitTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CommitTransactionRequest,
      tech.ydb.table.YdbTable.CommitTransactionResponse> getCommitTransactionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.CommitTransactionRequest, tech.ydb.table.YdbTable.CommitTransactionResponse> getCommitTransactionMethod;
    if ((getCommitTransactionMethod = TableServiceGrpc.getCommitTransactionMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getCommitTransactionMethod = TableServiceGrpc.getCommitTransactionMethod) == null) {
          TableServiceGrpc.getCommitTransactionMethod = getCommitTransactionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.CommitTransactionRequest, tech.ydb.table.YdbTable.CommitTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CommitTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CommitTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.CommitTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("CommitTransaction"))
              .build();
        }
      }
    }
    return getCommitTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.RollbackTransactionRequest,
      tech.ydb.table.YdbTable.RollbackTransactionResponse> getRollbackTransactionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RollbackTransaction",
      requestType = tech.ydb.table.YdbTable.RollbackTransactionRequest.class,
      responseType = tech.ydb.table.YdbTable.RollbackTransactionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.RollbackTransactionRequest,
      tech.ydb.table.YdbTable.RollbackTransactionResponse> getRollbackTransactionMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.RollbackTransactionRequest, tech.ydb.table.YdbTable.RollbackTransactionResponse> getRollbackTransactionMethod;
    if ((getRollbackTransactionMethod = TableServiceGrpc.getRollbackTransactionMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getRollbackTransactionMethod = TableServiceGrpc.getRollbackTransactionMethod) == null) {
          TableServiceGrpc.getRollbackTransactionMethod = getRollbackTransactionMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.RollbackTransactionRequest, tech.ydb.table.YdbTable.RollbackTransactionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RollbackTransaction"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.RollbackTransactionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.RollbackTransactionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("RollbackTransaction"))
              .build();
        }
      }
    }
    return getRollbackTransactionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableOptionsRequest,
      tech.ydb.table.YdbTable.DescribeTableOptionsResponse> getDescribeTableOptionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DescribeTableOptions",
      requestType = tech.ydb.table.YdbTable.DescribeTableOptionsRequest.class,
      responseType = tech.ydb.table.YdbTable.DescribeTableOptionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableOptionsRequest,
      tech.ydb.table.YdbTable.DescribeTableOptionsResponse> getDescribeTableOptionsMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.DescribeTableOptionsRequest, tech.ydb.table.YdbTable.DescribeTableOptionsResponse> getDescribeTableOptionsMethod;
    if ((getDescribeTableOptionsMethod = TableServiceGrpc.getDescribeTableOptionsMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getDescribeTableOptionsMethod = TableServiceGrpc.getDescribeTableOptionsMethod) == null) {
          TableServiceGrpc.getDescribeTableOptionsMethod = getDescribeTableOptionsMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.DescribeTableOptionsRequest, tech.ydb.table.YdbTable.DescribeTableOptionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DescribeTableOptions"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DescribeTableOptionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.DescribeTableOptionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("DescribeTableOptions"))
              .build();
        }
      }
    }
    return getDescribeTableOptionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ReadTableRequest,
      tech.ydb.table.YdbTable.ReadTableResponse> getStreamReadTableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamReadTable",
      requestType = tech.ydb.table.YdbTable.ReadTableRequest.class,
      responseType = tech.ydb.table.YdbTable.ReadTableResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ReadTableRequest,
      tech.ydb.table.YdbTable.ReadTableResponse> getStreamReadTableMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ReadTableRequest, tech.ydb.table.YdbTable.ReadTableResponse> getStreamReadTableMethod;
    if ((getStreamReadTableMethod = TableServiceGrpc.getStreamReadTableMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getStreamReadTableMethod = TableServiceGrpc.getStreamReadTableMethod) == null) {
          TableServiceGrpc.getStreamReadTableMethod = getStreamReadTableMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.ReadTableRequest, tech.ydb.table.YdbTable.ReadTableResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamReadTable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ReadTableRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ReadTableResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("StreamReadTable"))
              .build();
        }
      }
    }
    return getStreamReadTableMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BulkUpsertRequest,
      tech.ydb.table.YdbTable.BulkUpsertResponse> getBulkUpsertMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BulkUpsert",
      requestType = tech.ydb.table.YdbTable.BulkUpsertRequest.class,
      responseType = tech.ydb.table.YdbTable.BulkUpsertResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BulkUpsertRequest,
      tech.ydb.table.YdbTable.BulkUpsertResponse> getBulkUpsertMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.BulkUpsertRequest, tech.ydb.table.YdbTable.BulkUpsertResponse> getBulkUpsertMethod;
    if ((getBulkUpsertMethod = TableServiceGrpc.getBulkUpsertMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getBulkUpsertMethod = TableServiceGrpc.getBulkUpsertMethod) == null) {
          TableServiceGrpc.getBulkUpsertMethod = getBulkUpsertMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.BulkUpsertRequest, tech.ydb.table.YdbTable.BulkUpsertResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BulkUpsert"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.BulkUpsertRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.BulkUpsertResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("BulkUpsert"))
              .build();
        }
      }
    }
    return getBulkUpsertMethod;
  }

  private static volatile io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteScanQueryRequest,
      tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> getStreamExecuteScanQueryMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamExecuteScanQuery",
      requestType = tech.ydb.table.YdbTable.ExecuteScanQueryRequest.class,
      responseType = tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteScanQueryRequest,
      tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> getStreamExecuteScanQueryMethod() {
    io.grpc.MethodDescriptor<tech.ydb.table.YdbTable.ExecuteScanQueryRequest, tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> getStreamExecuteScanQueryMethod;
    if ((getStreamExecuteScanQueryMethod = TableServiceGrpc.getStreamExecuteScanQueryMethod) == null) {
      synchronized (TableServiceGrpc.class) {
        if ((getStreamExecuteScanQueryMethod = TableServiceGrpc.getStreamExecuteScanQueryMethod) == null) {
          TableServiceGrpc.getStreamExecuteScanQueryMethod = getStreamExecuteScanQueryMethod =
              io.grpc.MethodDescriptor.<tech.ydb.table.YdbTable.ExecuteScanQueryRequest, tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamExecuteScanQuery"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteScanQueryRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TableServiceMethodDescriptorSupplier("StreamExecuteScanQuery"))
              .build();
        }
      }
    }
    return getStreamExecuteScanQueryMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TableServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TableServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TableServiceStub>() {
        @java.lang.Override
        public TableServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TableServiceStub(channel, callOptions);
        }
      };
    return TableServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TableServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TableServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TableServiceBlockingStub>() {
        @java.lang.Override
        public TableServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TableServiceBlockingStub(channel, callOptions);
        }
      };
    return TableServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TableServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TableServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TableServiceFutureStub>() {
        @java.lang.Override
        public TableServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TableServiceFutureStub(channel, callOptions);
        }
      };
    return TableServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class TableServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Create new session. Implicit session creation is forbidden,
     * so user must create new session before execute any query,
     * otherwise BAD_SESSION status will be returned.
     * Simultaneous execution of requests are forbiden.
     * Sessions are volatile, can be invalidated by server, for example in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * </pre>
     */
    public void createSession(tech.ydb.table.YdbTable.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateSessionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateSessionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public void deleteSession(tech.ydb.table.YdbTable.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DeleteSessionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeleteSessionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public void keepAlive(tech.ydb.table.YdbTable.KeepAliveRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.KeepAliveResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getKeepAliveMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public void createTable(tech.ydb.table.YdbTable.CreateTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreateTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public void dropTable(tech.ydb.table.YdbTable.DropTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DropTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDropTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public void alterTable(tech.ydb.table.YdbTable.AlterTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.AlterTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAlterTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public void copyTable(tech.ydb.table.YdbTable.CopyTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCopyTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public void copyTables(tech.ydb.table.YdbTable.CopyTablesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTablesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCopyTablesMethod(), responseObserver);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public void describeTable(tech.ydb.table.YdbTable.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void explainDataQuery(tech.ydb.table.YdbTable.ExplainDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExplainDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExplainDataQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void prepareDataQuery(tech.ydb.table.YdbTable.PrepareDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.PrepareDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPrepareDataQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeDataQuery(tech.ydb.table.YdbTable.ExecuteDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteDataQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeSchemeQuery(tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getExecuteSchemeQueryMethod(), responseObserver);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public void beginTransaction(tech.ydb.table.YdbTable.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BeginTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getBeginTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public void commitTransaction(tech.ydb.table.YdbTable.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CommitTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCommitTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public void rollbackTransaction(tech.ydb.table.YdbTable.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.RollbackTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRollbackTransactionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public void describeTableOptions(tech.ydb.table.YdbTable.DescribeTableOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableOptionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDescribeTableOptionsMethod(), responseObserver);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public void streamReadTable(tech.ydb.table.YdbTable.ReadTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ReadTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getStreamReadTableMethod(), responseObserver);
    }

    /**
     * <pre>
     * Upserts a batch of rows non-transactionally.
     * Returns success only when all rows were successfully upserted. In case of an error some rows might
     * be upserted and some might not.
     * </pre>
     */
    public void bulkUpsert(tech.ydb.table.YdbTable.BulkUpsertRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BulkUpsertResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getBulkUpsertMethod(), responseObserver);
    }

    /**
     * <pre>
     * Executes scan query with streaming result.
     * </pre>
     */
    public void streamExecuteScanQuery(tech.ydb.table.YdbTable.ExecuteScanQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getStreamExecuteScanQueryMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getCreateSessionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.CreateSessionRequest,
                tech.ydb.table.YdbTable.CreateSessionResponse>(
                  this, METHODID_CREATE_SESSION)))
          .addMethod(
            getDeleteSessionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.DeleteSessionRequest,
                tech.ydb.table.YdbTable.DeleteSessionResponse>(
                  this, METHODID_DELETE_SESSION)))
          .addMethod(
            getKeepAliveMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.KeepAliveRequest,
                tech.ydb.table.YdbTable.KeepAliveResponse>(
                  this, METHODID_KEEP_ALIVE)))
          .addMethod(
            getCreateTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.CreateTableRequest,
                tech.ydb.table.YdbTable.CreateTableResponse>(
                  this, METHODID_CREATE_TABLE)))
          .addMethod(
            getDropTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.DropTableRequest,
                tech.ydb.table.YdbTable.DropTableResponse>(
                  this, METHODID_DROP_TABLE)))
          .addMethod(
            getAlterTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.AlterTableRequest,
                tech.ydb.table.YdbTable.AlterTableResponse>(
                  this, METHODID_ALTER_TABLE)))
          .addMethod(
            getCopyTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.CopyTableRequest,
                tech.ydb.table.YdbTable.CopyTableResponse>(
                  this, METHODID_COPY_TABLE)))
          .addMethod(
            getCopyTablesMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.CopyTablesRequest,
                tech.ydb.table.YdbTable.CopyTablesResponse>(
                  this, METHODID_COPY_TABLES)))
          .addMethod(
            getDescribeTableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.DescribeTableRequest,
                tech.ydb.table.YdbTable.DescribeTableResponse>(
                  this, METHODID_DESCRIBE_TABLE)))
          .addMethod(
            getExplainDataQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.ExplainDataQueryRequest,
                tech.ydb.table.YdbTable.ExplainDataQueryResponse>(
                  this, METHODID_EXPLAIN_DATA_QUERY)))
          .addMethod(
            getPrepareDataQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.PrepareDataQueryRequest,
                tech.ydb.table.YdbTable.PrepareDataQueryResponse>(
                  this, METHODID_PREPARE_DATA_QUERY)))
          .addMethod(
            getExecuteDataQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.ExecuteDataQueryRequest,
                tech.ydb.table.YdbTable.ExecuteDataQueryResponse>(
                  this, METHODID_EXECUTE_DATA_QUERY)))
          .addMethod(
            getExecuteSchemeQueryMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest,
                tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse>(
                  this, METHODID_EXECUTE_SCHEME_QUERY)))
          .addMethod(
            getBeginTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.BeginTransactionRequest,
                tech.ydb.table.YdbTable.BeginTransactionResponse>(
                  this, METHODID_BEGIN_TRANSACTION)))
          .addMethod(
            getCommitTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.CommitTransactionRequest,
                tech.ydb.table.YdbTable.CommitTransactionResponse>(
                  this, METHODID_COMMIT_TRANSACTION)))
          .addMethod(
            getRollbackTransactionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.RollbackTransactionRequest,
                tech.ydb.table.YdbTable.RollbackTransactionResponse>(
                  this, METHODID_ROLLBACK_TRANSACTION)))
          .addMethod(
            getDescribeTableOptionsMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.DescribeTableOptionsRequest,
                tech.ydb.table.YdbTable.DescribeTableOptionsResponse>(
                  this, METHODID_DESCRIBE_TABLE_OPTIONS)))
          .addMethod(
            getStreamReadTableMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.ReadTableRequest,
                tech.ydb.table.YdbTable.ReadTableResponse>(
                  this, METHODID_STREAM_READ_TABLE)))
          .addMethod(
            getBulkUpsertMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.BulkUpsertRequest,
                tech.ydb.table.YdbTable.BulkUpsertResponse>(
                  this, METHODID_BULK_UPSERT)))
          .addMethod(
            getStreamExecuteScanQueryMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                tech.ydb.table.YdbTable.ExecuteScanQueryRequest,
                tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse>(
                  this, METHODID_STREAM_EXECUTE_SCAN_QUERY)))
          .build();
    }
  }

  /**
   */
  public static final class TableServiceStub extends io.grpc.stub.AbstractAsyncStub<TableServiceStub> {
    private TableServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TableServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create new session. Implicit session creation is forbidden,
     * so user must create new session before execute any query,
     * otherwise BAD_SESSION status will be returned.
     * Simultaneous execution of requests are forbiden.
     * Sessions are volatile, can be invalidated by server, for example in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * </pre>
     */
    public void createSession(tech.ydb.table.YdbTable.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateSessionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateSessionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public void deleteSession(tech.ydb.table.YdbTable.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DeleteSessionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeleteSessionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public void keepAlive(tech.ydb.table.YdbTable.KeepAliveRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.KeepAliveResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getKeepAliveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public void createTable(tech.ydb.table.YdbTable.CreateTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCreateTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public void dropTable(tech.ydb.table.YdbTable.DropTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DropTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDropTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public void alterTable(tech.ydb.table.YdbTable.AlterTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.AlterTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAlterTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public void copyTable(tech.ydb.table.YdbTable.CopyTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCopyTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public void copyTables(tech.ydb.table.YdbTable.CopyTablesRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTablesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCopyTablesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public void describeTable(tech.ydb.table.YdbTable.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void explainDataQuery(tech.ydb.table.YdbTable.ExplainDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExplainDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExplainDataQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void prepareDataQuery(tech.ydb.table.YdbTable.PrepareDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.PrepareDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPrepareDataQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeDataQuery(tech.ydb.table.YdbTable.ExecuteDataQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteDataQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeSchemeQuery(tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getExecuteSchemeQueryMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public void beginTransaction(tech.ydb.table.YdbTable.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BeginTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBeginTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public void commitTransaction(tech.ydb.table.YdbTable.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CommitTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getCommitTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public void rollbackTransaction(tech.ydb.table.YdbTable.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.RollbackTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRollbackTransactionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public void describeTableOptions(tech.ydb.table.YdbTable.DescribeTableOptionsRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableOptionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDescribeTableOptionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public void streamReadTable(tech.ydb.table.YdbTable.ReadTableRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ReadTableResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getStreamReadTableMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Upserts a batch of rows non-transactionally.
     * Returns success only when all rows were successfully upserted. In case of an error some rows might
     * be upserted and some might not.
     * </pre>
     */
    public void bulkUpsert(tech.ydb.table.YdbTable.BulkUpsertRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BulkUpsertResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getBulkUpsertMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Executes scan query with streaming result.
     * </pre>
     */
    public void streamExecuteScanQuery(tech.ydb.table.YdbTable.ExecuteScanQueryRequest request,
        io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getStreamExecuteScanQueryMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class TableServiceBlockingStub extends io.grpc.stub.AbstractBlockingStub<TableServiceBlockingStub> {
    private TableServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TableServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create new session. Implicit session creation is forbidden,
     * so user must create new session before execute any query,
     * otherwise BAD_SESSION status will be returned.
     * Simultaneous execution of requests are forbiden.
     * Sessions are volatile, can be invalidated by server, for example in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * </pre>
     */
    public tech.ydb.table.YdbTable.CreateSessionResponse createSession(tech.ydb.table.YdbTable.CreateSessionRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateSessionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public tech.ydb.table.YdbTable.DeleteSessionResponse deleteSession(tech.ydb.table.YdbTable.DeleteSessionRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeleteSessionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public tech.ydb.table.YdbTable.KeepAliveResponse keepAlive(tech.ydb.table.YdbTable.KeepAliveRequest request) {
      return blockingUnaryCall(
          getChannel(), getKeepAliveMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public tech.ydb.table.YdbTable.CreateTableResponse createTable(tech.ydb.table.YdbTable.CreateTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getCreateTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public tech.ydb.table.YdbTable.DropTableResponse dropTable(tech.ydb.table.YdbTable.DropTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getDropTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public tech.ydb.table.YdbTable.AlterTableResponse alterTable(tech.ydb.table.YdbTable.AlterTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getAlterTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public tech.ydb.table.YdbTable.CopyTableResponse copyTable(tech.ydb.table.YdbTable.CopyTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getCopyTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public tech.ydb.table.YdbTable.CopyTablesResponse copyTables(tech.ydb.table.YdbTable.CopyTablesRequest request) {
      return blockingUnaryCall(
          getChannel(), getCopyTablesMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public tech.ydb.table.YdbTable.DescribeTableResponse describeTable(tech.ydb.table.YdbTable.DescribeTableRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public tech.ydb.table.YdbTable.ExplainDataQueryResponse explainDataQuery(tech.ydb.table.YdbTable.ExplainDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getExplainDataQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public tech.ydb.table.YdbTable.PrepareDataQueryResponse prepareDataQuery(tech.ydb.table.YdbTable.PrepareDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getPrepareDataQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public tech.ydb.table.YdbTable.ExecuteDataQueryResponse executeDataQuery(tech.ydb.table.YdbTable.ExecuteDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteDataQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse executeSchemeQuery(tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), getExecuteSchemeQueryMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public tech.ydb.table.YdbTable.BeginTransactionResponse beginTransaction(tech.ydb.table.YdbTable.BeginTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getBeginTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public tech.ydb.table.YdbTable.CommitTransactionResponse commitTransaction(tech.ydb.table.YdbTable.CommitTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getCommitTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public tech.ydb.table.YdbTable.RollbackTransactionResponse rollbackTransaction(tech.ydb.table.YdbTable.RollbackTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), getRollbackTransactionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public tech.ydb.table.YdbTable.DescribeTableOptionsResponse describeTableOptions(tech.ydb.table.YdbTable.DescribeTableOptionsRequest request) {
      return blockingUnaryCall(
          getChannel(), getDescribeTableOptionsMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public java.util.Iterator<tech.ydb.table.YdbTable.ReadTableResponse> streamReadTable(
        tech.ydb.table.YdbTable.ReadTableRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getStreamReadTableMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Upserts a batch of rows non-transactionally.
     * Returns success only when all rows were successfully upserted. In case of an error some rows might
     * be upserted and some might not.
     * </pre>
     */
    public tech.ydb.table.YdbTable.BulkUpsertResponse bulkUpsert(tech.ydb.table.YdbTable.BulkUpsertRequest request) {
      return blockingUnaryCall(
          getChannel(), getBulkUpsertMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Executes scan query with streaming result.
     * </pre>
     */
    public java.util.Iterator<tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse> streamExecuteScanQuery(
        tech.ydb.table.YdbTable.ExecuteScanQueryRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getStreamExecuteScanQueryMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class TableServiceFutureStub extends io.grpc.stub.AbstractFutureStub<TableServiceFutureStub> {
    private TableServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TableServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Create new session. Implicit session creation is forbidden,
     * so user must create new session before execute any query,
     * otherwise BAD_SESSION status will be returned.
     * Simultaneous execution of requests are forbiden.
     * Sessions are volatile, can be invalidated by server, for example in case
     * of fatal errors. All requests with this session will fail with BAD_SESSION status.
     * So, client must be able to handle BAD_SESSION status.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.CreateSessionResponse> createSession(
        tech.ydb.table.YdbTable.CreateSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateSessionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.DeleteSessionResponse> deleteSession(
        tech.ydb.table.YdbTable.DeleteSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeleteSessionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.KeepAliveResponse> keepAlive(
        tech.ydb.table.YdbTable.KeepAliveRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getKeepAliveMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.CreateTableResponse> createTable(
        tech.ydb.table.YdbTable.CreateTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCreateTableMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.DropTableResponse> dropTable(
        tech.ydb.table.YdbTable.DropTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDropTableMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.AlterTableResponse> alterTable(
        tech.ydb.table.YdbTable.AlterTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getAlterTableMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.CopyTableResponse> copyTable(
        tech.ydb.table.YdbTable.CopyTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCopyTableMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.CopyTablesResponse> copyTables(
        tech.ydb.table.YdbTable.CopyTablesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCopyTablesMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.DescribeTableResponse> describeTable(
        tech.ydb.table.YdbTable.DescribeTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeTableMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.ExplainDataQueryResponse> explainDataQuery(
        tech.ydb.table.YdbTable.ExplainDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExplainDataQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.PrepareDataQueryResponse> prepareDataQuery(
        tech.ydb.table.YdbTable.PrepareDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPrepareDataQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.ExecuteDataQueryResponse> executeDataQuery(
        tech.ydb.table.YdbTable.ExecuteDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteDataQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse> executeSchemeQuery(
        tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getExecuteSchemeQueryMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.BeginTransactionResponse> beginTransaction(
        tech.ydb.table.YdbTable.BeginTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBeginTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.CommitTransactionResponse> commitTransaction(
        tech.ydb.table.YdbTable.CommitTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getCommitTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.RollbackTransactionResponse> rollbackTransaction(
        tech.ydb.table.YdbTable.RollbackTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRollbackTransactionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.DescribeTableOptionsResponse> describeTableOptions(
        tech.ydb.table.YdbTable.DescribeTableOptionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDescribeTableOptionsMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Upserts a batch of rows non-transactionally.
     * Returns success only when all rows were successfully upserted. In case of an error some rows might
     * be upserted and some might not.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<tech.ydb.table.YdbTable.BulkUpsertResponse> bulkUpsert(
        tech.ydb.table.YdbTable.BulkUpsertRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getBulkUpsertMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE_SESSION = 0;
  private static final int METHODID_DELETE_SESSION = 1;
  private static final int METHODID_KEEP_ALIVE = 2;
  private static final int METHODID_CREATE_TABLE = 3;
  private static final int METHODID_DROP_TABLE = 4;
  private static final int METHODID_ALTER_TABLE = 5;
  private static final int METHODID_COPY_TABLE = 6;
  private static final int METHODID_COPY_TABLES = 7;
  private static final int METHODID_DESCRIBE_TABLE = 8;
  private static final int METHODID_EXPLAIN_DATA_QUERY = 9;
  private static final int METHODID_PREPARE_DATA_QUERY = 10;
  private static final int METHODID_EXECUTE_DATA_QUERY = 11;
  private static final int METHODID_EXECUTE_SCHEME_QUERY = 12;
  private static final int METHODID_BEGIN_TRANSACTION = 13;
  private static final int METHODID_COMMIT_TRANSACTION = 14;
  private static final int METHODID_ROLLBACK_TRANSACTION = 15;
  private static final int METHODID_DESCRIBE_TABLE_OPTIONS = 16;
  private static final int METHODID_STREAM_READ_TABLE = 17;
  private static final int METHODID_BULK_UPSERT = 18;
  private static final int METHODID_STREAM_EXECUTE_SCAN_QUERY = 19;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TableServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(TableServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE_SESSION:
          serviceImpl.createSession((tech.ydb.table.YdbTable.CreateSessionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateSessionResponse>) responseObserver);
          break;
        case METHODID_DELETE_SESSION:
          serviceImpl.deleteSession((tech.ydb.table.YdbTable.DeleteSessionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DeleteSessionResponse>) responseObserver);
          break;
        case METHODID_KEEP_ALIVE:
          serviceImpl.keepAlive((tech.ydb.table.YdbTable.KeepAliveRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.KeepAliveResponse>) responseObserver);
          break;
        case METHODID_CREATE_TABLE:
          serviceImpl.createTable((tech.ydb.table.YdbTable.CreateTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CreateTableResponse>) responseObserver);
          break;
        case METHODID_DROP_TABLE:
          serviceImpl.dropTable((tech.ydb.table.YdbTable.DropTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DropTableResponse>) responseObserver);
          break;
        case METHODID_ALTER_TABLE:
          serviceImpl.alterTable((tech.ydb.table.YdbTable.AlterTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.AlterTableResponse>) responseObserver);
          break;
        case METHODID_COPY_TABLE:
          serviceImpl.copyTable((tech.ydb.table.YdbTable.CopyTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTableResponse>) responseObserver);
          break;
        case METHODID_COPY_TABLES:
          serviceImpl.copyTables((tech.ydb.table.YdbTable.CopyTablesRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CopyTablesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TABLE:
          serviceImpl.describeTable((tech.ydb.table.YdbTable.DescribeTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableResponse>) responseObserver);
          break;
        case METHODID_EXPLAIN_DATA_QUERY:
          serviceImpl.explainDataQuery((tech.ydb.table.YdbTable.ExplainDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExplainDataQueryResponse>) responseObserver);
          break;
        case METHODID_PREPARE_DATA_QUERY:
          serviceImpl.prepareDataQuery((tech.ydb.table.YdbTable.PrepareDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.PrepareDataQueryResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_DATA_QUERY:
          serviceImpl.executeDataQuery((tech.ydb.table.YdbTable.ExecuteDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteDataQueryResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_SCHEME_QUERY:
          serviceImpl.executeSchemeQuery((tech.ydb.table.YdbTable.ExecuteSchemeQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteSchemeQueryResponse>) responseObserver);
          break;
        case METHODID_BEGIN_TRANSACTION:
          serviceImpl.beginTransaction((tech.ydb.table.YdbTable.BeginTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BeginTransactionResponse>) responseObserver);
          break;
        case METHODID_COMMIT_TRANSACTION:
          serviceImpl.commitTransaction((tech.ydb.table.YdbTable.CommitTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.CommitTransactionResponse>) responseObserver);
          break;
        case METHODID_ROLLBACK_TRANSACTION:
          serviceImpl.rollbackTransaction((tech.ydb.table.YdbTable.RollbackTransactionRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.RollbackTransactionResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TABLE_OPTIONS:
          serviceImpl.describeTableOptions((tech.ydb.table.YdbTable.DescribeTableOptionsRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.DescribeTableOptionsResponse>) responseObserver);
          break;
        case METHODID_STREAM_READ_TABLE:
          serviceImpl.streamReadTable((tech.ydb.table.YdbTable.ReadTableRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ReadTableResponse>) responseObserver);
          break;
        case METHODID_BULK_UPSERT:
          serviceImpl.bulkUpsert((tech.ydb.table.YdbTable.BulkUpsertRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.BulkUpsertResponse>) responseObserver);
          break;
        case METHODID_STREAM_EXECUTE_SCAN_QUERY:
          serviceImpl.streamExecuteScanQuery((tech.ydb.table.YdbTable.ExecuteScanQueryRequest) request,
              (io.grpc.stub.StreamObserver<tech.ydb.table.YdbTable.ExecuteScanQueryPartialResponse>) responseObserver);
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

  private static abstract class TableServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TableServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return tech.ydb.table.v1.YdbTableV1.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TableService");
    }
  }

  private static final class TableServiceFileDescriptorSupplier
      extends TableServiceBaseDescriptorSupplier {
    TableServiceFileDescriptorSupplier() {}
  }

  private static final class TableServiceMethodDescriptorSupplier
      extends TableServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    TableServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (TableServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TableServiceFileDescriptorSupplier())
              .addMethod(getCreateSessionMethod())
              .addMethod(getDeleteSessionMethod())
              .addMethod(getKeepAliveMethod())
              .addMethod(getCreateTableMethod())
              .addMethod(getDropTableMethod())
              .addMethod(getAlterTableMethod())
              .addMethod(getCopyTableMethod())
              .addMethod(getCopyTablesMethod())
              .addMethod(getDescribeTableMethod())
              .addMethod(getExplainDataQueryMethod())
              .addMethod(getPrepareDataQueryMethod())
              .addMethod(getExecuteDataQueryMethod())
              .addMethod(getExecuteSchemeQueryMethod())
              .addMethod(getBeginTransactionMethod())
              .addMethod(getCommitTransactionMethod())
              .addMethod(getRollbackTransactionMethod())
              .addMethod(getDescribeTableOptionsMethod())
              .addMethod(getStreamReadTableMethod())
              .addMethod(getBulkUpsertMethod())
              .addMethod(getStreamExecuteScanQueryMethod())
              .build();
        }
      }
    }
    return result;
  }
}
