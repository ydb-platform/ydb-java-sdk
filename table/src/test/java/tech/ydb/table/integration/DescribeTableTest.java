package tech.ydb.table.integration;

import java.util.Collections;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Issue;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.ColumnFamily;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class DescribeTableTest {
    private static final String TABLE_NAME = "table_describe_test";
    private static final String TABLE_COPY_NAME = "table_describe_test_copy";

    @ClassRule
    public static final GrpcTransportRule YDB_TRANSPORT = new GrpcTransportRule();

    private static final SessionRetryContext CTX = SessionRetryContext.create(SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(YDB_TRANSPORT)
    ).build()).build();

    private final String tablePath = YDB_TRANSPORT.getDatabase() + "/" + TABLE_NAME;
    private final String tableCopyPath = YDB_TRANSPORT.getDatabase() + "/" + TABLE_COPY_NAME;

    @After
    public void dropTable() {
        CTX.supplyStatus(session -> session.dropTable(tablePath)).join();
        CTX.supplyStatus(session -> session.dropTable(tableCopyPath)).join();
    }

    @Test
    public void wrongTypeTest() {
        String databasePath = YDB_TRANSPORT.getDatabase();
        Status status = CTX.supplyResult(s -> s.describeTable(databasePath)).join().getStatus();

        Assert.assertFalse("Unexpected success", status.isSuccess());
        Assert.assertEquals(StatusCode.SCHEME_ERROR, status.getCode());
        Issue[] issues = status.getIssues();
        Assert.assertEquals(1, issues.length);
        String entryName = databasePath.substring(1); // remove lead /
        Assert.assertEquals("Entry " + entryName + " with type DIRECTORY is not a table", issues[0].getMessage());
        Assert.assertEquals(Issue.Severity.ERROR, issues[0].getSeverity());
    }

    @Test
    public void copyTableTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int8)
                .addNonnullColumn("version", PrimitiveType.Uint32)
                .addNullableColumn("value", PrimitiveType.Text, PrimitiveValue.newText("123"))
                .addColumnFamily(new ColumnFamily("default", null, ColumnFamily.Compression.COMPRESSION_LZ4))
                .addColumnFamily(new ColumnFamily("raw", null, null))
                .setPrimaryKeys("id", "version")
                .build();

        CTX.supplyStatus(session -> session.createTable(tablePath, createTable)).join()
                .expectSuccess("cannot create table " + tablePath);

        TableDescription tableDesc = CTX.supplyResult(session -> session.describeTable(tablePath)).join().getValue();

        Assert.assertEquals(2, tableDesc.getColumnFamilies().size());
        Assert.assertEquals(ColumnFamily.Compression.COMPRESSION_LZ4, findFamily(tableDesc, "default").getCompression());
        Assert.assertEquals(ColumnFamily.Compression.COMPRESSION_NONE, findFamily(tableDesc, "raw").getCompression());

        CTX.supplyStatus(session -> session.createTable(tableCopyPath, tableDesc)).join()
                .expectSuccess("cannot create table " + tableCopyPath);

        TableDescription copyDesc = CTX.supplyResult(session -> session.describeTable(tableCopyPath)).join().getValue();
        Assert.assertEquals(2, copyDesc.getColumnFamilies().size());
        Assert.assertEquals(ColumnFamily.Compression.COMPRESSION_LZ4, findFamily(copyDesc, "default").getCompression());
        Assert.assertEquals(ColumnFamily.Compression.COMPRESSION_NONE, findFamily(copyDesc, "raw").getCompression());
    }

    @Test
    public void jsonIndexTest() {
        CTX.supplyStatus(session -> session.executeSchemeQuery("CREATE TABLE `" + tablePath + "` ("
                + "id Uint64 NOT NULL, payload JsonDocument NOT NULL, title Text, PRIMARY KEY(id),"
                + "INDEX global_idx GLOBAL ON (title), INDEX json_idx GLOBAL USING json ON (payload))"))
                .join().expectSuccess();

        TableDescription description = CTX.supplyResult(session -> session.describeTable(tablePath)).join().getValue();
        Assert.assertEquals(2, description.getIndexes().size());
        Assert.assertEquals(TableIndex.Type.GLOBAL, description.getIndexes().get(0).getType());
        TableIndex index = description.getIndexes().get(1);
        Assert.assertEquals("json_idx", index.getName());
        Assert.assertEquals(TableIndex.Type.GLOBAL_JSON, index.getType());
        Assert.assertEquals(Collections.singletonList("payload"), index.getColumns());
        Assert.assertTrue(index.getDataColumns().isEmpty());

        CTX.supplyStatus(session -> session.createTable(tableCopyPath, description)).join().expectSuccess();
        TableDescription copy = CTX.supplyResult(session -> session.describeTable(tableCopyPath)).join().getValue();
        Assert.assertEquals(2, copy.getIndexes().size());
        Assert.assertEquals(TableIndex.Type.GLOBAL_JSON, copy.getIndexes().get(1).getType());
    }

    private ColumnFamily findFamily(TableDescription desc, String name) {
        for (ColumnFamily family : desc.getColumnFamilies()) {
            if (name.equals(family.getName())) {
                return family;
            }
        }
        throw new AssertionError("Expected column family with name " + name);
    }
}
