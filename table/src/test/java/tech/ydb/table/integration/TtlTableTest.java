package tech.ydb.table.integration;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.description.TableTtl;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class TtlTableTest {
    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final String TABLE_NAME = "ttl_table_test";

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
                .addNullableColumn("date", PrimitiveType.Datetime)
                .addNullableColumn("value", PrimitiveType.Uint64)
                .setPrimaryKey("id")
                .setTtlSettings(TableTtl.dateTimeColumn("date", 15).withRunIntervalSeconds(7200))
                .build();

        Status createStatus = ctx.supplyStatus(
                session -> session.createTable(tablePath, createTableDesc, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table ttl " + createStatus, createStatus.isSuccess());

        // --------------------- describe table after creating -----------------------------
        Result<TableDescription> describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with ttl " + describeResult.getStatus(), describeResult.isSuccess());

        TableTtl ttl = describeResult.getValue().getTableTtl();

        Assert.assertNotNull(ttl);
        Assert.assertEquals(TableTtl.TtlMode.DATE_TYPE_COLUMN, ttl.getTtlMode());
        Assert.assertEquals("date", ttl.getDateTimeColumn());
        Assert.assertEquals(Integer.valueOf(15), ttl.getExpireAfterSeconds());
        Assert.assertEquals(TableTtl.TtlUnit.UNSPECIFIED, ttl.getTtlUnit());
        Assert.assertEquals(Integer.valueOf(7200), ttl.getRunIntervaelSeconds());

        // --------------------- alter table with changing ttl -----------------------------

        Status alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .setTableTtl(TableTtl.valueSinceUnixEpoch("value", TableTtl.TtlUnit.SECONDS, 60))
                )
        ).join();
        Assert.assertTrue("Alter table with ttl " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        ttl = describeResult.getValue().getTableTtl();

        Assert.assertNotNull(ttl);
        Assert.assertEquals(TableTtl.TtlMode.VALUE_SINCE_UNIX_EPOCH, ttl.getTtlMode());
        Assert.assertEquals("value", ttl.getDateTimeColumn());
        Assert.assertEquals(Integer.valueOf(60), ttl.getExpireAfterSeconds());
        Assert.assertEquals(TableTtl.TtlUnit.SECONDS, ttl.getTtlUnit());
        Assert.assertEquals(Integer.valueOf(0), ttl.getRunIntervaelSeconds());

        // --------------------- alter table with dropping ttl -----------------------------
        alterStatus = ctx.supplyStatus(
                session -> session.alterTable(tablePath, new AlterTableSettings()
                        .setTableTtl(TableTtl.notSet())
                )
        ).join();
        Assert.assertTrue("Alter table with dropping ttl " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after first altering -----------------------------
        describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        ttl = describeResult.getValue().getTableTtl();

        Assert.assertNotNull(ttl);
        Assert.assertEquals(TableTtl.TtlMode.NOT_SET, ttl.getTtlMode());
        Assert.assertEquals("", ttl.getDateTimeColumn());
        Assert.assertEquals(Integer.valueOf(0), ttl.getExpireAfterSeconds());
        Assert.assertEquals(TableTtl.TtlUnit.UNSPECIFIED, ttl.getTtlUnit());
        Assert.assertNull(ttl.getRunIntervaelSeconds());
    }

    @Test
    public void noTtlTableTest() {
        // --------------------- create table -----------------------------
        TableDescription createTableDesc = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("date", PrimitiveType.Datetime)
                .addNullableColumn("value", PrimitiveType.Uint64)
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(
                session -> session.createTable(tablePath, createTableDesc, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table ttl " + createStatus, createStatus.isSuccess());

        // --------------------- describe table after creating -----------------------------
        Result<TableDescription> describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with ttl " + describeResult.getStatus(), describeResult.isSuccess());

        TableTtl ttl = describeResult.getValue().getTableTtl();

        Assert.assertNotNull(ttl);
        Assert.assertEquals(TableTtl.TtlMode.NOT_SET, ttl.getTtlMode());
        Assert.assertEquals("", ttl.getDateTimeColumn());
        Assert.assertEquals(Integer.valueOf(0), ttl.getExpireAfterSeconds());
        Assert.assertEquals(TableTtl.TtlUnit.UNSPECIFIED, ttl.getTtlUnit());
        Assert.assertNull(ttl.getRunIntervaelSeconds());
    }
}
