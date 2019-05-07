package ru.yandex.ydb.table.v1;

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
    comments = "Source: kikimr/public/api/grpc/ydb_table_v1.proto")
public final class TableServiceGrpc {

  private TableServiceGrpc() {}

  public static final String SERVICE_NAME = "Ydb.Table.V1.TableService";

  // Static method descriptors that strictly reflect the proto.
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.CreateSessionRequest,
      ru.yandex.ydb.table.YdbTable.CreateSessionResponse> METHOD_CREATE_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "CreateSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CreateSessionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CreateSessionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.DeleteSessionRequest,
      ru.yandex.ydb.table.YdbTable.DeleteSessionResponse> METHOD_DELETE_SESSION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "DeleteSession"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DeleteSessionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DeleteSessionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.KeepAliveRequest,
      ru.yandex.ydb.table.YdbTable.KeepAliveResponse> METHOD_KEEP_ALIVE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "KeepAlive"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.KeepAliveRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.KeepAliveResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.CreateTableRequest,
      ru.yandex.ydb.table.YdbTable.CreateTableResponse> METHOD_CREATE_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "CreateTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CreateTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CreateTableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.DropTableRequest,
      ru.yandex.ydb.table.YdbTable.DropTableResponse> METHOD_DROP_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "DropTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DropTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DropTableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.AlterTableRequest,
      ru.yandex.ydb.table.YdbTable.AlterTableResponse> METHOD_ALTER_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "AlterTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.AlterTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.AlterTableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.CopyTableRequest,
      ru.yandex.ydb.table.YdbTable.CopyTableResponse> METHOD_COPY_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "CopyTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CopyTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CopyTableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.CopyTablesRequest,
      ru.yandex.ydb.table.YdbTable.CopyTablesResponse> METHOD_COPY_TABLES =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "CopyTables"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CopyTablesRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CopyTablesResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.DescribeTableRequest,
      ru.yandex.ydb.table.YdbTable.DescribeTableResponse> METHOD_DESCRIBE_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "DescribeTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DescribeTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DescribeTableResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest,
      ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse> METHOD_EXPLAIN_DATA_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "ExplainDataQuery"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest,
      ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse> METHOD_PREPARE_DATA_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "PrepareDataQuery"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest,
      ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse> METHOD_EXECUTE_DATA_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "ExecuteDataQuery"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest,
      ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse> METHOD_EXECUTE_SCHEME_QUERY =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "ExecuteSchemeQuery"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.BeginTransactionRequest,
      ru.yandex.ydb.table.YdbTable.BeginTransactionResponse> METHOD_BEGIN_TRANSACTION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "BeginTransaction"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.BeginTransactionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.BeginTransactionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.CommitTransactionRequest,
      ru.yandex.ydb.table.YdbTable.CommitTransactionResponse> METHOD_COMMIT_TRANSACTION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "CommitTransaction"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CommitTransactionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.CommitTransactionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest,
      ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse> METHOD_ROLLBACK_TRANSACTION =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "RollbackTransaction"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest,
      ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse> METHOD_DESCRIBE_TABLE_OPTIONS =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.UNARY,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "DescribeTableOptions"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse.getDefaultInstance()));
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  public static final io.grpc.MethodDescriptor<ru.yandex.ydb.table.YdbTable.ReadTableRequest,
      ru.yandex.ydb.table.YdbTable.ReadTableResponse> METHOD_STREAM_READ_TABLE =
      io.grpc.MethodDescriptor.create(
          io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
          generateFullMethodName(
              "Ydb.Table.V1.TableService", "StreamReadTable"),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ReadTableRequest.getDefaultInstance()),
          io.grpc.protobuf.ProtoUtils.marshaller(ru.yandex.ydb.table.YdbTable.ReadTableResponse.getDefaultInstance()));

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TableServiceStub newStub(io.grpc.Channel channel) {
    return new TableServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TableServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new TableServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
   */
  public static TableServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new TableServiceFutureStub(channel);
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
    public void createSession(ru.yandex.ydb.table.YdbTable.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateSessionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_SESSION, responseObserver);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public void deleteSession(ru.yandex.ydb.table.YdbTable.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DeleteSessionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DELETE_SESSION, responseObserver);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public void keepAlive(ru.yandex.ydb.table.YdbTable.KeepAliveRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.KeepAliveResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_KEEP_ALIVE, responseObserver);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public void createTable(ru.yandex.ydb.table.YdbTable.CreateTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_CREATE_TABLE, responseObserver);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public void dropTable(ru.yandex.ydb.table.YdbTable.DropTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DropTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DROP_TABLE, responseObserver);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public void alterTable(ru.yandex.ydb.table.YdbTable.AlterTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.AlterTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ALTER_TABLE, responseObserver);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public void copyTable(ru.yandex.ydb.table.YdbTable.CopyTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_COPY_TABLE, responseObserver);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public void copyTables(ru.yandex.ydb.table.YdbTable.CopyTablesRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTablesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_COPY_TABLES, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public void describeTable(ru.yandex.ydb.table.YdbTable.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_TABLE, responseObserver);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void explainDataQuery(ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXPLAIN_DATA_QUERY, responseObserver);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void prepareDataQuery(ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_PREPARE_DATA_QUERY, responseObserver);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeDataQuery(ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXECUTE_DATA_QUERY, responseObserver);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeSchemeQuery(ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_EXECUTE_SCHEME_QUERY, responseObserver);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public void beginTransaction(ru.yandex.ydb.table.YdbTable.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.BeginTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_BEGIN_TRANSACTION, responseObserver);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public void commitTransaction(ru.yandex.ydb.table.YdbTable.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CommitTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_COMMIT_TRANSACTION, responseObserver);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public void rollbackTransaction(ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_ROLLBACK_TRANSACTION, responseObserver);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public void describeTableOptions(ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_DESCRIBE_TABLE_OPTIONS, responseObserver);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public void streamReadTable(ru.yandex.ydb.table.YdbTable.ReadTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ReadTableResponse> responseObserver) {
      asyncUnimplementedUnaryCall(METHOD_STREAM_READ_TABLE, responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            METHOD_CREATE_SESSION,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.CreateSessionRequest,
                ru.yandex.ydb.table.YdbTable.CreateSessionResponse>(
                  this, METHODID_CREATE_SESSION)))
          .addMethod(
            METHOD_DELETE_SESSION,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.DeleteSessionRequest,
                ru.yandex.ydb.table.YdbTable.DeleteSessionResponse>(
                  this, METHODID_DELETE_SESSION)))
          .addMethod(
            METHOD_KEEP_ALIVE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.KeepAliveRequest,
                ru.yandex.ydb.table.YdbTable.KeepAliveResponse>(
                  this, METHODID_KEEP_ALIVE)))
          .addMethod(
            METHOD_CREATE_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.CreateTableRequest,
                ru.yandex.ydb.table.YdbTable.CreateTableResponse>(
                  this, METHODID_CREATE_TABLE)))
          .addMethod(
            METHOD_DROP_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.DropTableRequest,
                ru.yandex.ydb.table.YdbTable.DropTableResponse>(
                  this, METHODID_DROP_TABLE)))
          .addMethod(
            METHOD_ALTER_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.AlterTableRequest,
                ru.yandex.ydb.table.YdbTable.AlterTableResponse>(
                  this, METHODID_ALTER_TABLE)))
          .addMethod(
            METHOD_COPY_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.CopyTableRequest,
                ru.yandex.ydb.table.YdbTable.CopyTableResponse>(
                  this, METHODID_COPY_TABLE)))
          .addMethod(
            METHOD_COPY_TABLES,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.CopyTablesRequest,
                ru.yandex.ydb.table.YdbTable.CopyTablesResponse>(
                  this, METHODID_COPY_TABLES)))
          .addMethod(
            METHOD_DESCRIBE_TABLE,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.DescribeTableRequest,
                ru.yandex.ydb.table.YdbTable.DescribeTableResponse>(
                  this, METHODID_DESCRIBE_TABLE)))
          .addMethod(
            METHOD_EXPLAIN_DATA_QUERY,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest,
                ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse>(
                  this, METHODID_EXPLAIN_DATA_QUERY)))
          .addMethod(
            METHOD_PREPARE_DATA_QUERY,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest,
                ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse>(
                  this, METHODID_PREPARE_DATA_QUERY)))
          .addMethod(
            METHOD_EXECUTE_DATA_QUERY,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest,
                ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse>(
                  this, METHODID_EXECUTE_DATA_QUERY)))
          .addMethod(
            METHOD_EXECUTE_SCHEME_QUERY,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest,
                ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse>(
                  this, METHODID_EXECUTE_SCHEME_QUERY)))
          .addMethod(
            METHOD_BEGIN_TRANSACTION,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.BeginTransactionRequest,
                ru.yandex.ydb.table.YdbTable.BeginTransactionResponse>(
                  this, METHODID_BEGIN_TRANSACTION)))
          .addMethod(
            METHOD_COMMIT_TRANSACTION,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.CommitTransactionRequest,
                ru.yandex.ydb.table.YdbTable.CommitTransactionResponse>(
                  this, METHODID_COMMIT_TRANSACTION)))
          .addMethod(
            METHOD_ROLLBACK_TRANSACTION,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest,
                ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse>(
                  this, METHODID_ROLLBACK_TRANSACTION)))
          .addMethod(
            METHOD_DESCRIBE_TABLE_OPTIONS,
            asyncUnaryCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest,
                ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse>(
                  this, METHODID_DESCRIBE_TABLE_OPTIONS)))
          .addMethod(
            METHOD_STREAM_READ_TABLE,
            asyncServerStreamingCall(
              new MethodHandlers<
                ru.yandex.ydb.table.YdbTable.ReadTableRequest,
                ru.yandex.ydb.table.YdbTable.ReadTableResponse>(
                  this, METHODID_STREAM_READ_TABLE)))
          .build();
    }
  }

  /**
   */
  public static final class TableServiceStub extends io.grpc.stub.AbstractStub<TableServiceStub> {
    private TableServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TableServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
    public void createSession(ru.yandex.ydb.table.YdbTable.CreateSessionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateSessionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_SESSION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public void deleteSession(ru.yandex.ydb.table.YdbTable.DeleteSessionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DeleteSessionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DELETE_SESSION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public void keepAlive(ru.yandex.ydb.table.YdbTable.KeepAliveRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.KeepAliveResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_KEEP_ALIVE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public void createTable(ru.yandex.ydb.table.YdbTable.CreateTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_CREATE_TABLE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public void dropTable(ru.yandex.ydb.table.YdbTable.DropTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DropTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DROP_TABLE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public void alterTable(ru.yandex.ydb.table.YdbTable.AlterTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.AlterTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ALTER_TABLE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public void copyTable(ru.yandex.ydb.table.YdbTable.CopyTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_COPY_TABLE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public void copyTables(ru.yandex.ydb.table.YdbTable.CopyTablesRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTablesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_COPY_TABLES, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public void describeTable(ru.yandex.ydb.table.YdbTable.DescribeTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void explainDataQuery(ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXPLAIN_DATA_QUERY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void prepareDataQuery(ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_PREPARE_DATA_QUERY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeDataQuery(ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_DATA_QUERY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public void executeSchemeQuery(ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_SCHEME_QUERY, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public void beginTransaction(ru.yandex.ydb.table.YdbTable.BeginTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.BeginTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_BEGIN_TRANSACTION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public void commitTransaction(ru.yandex.ydb.table.YdbTable.CommitTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CommitTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_COMMIT_TRANSACTION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public void rollbackTransaction(ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_ROLLBACK_TRANSACTION, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public void describeTableOptions(ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE_OPTIONS, getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public void streamReadTable(ru.yandex.ydb.table.YdbTable.ReadTableRequest request,
        io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ReadTableResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(METHOD_STREAM_READ_TABLE, getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class TableServiceBlockingStub extends io.grpc.stub.AbstractStub<TableServiceBlockingStub> {
    private TableServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TableServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
    public ru.yandex.ydb.table.YdbTable.CreateSessionResponse createSession(ru.yandex.ydb.table.YdbTable.CreateSessionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_SESSION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.DeleteSessionResponse deleteSession(ru.yandex.ydb.table.YdbTable.DeleteSessionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DELETE_SESSION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.KeepAliveResponse keepAlive(ru.yandex.ydb.table.YdbTable.KeepAliveRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_KEEP_ALIVE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.CreateTableResponse createTable(ru.yandex.ydb.table.YdbTable.CreateTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_CREATE_TABLE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.DropTableResponse dropTable(ru.yandex.ydb.table.YdbTable.DropTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DROP_TABLE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.AlterTableResponse alterTable(ru.yandex.ydb.table.YdbTable.AlterTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ALTER_TABLE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.CopyTableResponse copyTable(ru.yandex.ydb.table.YdbTable.CopyTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_COPY_TABLE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.CopyTablesResponse copyTables(ru.yandex.ydb.table.YdbTable.CopyTablesRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_COPY_TABLES, getCallOptions(), request);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.DescribeTableResponse describeTable(ru.yandex.ydb.table.YdbTable.DescribeTableRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_TABLE, getCallOptions(), request);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse explainDataQuery(ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXPLAIN_DATA_QUERY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse prepareDataQuery(ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_PREPARE_DATA_QUERY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse executeDataQuery(ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXECUTE_DATA_QUERY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse executeSchemeQuery(ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_EXECUTE_SCHEME_QUERY, getCallOptions(), request);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.BeginTransactionResponse beginTransaction(ru.yandex.ydb.table.YdbTable.BeginTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_BEGIN_TRANSACTION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.CommitTransactionResponse commitTransaction(ru.yandex.ydb.table.YdbTable.CommitTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_COMMIT_TRANSACTION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse rollbackTransaction(ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_ROLLBACK_TRANSACTION, getCallOptions(), request);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse describeTableOptions(ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest request) {
      return blockingUnaryCall(
          getChannel(), METHOD_DESCRIBE_TABLE_OPTIONS, getCallOptions(), request);
    }

    /**
     * <pre>
     * Streaming read table
     * </pre>
     */
    public java.util.Iterator<ru.yandex.ydb.table.YdbTable.ReadTableResponse> streamReadTable(
        ru.yandex.ydb.table.YdbTable.ReadTableRequest request) {
      return blockingServerStreamingCall(
          getChannel(), METHOD_STREAM_READ_TABLE, getCallOptions(), request);
    }
  }

  /**
   */
  public static final class TableServiceFutureStub extends io.grpc.stub.AbstractStub<TableServiceFutureStub> {
    private TableServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TableServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TableServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
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
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.CreateSessionResponse> createSession(
        ru.yandex.ydb.table.YdbTable.CreateSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_SESSION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Ends a session, releasing server resources associated with it.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.DeleteSessionResponse> deleteSession(
        ru.yandex.ydb.table.YdbTable.DeleteSessionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DELETE_SESSION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Idle sessions can be kept alive by calling KeepAlive periodically.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.KeepAliveResponse> keepAlive(
        ru.yandex.ydb.table.YdbTable.KeepAliveRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_KEEP_ALIVE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates new table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.CreateTableResponse> createTable(
        ru.yandex.ydb.table.YdbTable.CreateTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_CREATE_TABLE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Drop table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.DropTableResponse> dropTable(
        ru.yandex.ydb.table.YdbTable.DropTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DROP_TABLE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Modifies schema of given table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.AlterTableResponse> alterTable(
        ru.yandex.ydb.table.YdbTable.AlterTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ALTER_TABLE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates copy of given table.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.CopyTableResponse> copyTable(
        ru.yandex.ydb.table.YdbTable.CopyTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_COPY_TABLE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Creates consistent copy of given tables.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.CopyTablesResponse> copyTables(
        ru.yandex.ydb.table.YdbTable.CopyTablesRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_COPY_TABLES, getCallOptions()), request);
    }

    /**
     * <pre>
     * Returns information about given table (metadata).
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.DescribeTableResponse> describeTable(
        ru.yandex.ydb.table.YdbTable.DescribeTableRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE, getCallOptions()), request);
    }

    /**
     * <pre>
     * Explains data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse> explainDataQuery(
        ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXPLAIN_DATA_QUERY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Prepares data query, returns query id.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse> prepareDataQuery(
        ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_PREPARE_DATA_QUERY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Executes data query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse> executeDataQuery(
        ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_DATA_QUERY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Executes scheme query.
     * SessionId of previously created session must be provided.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse> executeSchemeQuery(
        ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_EXECUTE_SCHEME_QUERY, getCallOptions()), request);
    }

    /**
     * <pre>
     * Begins new transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.BeginTransactionResponse> beginTransaction(
        ru.yandex.ydb.table.YdbTable.BeginTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_BEGIN_TRANSACTION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Commits specified active transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.CommitTransactionResponse> commitTransaction(
        ru.yandex.ydb.table.YdbTable.CommitTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_COMMIT_TRANSACTION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Performs a rollback of the specified active transaction.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse> rollbackTransaction(
        ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_ROLLBACK_TRANSACTION, getCallOptions()), request);
    }

    /**
     * <pre>
     * Describe supported table options.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse> describeTableOptions(
        ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest request) {
      return futureUnaryCall(
          getChannel().newCall(METHOD_DESCRIBE_TABLE_OPTIONS, getCallOptions()), request);
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
          serviceImpl.createSession((ru.yandex.ydb.table.YdbTable.CreateSessionRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateSessionResponse>) responseObserver);
          break;
        case METHODID_DELETE_SESSION:
          serviceImpl.deleteSession((ru.yandex.ydb.table.YdbTable.DeleteSessionRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DeleteSessionResponse>) responseObserver);
          break;
        case METHODID_KEEP_ALIVE:
          serviceImpl.keepAlive((ru.yandex.ydb.table.YdbTable.KeepAliveRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.KeepAliveResponse>) responseObserver);
          break;
        case METHODID_CREATE_TABLE:
          serviceImpl.createTable((ru.yandex.ydb.table.YdbTable.CreateTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CreateTableResponse>) responseObserver);
          break;
        case METHODID_DROP_TABLE:
          serviceImpl.dropTable((ru.yandex.ydb.table.YdbTable.DropTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DropTableResponse>) responseObserver);
          break;
        case METHODID_ALTER_TABLE:
          serviceImpl.alterTable((ru.yandex.ydb.table.YdbTable.AlterTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.AlterTableResponse>) responseObserver);
          break;
        case METHODID_COPY_TABLE:
          serviceImpl.copyTable((ru.yandex.ydb.table.YdbTable.CopyTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTableResponse>) responseObserver);
          break;
        case METHODID_COPY_TABLES:
          serviceImpl.copyTables((ru.yandex.ydb.table.YdbTable.CopyTablesRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CopyTablesResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TABLE:
          serviceImpl.describeTable((ru.yandex.ydb.table.YdbTable.DescribeTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableResponse>) responseObserver);
          break;
        case METHODID_EXPLAIN_DATA_QUERY:
          serviceImpl.explainDataQuery((ru.yandex.ydb.table.YdbTable.ExplainDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExplainDataQueryResponse>) responseObserver);
          break;
        case METHODID_PREPARE_DATA_QUERY:
          serviceImpl.prepareDataQuery((ru.yandex.ydb.table.YdbTable.PrepareDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.PrepareDataQueryResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_DATA_QUERY:
          serviceImpl.executeDataQuery((ru.yandex.ydb.table.YdbTable.ExecuteDataQueryRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteDataQueryResponse>) responseObserver);
          break;
        case METHODID_EXECUTE_SCHEME_QUERY:
          serviceImpl.executeSchemeQuery((ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ExecuteSchemeQueryResponse>) responseObserver);
          break;
        case METHODID_BEGIN_TRANSACTION:
          serviceImpl.beginTransaction((ru.yandex.ydb.table.YdbTable.BeginTransactionRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.BeginTransactionResponse>) responseObserver);
          break;
        case METHODID_COMMIT_TRANSACTION:
          serviceImpl.commitTransaction((ru.yandex.ydb.table.YdbTable.CommitTransactionRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.CommitTransactionResponse>) responseObserver);
          break;
        case METHODID_ROLLBACK_TRANSACTION:
          serviceImpl.rollbackTransaction((ru.yandex.ydb.table.YdbTable.RollbackTransactionRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.RollbackTransactionResponse>) responseObserver);
          break;
        case METHODID_DESCRIBE_TABLE_OPTIONS:
          serviceImpl.describeTableOptions((ru.yandex.ydb.table.YdbTable.DescribeTableOptionsRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.DescribeTableOptionsResponse>) responseObserver);
          break;
        case METHODID_STREAM_READ_TABLE:
          serviceImpl.streamReadTable((ru.yandex.ydb.table.YdbTable.ReadTableRequest) request,
              (io.grpc.stub.StreamObserver<ru.yandex.ydb.table.YdbTable.ReadTableResponse>) responseObserver);
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

  private static final class TableServiceDescriptorSupplier implements io.grpc.protobuf.ProtoFileDescriptorSupplier {
    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return ru.yandex.ydb.table.v1.YdbTableV1.getDescriptor();
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
              .setSchemaDescriptor(new TableServiceDescriptorSupplier())
              .addMethod(METHOD_CREATE_SESSION)
              .addMethod(METHOD_DELETE_SESSION)
              .addMethod(METHOD_KEEP_ALIVE)
              .addMethod(METHOD_CREATE_TABLE)
              .addMethod(METHOD_DROP_TABLE)
              .addMethod(METHOD_ALTER_TABLE)
              .addMethod(METHOD_COPY_TABLE)
              .addMethod(METHOD_COPY_TABLES)
              .addMethod(METHOD_DESCRIBE_TABLE)
              .addMethod(METHOD_EXPLAIN_DATA_QUERY)
              .addMethod(METHOD_PREPARE_DATA_QUERY)
              .addMethod(METHOD_EXECUTE_DATA_QUERY)
              .addMethod(METHOD_EXECUTE_SCHEME_QUERY)
              .addMethod(METHOD_BEGIN_TRANSACTION)
              .addMethod(METHOD_COMMIT_TRANSACTION)
              .addMethod(METHOD_ROLLBACK_TRANSACTION)
              .addMethod(METHOD_DESCRIBE_TABLE_OPTIONS)
              .addMethod(METHOD_STREAM_READ_TABLE)
              .build();
        }
      }
    }
    return result;
  }
}
