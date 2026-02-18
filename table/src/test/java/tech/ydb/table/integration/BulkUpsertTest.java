package tech.ydb.table.integration;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.ApacheArrowWriter;
import tech.ydb.table.query.BulkUpsertData;
import tech.ydb.table.query.Params;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.ExecuteScanQuerySettings;
import tech.ydb.table.settings.ExecuteSchemeQuerySettings;
import tech.ydb.table.values.ListValue;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class BulkUpsertTest {
    @ClassRule
    public static final GrpcTransportRule YDB = new GrpcTransportRule();
    private static final SimpleTableClient client = SimpleTableClient.newClient(GrpcTableRpc.useTransport(YDB)).build();
    private static final SessionRetryContext retryCtx = SessionRetryContext.create(client)
            .idempotent(false)
            .build();

    private static final String TEST_TABLE = "arrow/test_table";
    private static final String DROP_TABLE_YQL = "DROP TABLE IF EXISTS `" + TEST_TABLE + "`;";
    private static final String SELECT_TABLE_YQL = "DECLARE $id1 AS Uint64; SELECT * FROM `" + TEST_TABLE + "` "
            + "WHERE id1 = $id1 ORDER BY id2";

    private static String tablePath() {
        return YDB.getDatabase() + "/" + TEST_TABLE;
    }

    @After
    public void cleanTable() {
        retryCtx.supplyStatus(session -> session.executeSchemeQuery(DROP_TABLE_YQL, new ExecuteSchemeQuerySettings()))
            .join().expectSuccess("cannot drop table");
    }

    private static void createTable(TableDescription table) {
        retryCtx.supplyStatus(s -> s.createTable(tablePath(), table))
                .join().expectSuccess("Cannot create table");
    }

    private static void bulkUpsert(BulkUpsertData data) {
        retryCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath(), data))
                .join().expectSuccess("bulk upsert problem in table " + tablePath());
    }

    private static void bulkUpsert(ListValue rows) {
        retryCtx.supplyStatus(session -> session.executeBulkUpsert(tablePath(), rows))
                .join().expectSuccess("bulk upsert problem in table " + tablePath());
    }

    private static int readTable(long id1, BiConsumer<Integer, ResultSetReader> validator) {
        AtomicInteger count = new AtomicInteger();

        try {
            retryCtx.supplyStatus(session -> {
                count.set(0);

                GrpcReadStream<ResultSetReader> stream = session.executeScanQuery(SELECT_TABLE_YQL,
                        Params.of("$id1", PrimitiveValue.newUint64(id1)),
                        ExecuteScanQuerySettings.newBuilder().build()
                );

                return stream.start((rs) -> {
                    while (rs.next()) {
                        int idx = count.getAndIncrement();
                        validator.accept(idx, rs);
                    }
                });
            }).join().expectSuccess("Cannot read table " + TEST_TABLE);
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof AssertionError) {
                throw (AssertionError) ex.getCause();
            }
            throw ex;
        }

        return count.get();
    }

    @Test
    public void writeProtobufToDataShardTest() {
        // Create table
        TableDescription table = AllTypesRecord.createTableDescription(false);
        createTable(table);

        Set<String> columnNames = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());

        // Write & read batch of 1000 records with id1 = 1
        List<AllTypesRecord> batch1 = AllTypesRecord.randomBatch(1, 1, 1000);
        bulkUpsert(AllTypesRecord.createProtobufBatch(table, batch1));

        int rows1Count = readTable(1, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch1.size());
            batch1.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(1000, rows1Count);

        // Write & read batch of 2000 records with id1 = 2
        List<AllTypesRecord> batch2 = AllTypesRecord.randomBatch(2, 1, 2000);
        bulkUpsert(AllTypesRecord.createProtobufBatch(table, batch2));

        int rows2Count = readTable(2, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch2.size());
            batch2.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(2000, rows2Count);
    }

    @Test
    public void writeProtobufToColumnShardTable() {
        // Create table
        TableDescription table = AllTypesRecord.createTableDescription(true);
        Set<String> columnNames = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());

        createTable(table);

        // Write & read batch of 2500 records with id1 = 1
        List<AllTypesRecord> batch1 = AllTypesRecord.randomBatch(1, 1, 2500);
        bulkUpsert(AllTypesRecord.createProtobufBatch(table, batch1));
        // Read table and validate data
        int rows1Count = readTable(1, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch1.size());
            batch1.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(2500, rows1Count);

        // Write & read batch of 5000 records with id1 = 2
        List<AllTypesRecord> batch2 = AllTypesRecord.randomBatch(2, 1, 5000);
        bulkUpsert(AllTypesRecord.createProtobufBatch(table, batch2));

        int rows2Count = readTable(2, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch2.size());
            batch2.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(5000, rows2Count);
    }

    @Test
    public void writeApacheArrowToDataShardTest() {
        // Create table
        TableDescription table = AllTypesRecord.createTableDescription(false);
        retryCtx.supplyStatus(s -> s.createTable(tablePath(), table)).join().expectSuccess("Cannot create table");

        Set<String> columnNames = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());

        ApacheArrowWriter.Schema schema = ApacheArrowWriter.newSchema();
        table.getColumns().forEach(column -> schema.addColumn(column.getName(), column.getType()));

        // Create batch of 1000 records
        List<AllTypesRecord> batch1 = AllTypesRecord.randomBatch(1, 1, 1000);
        // Create batch of 2000 records
        List<AllTypesRecord> batch2 = AllTypesRecord.randomBatch(2, 1, 2000);

        try (BufferAllocator allocator = new RootAllocator()) {
            try (ApacheArrowWriter writer = schema.createWriter(allocator)) {
                // create batch with estimated size
                ApacheArrowWriter.Batch data1 = writer.createNewBatch(1000);
                batch1.forEach(r -> r.writeToApacheArrow(columnNames, data1.writeNextRow()));
                bulkUpsert(data1.buildBatch());

                // create batch without estimated size
                ApacheArrowWriter.Batch data2 = writer.createNewBatch(0);
                batch2.forEach(r -> r.writeToApacheArrow(columnNames, data2.writeNextRow()));
                bulkUpsert(data2.buildBatch());

            } catch (IOException ex) {
                throw new AssertionError("Cannot serialize apache arrow", ex);
            }
        }

        int rows1Count = readTable(1, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch1.size());
            batch1.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(1000, rows1Count);

        int rows2Count = readTable(2, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index ", idx < batch2.size());
            batch2.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(2000, rows2Count);
    }

    @Test
    public void writeApacheArrowToColumnShardTest() {
        // Create table
        TableDescription table = AllTypesRecord.createTableDescription(true);
        retryCtx.supplyStatus(s -> s.createTable(tablePath(), table)).join().expectSuccess("Cannot create table");

        Set<String> columnNames = table.getColumns().stream().map(TableColumn::getName).collect(Collectors.toSet());

        ApacheArrowWriter.Schema schema = ApacheArrowWriter.newSchema();
        table.getColumns().forEach(column -> schema.addColumn(column.getName(), column.getType()));

        // Create batch of 2500 records
        List<AllTypesRecord> batch1 = AllTypesRecord.randomBatch(1, 1, 2500);
        // Create batch of 5000 records
        List<AllTypesRecord> batch2 = AllTypesRecord.randomBatch(2, 1, 5000);

        try (BufferAllocator allocator = new RootAllocator()) {
            try (ApacheArrowWriter writer = schema.createWriter(allocator)) {
                // create batch without estimated size
                ApacheArrowWriter.Batch data1 = writer.createNewBatch(0);
                batch1.forEach(r -> r.writeToApacheArrow(columnNames, data1.writeNextRow()));
                bulkUpsert(data1.buildBatch());

                // create batch with estimated size
                ApacheArrowWriter.Batch data2 = writer.createNewBatch(5000);
                batch2.forEach(r -> r.writeToApacheArrow(columnNames, data2.writeNextRow()));
                bulkUpsert(data2.buildBatch());

            } catch (IOException ex) {
                throw new AssertionError("Cannot serialize apache arrow", ex);
            }
        }

        int rows1Count = readTable(1, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index", idx < batch1.size());
            batch1.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(2500, rows1Count);

        int rows2Count = readTable(2, (idx, rs) -> {
            Assert.assertTrue("Unexpected row index ", idx < batch2.size());
            batch2.get(idx).assertRow(columnNames, idx, rs);
        });
        Assert.assertEquals(5000, rows2Count);
    }

//
//    private void assertApacheArrowBatch(ByteString schemaBytes, ByteString batchBytes, Iterator<Record> it) {
//        try (BufferAllocator allocator = new RootAllocator()) {
//            Schema schema = readApacheArrowSchema(schemaBytes);
//            try (VectorSchemaRoot vector = VectorSchemaRoot.create(schema, allocator)) {
//                try (InputStream is = batchBytes.newInput()) {
//                    try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
//                        try (ArrowRecordBatch batch = MessageSerializer.deserializeRecordBatch(channel, allocator)) {
//                            VectorLoader loader = new VectorLoader(vector);
//                            loader.load(batch);
//                        }
//                    }
//                }
//
//                UInt8Vector id1 = (UInt8Vector) vector.getVector("id1");
//                BigIntVector id2 = (BigIntVector) vector.getVector("id2");
//                IntVector length = (IntVector) vector.getVector("length");
//                VarCharVector hash = (VarCharVector) vector.getVector("hash");
//                VarBinaryVector data = (VarBinaryVector) vector.getVector("data");
//                TimeStampMicroTZVector tm = (TimeStampMicroTZVector) vector.getVector("timestamp");
//                UInt2Vector date = (UInt2Vector) vector.getVector("date");
//                Float8Vector amount = (Float8Vector) vector.getVector("amount");
//
//                for (int idx = 0; idx < vector.getRowCount(); idx++) {
//                    Assert.assertTrue("Assert has no row " + idx, it.hasNext());
//                    Record r = it.next();
//
//                    Assert.assertEquals("Row " + idx + " fail", r.id1, id1.get(idx));
//                    Assert.assertEquals("Row " + idx + " fail", r.id2, id2.get(idx));
//                    Assert.assertEquals("Row " + idx + " fail", r.length, length.get(idx));
//                    Assert.assertArrayEquals("Row " + idx + " fail", r.hash.getBytes(), hash.get(idx));
//                    Assert.assertArrayEquals("Row " + idx + " fail", r.data, data.get(idx));
//
//                    long rm = r.timestamp.getEpochSecond() * 1000000L + r.timestamp.getNano() / 1000;
//                    Assert.assertEquals("Row " + idx + " fail", rm, tm.get(idx));
//                    Assert.assertEquals("Row " + idx + " fail", r.date, LocalDate.ofEpochDay(date.get(idx)));
//                    Assert.assertEquals("Row " + idx + " fail", r.amount, amount.get(idx), 1e-6);
//                }
//            }
//        } catch (IOException ex) {
//          throw new RuntimeException(ex);
//        }
//    }
}
