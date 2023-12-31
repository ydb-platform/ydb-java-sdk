package tech.ydb.table.integration;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.CopyTablesSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.RenameTablesSettings;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Maksim Zinal
 */
public class RenameTablesTest {
    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydbTransport)
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    private final String origTableName1 = "test1_table";
    private final String origTableName2 = "test2_table";

    private final String renamedTableName1 = "test1_table_renamed";
    private final String renamedTableName2 = "test2_table_renamed";

    private final String copiedTableName1 = "test1_table_copied";
    private final String copiedTableName2 = "test2_table_copied";

    private final String origTablePath1 = ydbTransport.getDatabase() + "/" + origTableName1;
    private final String origTablePath2 = ydbTransport.getDatabase() + "/" + origTableName2;

    private final String renamedTablePath1 = ydbTransport.getDatabase() + "/" + renamedTableName1;
    private final String renamedTablePath2 = ydbTransport.getDatabase() + "/" + renamedTableName2;

    private final String copiedTablePath1 = ydbTransport.getDatabase() + "/" + copiedTableName1;
    private final String copiedTablePath2 = ydbTransport.getDatabase() + "/" + copiedTableName2;

    @Test
    public void testRenameTables() {
        TableDescription tableDescription = TableDescription.newBuilder()
                .addNullableColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("code", PrimitiveType.Text)
                .addNullableColumn("size", PrimitiveType.Float)
                .addNullableColumn("created", PrimitiveType.Timestamp)
                .addNullableColumn("data", PrimitiveType.Json)
                .setPrimaryKey("id")
                .addGlobalIndex("ix1", Arrays.asList("code"))
                .addGlobalAsyncIndex("ix2", Arrays.asList("created"))
                .build();

        createTable(origTablePath1, tableDescription);
        createTable(origTablePath2, tableDescription);

        renameTables();

        describeTable(renamedTablePath1, tableDescription);
        describeTable(renamedTablePath2, tableDescription);

        copyTables();

        describeTable(copiedTablePath1, tableDescription);
        describeTable(copiedTablePath2, tableDescription);

        dropTable(renamedTablePath1);
        dropTable(renamedTablePath2);
        dropTable(copiedTablePath1);
        dropTable(copiedTablePath2);
    }

    private void createTable(String tablePath, TableDescription tableDescription) {
        Status status = ctx.supplyStatus(
                session -> session.createTable(tablePath, tableDescription, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table " + status, status.isSuccess());
    }

    private void renameTables() {
        RenameTablesSettings settings = new RenameTablesSettings();
        settings.addTable(origTablePath1, renamedTablePath1);
        settings.addTable(origTablePath2, renamedTablePath2);
        Status status = ctx.supplyStatus(session -> session.renameTables(settings)).join();
        Assert.assertTrue("Rename tables " + status, status.isSuccess());
    }

    private void copyTables() {
        CopyTablesSettings settings = new CopyTablesSettings();
        settings.addTable(renamedTablePath1, copiedTablePath1, false);
        settings.addTable(renamedTablePath2, copiedTablePath2, false);
        Status status = ctx.supplyStatus(session -> session.copyTables(settings)).join();
        Assert.assertTrue("Copy tables " + status, status.isSuccess());
    }

    private void describeTable(String tablePath, TableDescription tableDescription) {
        Result<TableDescription> describeResult = ctx.supplyResult(session -> {
            return session.describeTable(tablePath);
        }).join();
        Assert.assertTrue("Describe table " + describeResult.getStatus(), describeResult.isSuccess());

        TableDescription description = describeResult.getValue();

        Assert.assertEquals(
                "Table description columns size",
                tableDescription.getColumns().size(),
                description.getColumns().size()
        );

        Assert.assertEquals(
                "Table description primary keys size",
                tableDescription.getPrimaryKeys().size(),
                description.getPrimaryKeys().size()
        );

        Assert.assertEquals(
                "Table description indexes size",
                tableDescription.getIndexes().size(),
                description.getIndexes().size()
        );

        for (int idx = 0; idx < tableDescription.getColumns().size(); idx += 1) {
            TableColumn one = tableDescription.getColumns().get(idx);
            TableColumn two = description.getColumns().get(idx);

            Assert.assertEquals("Table column name " + idx, one.getName(), two.getName());
            Assert.assertEquals("Table column type " + idx, one.getType(), two.getType());
        }

        for (int idx = 0; idx < tableDescription.getPrimaryKeys().size(); idx += 1) {
            String one = tableDescription.getPrimaryKeys().get(idx);
            String two = description.getPrimaryKeys().get(idx);
            Assert.assertEquals("Table primary key " + idx, one, two);
        }
    }

    private void dropTable(String tablePath) {
        Status status = ctx.supplyStatus(
                session -> session.dropTable(tablePath)
        ).join();

        Assert.assertTrue("Drop table " + status, status.isSuccess());
    }

}
