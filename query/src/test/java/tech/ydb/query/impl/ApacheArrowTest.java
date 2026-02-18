package tech.ydb.query.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.RootAllocator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.common.transaction.TxMode;
import tech.ydb.core.Status;
import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryClient;
import tech.ydb.query.QuerySession;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryResultPart;
import tech.ydb.query.result.arrow.ArrowPartsHandler;
import tech.ydb.query.result.arrow.ArrowQueryResultPart;
import tech.ydb.query.settings.ExecuteQuerySettings;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.Params;
import tech.ydb.table.query.arrow.ApacheArrowData;
import tech.ydb.table.query.arrow.ApacheArrowWriter;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ApacheArrowTest {
    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule();

    private static final String ROW_TABLE_NAME = "arrow/ds_table";
    private static final String COLUMN_TABLE_NAME = "arrow/cs_table";

    private static final TableDescription ROW_TABLE = AllTypesRecord.createTableDescription(false);
    private static final TableDescription COLUMN_TABLE = AllTypesRecord.createTableDescription(true);

    private static final List<AllTypesRecord> ROW_BATCH = AllTypesRecord.randomBatch(1, 0, 2000);
    private static final List<AllTypesRecord> COLUMN_BATCH = AllTypesRecord.randomBatch(1, 0, 5000);

    private static final SessionRetryContext retryCtx = SessionRetryContext.create(
            SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build()
    ).build();

    private static RootAllocator allocator;
    private static QueryClient client;

    private static String tablePath(String tableName) {
        return YDB.getDatabase() + "/" + tableName;
    }

    private static String dropTableYql(String tableName) {
        return "DROP TABLE IF EXISTS `" + tableName + "`;";
    }

    private static String selectTableYql(String tableName) {
        return "SELECT * FROM `" + tableName + "` ORDER BY id1, id2";
    }

    private static void dropTable(String tablePath) {
        ExecuteSchemeQuerySettings settings = new ExecuteSchemeQuerySettings();
        retryCtx.supplyStatus(
                session -> session.executeSchemeQuery(dropTableYql(tablePath), settings)
        ).join().expectSuccess("cannot drop table " + tablePath);
    }

    private static void initTable(String tablePath, TableDescription table, List<AllTypesRecord> data) {
        retryCtx.supplyStatus(session -> session.createTable(tablePath, table))
                .join().expectSuccess("cannot create table " + tablePath);

        Set<String> columnNames = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());
        ApacheArrowWriter.Schema schema = ApacheArrowWriter.newSchema();
        table.getColumns().forEach(column -> schema.addColumn(column.getName(), column.getType()));

        try (ApacheArrowWriter writer = schema.createWriter(allocator)) {
            // create batch with estimated size
            ApacheArrowWriter.Batch batch = writer.createNewBatch(data.size());
            data.forEach(r -> r.writeToApacheArrow(columnNames, batch.writeNextRow()));
            ApacheArrowData bulkData = batch.buildBatch();
            retryCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath, bulkData))
                    .join().expectSuccess("bulk upsert problem in table " + tablePath);
        } catch (IOException ex) {
            throw new AssertionError("Cannot serialize apache arrow", ex);
        }
    }

    @BeforeClass
    public static void initTables() {
        allocator = new RootAllocator();
        client = QueryClient.newClient(YDB).build();

        initTable(tablePath(ROW_TABLE_NAME), AllTypesRecord.createTableDescription(false), ROW_BATCH);
        initTable(tablePath(COLUMN_TABLE_NAME), AllTypesRecord.createTableDescription(true), COLUMN_BATCH);
    }

    @AfterClass
    public static void cleanTables() {
        allocator.close();
        client.close();

        dropTable(ROW_TABLE_NAME);
        dropTable(COLUMN_TABLE_NAME);
    }

    private static void assertStatusOK(Status status) {
        if (status.getCause() instanceof AssertionError) {
            throw (AssertionError) status.getCause();
        }

        status.expectSuccess("Cannot execute query");
    }

    private static void assertIllegalStateExceptionFuture(String message, CompletableFuture<?> future) {
        CompletionException ex = Assert.assertThrows(CompletionException.class, future::join);
        Assert.assertTrue(ex.getCause() instanceof IllegalStateException);
        Assert.assertEquals(message, ex.getCause().getMessage());
    }

    private static class BatchAssert {
        private final AtomicInteger idx = new AtomicInteger(0);
        private final Iterator<AllTypesRecord> iter;
        private final Set<String> columnsToAssert;

        public BatchAssert(TableDescription table, List<AllTypesRecord> batch) {
            this.iter = batch.iterator();
            this.columnsToAssert = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());
        }

        public void assertResultSetReader(ResultSetReader rs) {
            while (rs.next()) {
                Assert.assertTrue(iter.hasNext());
                iter.next().assertRow(columnsToAssert, idx.incrementAndGet(), rs);
            }
        }

        public void assertFinish() {
            Assert.assertFalse(iter.hasNext());
        }
    }

    @Test
    public void backwardCompatibilityTest() {
        BatchAssert ba = new BatchAssert(ROW_TABLE, ROW_BATCH);
        String query = selectTableYql(ROW_TABLE_NAME);

        try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            // Execute query without ApacheArrow (or if server doesn't support it)
            QueryStream stream = session.createQuery(query, TxMode.SNAPSHOT_RO);
            assertStatusOK(stream.execute(new ArrowPartsHandler(allocator) {
                @Override
                public void onNextPart(QueryResultPart part) {
                    Assert.assertFalse(part instanceof ArrowQueryResultPart);
                    Assert.assertEquals(0, part.getResultSetIndex());
                    ba.assertResultSetReader(part.getResultSetReader());
                }
            }).join().getStatus());

            ba.assertFinish();
        }
    }

    @Test
    public void unsupportedTypesTest() {
        ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder().useApacheArrowFormat().build();
        String query = "SELECT AddTimezone(Timestamp('2026-10-29T04:23:45.987654Z'), 'Europe/Warsaw') as p1;";

        try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            QueryStream stream = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings);

            assertIllegalStateExceptionFuture("Unsupported type for ApacheArrow reader: type_id: TZ_TIMESTAMP\n",
                    stream.execute(new ArrowPartsHandler(allocator) {
                        @Override
                        public void onNextPart(QueryResultPart part) {
                            // not called
                        }
                    }));
        }
    }

    @Test
    public void readApacheArrowDataShardsDataTest() throws Throwable {
        BatchAssert ba = new BatchAssert(ROW_TABLE, ROW_BATCH);
        String query = selectTableYql(ROW_TABLE_NAME);
        ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder().useApacheArrowFormat().build();

        try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            QueryStream stream = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings);
            assertStatusOK(stream.execute(new ArrowPartsHandler(allocator) {
                @Override
                public void onNextPart(QueryResultPart part) {
                    Assert.assertTrue(part instanceof ArrowQueryResultPart);
                    Assert.assertEquals(0, part.getResultSetIndex());
                    ba.assertResultSetReader(part.getResultSetReader());
                }
            }).join().getStatus());
            ba.assertFinish();
        }
    }

    @Test
    public void readApacheArrowColumnShardsDataTest() throws Throwable {
        BatchAssert ba = new BatchAssert(COLUMN_TABLE, COLUMN_BATCH);
        String query = selectTableYql(COLUMN_TABLE_NAME);
        ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder().useApacheArrowFormat().build();

        try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            QueryStream stream = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings);
            assertStatusOK(stream.execute(new ArrowPartsHandler(allocator) {
                @Override
                public void onNextPart(QueryResultPart part) {
                    Assert.assertTrue(part instanceof ArrowQueryResultPart);
                    Assert.assertEquals(0, part.getResultSetIndex());
                    ba.assertResultSetReader(part.getResultSetReader());
                }
            }).join().getStatus());
            ba.assertFinish();
        }
    }

    @Test
    public void copyTableTest() throws Throwable {
        BatchAssert ba = new BatchAssert(ROW_TABLE, ROW_BATCH);

        String newTablePath = tablePath(ROW_TABLE_NAME + "_copy");
        retryCtx.supplyStatus(session -> session.createTable(newTablePath, ROW_TABLE)).join()
                .expectSuccess("cannot create table " + newTablePath);

        try (QuerySession session = client.createSession(Duration.ofSeconds(5)).join().getValue()) {
            // binary copy ROW_TABLE to newTableName
            String query = selectTableYql(ROW_TABLE_NAME);
            ExecuteQuerySettings settings = ExecuteQuerySettings.newBuilder().useApacheArrowFormat().build();
            QueryStream stream = session.createQuery(query, TxMode.SNAPSHOT_RO, Params.empty(), settings);
            assertStatusOK(stream.execute(new QueryStream.PartsHandler() {
                @Override
                public void onNextPart(QueryResultPart part) {
                    // not used
                }

                @Override
                public void onNextRawPart(long index, ValueProtos.ResultSet rs) {
                    Assert.assertTrue(rs.hasArrowFormatMeta());
                    ByteString schema = rs.getArrowFormatMeta().getSchema();
                    ApacheArrowData data = new ApacheArrowData(schema, rs.getData());
                    retryCtx.supplyStatus(session -> session.executeBulkUpsert(newTablePath, data))
                            .join().expectSuccess("cannot execute bulk upsert");
                }
            }).join().getStatus());

            // check data in the copied table
            assertStatusOK(session
                    .createQuery(selectTableYql(newTablePath), TxMode.SNAPSHOT_RO)
                    .execute(part -> ba.assertResultSetReader(part.getResultSetReader()))
                    .join().getStatus());
            ba.assertFinish();
        } finally {
            dropTable(newTablePath);
        }
    }
}
