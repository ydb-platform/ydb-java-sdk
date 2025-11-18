package tech.ydb.table.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableIndex;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.Type;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class AlterTableTest {
    private final static String DEFAULT_FAMILY = "default";
    private final static String EMPTY_FAMILY = "";

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final String TABLE_NAME = "alter_table_test";

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydbTransport)
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    private final String tablePath = ydbTransport.getDatabase() + "/" + TABLE_NAME;

    @After
    public void dropTable() {
        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void alterTableTest() {
        // --------------------- create table -----------------------------
        TableDescription createTableDesc = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("code", PrimitiveType.Text)
                .addNullableColumn("size", PrimitiveType.Float)
                .addNullableColumn("created", PrimitiveType.Timestamp)
                .addNullableColumn("data", PrimitiveType.Text)
                .setPrimaryKey("id")
                .addGlobalIndex("idx1", Arrays.asList("id", "code"))
                .addGlobalAsyncIndex("idx2", Collections.singletonList("data"), Collections.singletonList("code"))
                .build();

        Status createStatus = ctx.supplyStatus(
                session -> session.createTable(tablePath, createTableDesc, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table with indexes " + createStatus, createStatus.isSuccess());

        // --------------------- describe table after creating -----------------------------
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with indexes " + describeResult.getStatus(), describeResult.isSuccess());

        TableDescription description = describeResult.getValue();

        Assert.assertEquals(TableDescription.StoreType.ROW, description.getStoreType());
        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Uint64, true, false);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "created", PrimitiveType.Timestamp);
        assertColumn(description.getColumns().get(4), "data", PrimitiveType.Text);

        Assert.assertEquals(2, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(0), "idx1", Arrays.asList("id", "code"), Collections.emptyList());
        assertIndexAsync(description.getIndexes().get(1), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));

        // --------------------- alter table with changing columns -----------------------------

        Status alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .addNullableColumn("data2", PrimitiveType.Bytes)
                        .dropColumn("created"))
        ).join();
        Assert.assertTrue("Alter table with column " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        description = describeResult.getValue();

        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Uint64, true, false);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "data", PrimitiveType.Text);
        assertColumn(description.getColumns().get(4), "data2", PrimitiveType.Bytes);

        Assert.assertEquals(2, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(0), "idx1", Arrays.asList("id", "code"), Collections.emptyList());
        assertIndexAsync(description.getIndexes().get(1), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));

        // --------------------- alter table with changing indexes -----------------------------
        alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .dropIndex("idx1"))
        ).join();
        Assert.assertTrue("Alter table with indexes " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        description = describeResult.getValue();

        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Uint64, true, false);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "data", PrimitiveType.Text);
        assertColumn(description.getColumns().get(4), "data2", PrimitiveType.Bytes);

        Assert.assertEquals(1, description.getIndexes().size());
        assertIndexAsync(description.getIndexes().get(0), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));
    }

    @Test
    public void alterTableWithSerialTest() {
        // --------------------- create table -----------------------------
        Status createStatus = ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE alter_table_test ("
                + " id BigSerial NOT NULL,"
                + " code Text NOT NULL,"
                + " size Float,"
                + " created Timestamp,"
                + " data Text,"
                + " PRIMARY KEY(id),"
                + " INDEX idx1 GLOBAL ON (id, code),"
                + " INDEX idx2 GLOBAL ASYNC ON (data) COVER (code)"
                + ")"
        )).join();
        Assert.assertTrue("Create table with indexes " + createStatus, createStatus.isSuccess());

        // --------------------- describe table after creating -----------------------------
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with indexes " + describeResult.getStatus(), describeResult.isSuccess());

        TableDescription description = describeResult.getValue();

        Assert.assertEquals(TableDescription.StoreType.ROW, description.getStoreType());
        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Int64, true, true);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text, true, false);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "created", PrimitiveType.Timestamp);
        assertColumn(description.getColumns().get(4), "data", PrimitiveType.Text);

        Assert.assertEquals(2, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(0), "idx1", Arrays.asList("id", "code"), Collections.emptyList());
        assertIndexAsync(description.getIndexes().get(1), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));

        // --------------------- alter table with changing columns -----------------------------

        Status alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .addNullableColumn("data2", PrimitiveType.Bytes)
                        .dropColumn("created"))
        ).join();
        Assert.assertTrue("Alter table with column " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        description = describeResult.getValue();

        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Int64, true, true);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text, true, false);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "data", PrimitiveType.Text);
        assertColumn(description.getColumns().get(4), "data2", PrimitiveType.Bytes);

        Assert.assertEquals(2, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(0), "idx1", Arrays.asList("id", "code"), Collections.emptyList());
        assertIndexAsync(description.getIndexes().get(1), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));

        // --------------------- alter table with changing indexes -----------------------------
        alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .dropIndex("idx1"))
        ).join();
        Assert.assertTrue("Alter table with indexes " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        description = describeResult.getValue();

        Assert.assertEquals(1, description.getColumnFamilies().size());
        Assert.assertEquals(DEFAULT_FAMILY, description.getColumnFamilies().get(0).getName());

        Assert.assertEquals(5, description.getColumns().size());
        assertColumn(description.getColumns().get(0), "id", PrimitiveType.Int64, true, true);
        assertColumn(description.getColumns().get(1), "code", PrimitiveType.Text, true, false);
        assertColumn(description.getColumns().get(2), "size", PrimitiveType.Float);
        assertColumn(description.getColumns().get(3), "data", PrimitiveType.Text);
        assertColumn(description.getColumns().get(4), "data2", PrimitiveType.Bytes);

        Assert.assertEquals(1, description.getIndexes().size());
        assertIndexAsync(description.getIndexes().get(0), "idx2", Collections.singletonList("data"), Collections.singletonList("code"));
    }

    @Test
    public void renameIndexTest() {
        // --------------------- create table -----------------------------
        ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE " + TABLE_NAME + " ("
                + " id Uint64 NOT NULL,"
                + " code Text NOT NULL,"
                + " size Float,"
                + " created Timestamp,"
                + " data Text,"
                + " PRIMARY KEY(id),"
                + " INDEX idx1 GLOBAL ON (id, code),"
                + " INDEX idx2 GLOBAL ASYNC ON (data) COVER (code)"
                + ")"
        )).join().expectSuccess();

        Status alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .addRenameIndex("idx1", "new_name"))
        ).join();
        Assert.assertTrue("Alter table with rename indexes " + alterStatus, alterStatus.isSuccess());

        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        TableDescription description = describeResult.getValue();
        Assert.assertEquals(2, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(1), "new_name", Arrays.asList("id", "code"), Collections.emptyList());

        alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .addRenameIndex("new_name", "idx2", true))
        ).join();
        Assert.assertTrue("Alter table with rename indexes " + alterStatus, alterStatus.isSuccess());

        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        description = describeResult.getValue();

        Assert.assertEquals(1, description.getIndexes().size());
        assertIndexSync(description.getIndexes().get(0), "idx2", Arrays.asList("id", "code"), Collections.emptyList());
    }

    private void assertColumn(TableColumn column, String name, Type type) {
        assertColumn(column, name, type, false, false);
    }

    private void assertColumn(TableColumn column, String name, Type type, boolean isNotNull, boolean hasDefaultValue) {
        Assert.assertEquals(name, column.getName());
        Assert.assertEquals(isNotNull ? type : type.makeOptional(), column.getType());
        Assert.assertEquals(EMPTY_FAMILY, column.getFamily());
//        Assert.assertEquals(isNotNull, column.isNotNull());
        Assert.assertEquals(hasDefaultValue, column.hasDefaultValue());
    }

    private void assertIndexSync(TableIndex index, String name, List<String> columns, List<String> dataColumns) {
        Assert.assertEquals(name, index.getName());
        Assert.assertEquals(TableIndex.Type.GLOBAL, index.getType());
        assertListEquals(columns, index.getColumns());
        assertListEquals(dataColumns, index.getDataColumns());
    }

    private void assertIndexAsync(TableIndex index, String name, List<String> columns, List<String> dataColumns) {
        Assert.assertEquals(name, index.getName());
        Assert.assertEquals(TableIndex.Type.GLOBAL_ASYNC, index.getType());
        assertListEquals(columns, index.getColumns());
        assertListEquals(dataColumns, index.getDataColumns());
    }

    private void assertListEquals(List<String> expected, List<String> values) {
        Assert.assertEquals(expected.size(), values.size());
        for (int idx = 0; idx < expected.size(); idx += 1) {
            Assert.assertEquals(expected.get(idx), values.get(idx));
        }
    }
}
