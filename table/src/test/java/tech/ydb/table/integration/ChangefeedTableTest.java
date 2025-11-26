package tech.ydb.table.integration;

import java.time.Duration;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.TableClient;
import tech.ydb.table.description.ChangefeedDescription;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.Changefeed;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ChangefeedTableTest {
    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final String TABLE_NAME = "table_changefeed_test";

    private final TableClient tableClient = TableClient.newClient(ydbTransport).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    private final String tablePath = ydbTransport.getDatabase() + "/" + TABLE_NAME;

    @After
    public void dropTable() {
        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
        tableClient.close();
    }

    @Test
    public void tableChangefeedsErrorsTest() {
        TableDescription tableDescription = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("code", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(
                session -> session.createTable(tablePath, tableDescription, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table " + createStatus, createStatus.isSuccess());

        Status alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .addChangefeed(Changefeed.newBuilder("change1").build())
                .addChangefeed(Changefeed.newBuilder("change2").build())
        )).join();

        Assert.assertFalse("Alter table add multiple changefeeds", alterStatus.isSuccess());
        Assert.assertEquals("Status{code = UNSUPPORTED(code=400180), issues = "
                + "[Only one changefeed can be added by one operation (S_ERROR)]}", alterStatus.toString());

        alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .dropChangefeed("change1")
                .addChangefeed(Changefeed.newBuilder("test1")
                        .withInitialScan(false)
                        .withMode(Changefeed.Mode.UPDATES)
                        .withVirtualTimestamps(false)
                        .build())
        )).join();
        Assert.assertFalse("Alter table mixed alter", alterStatus.isSuccess());
        Assert.assertEquals("Status{code = UNSUPPORTED(code=400180), issues = "
                + "[Mixed alter is unsupported (S_ERROR)]}", alterStatus.toString());

        alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .dropChangefeed("change1")
                .dropChangefeed("change2")
        )).join();
        Assert.assertFalse("Alter table drop multiple changefeeds", alterStatus.isSuccess());
        Assert.assertEquals("Status{code = UNSUPPORTED(code=400180), issues = "
                + "[Only one changefeed can be removed by one operation (S_ERROR)]}", alterStatus.toString());
    }

    @Test
    public void tableChangefeedsTest() {
        // --------------------- create tables -----------------------------
        TableDescription tableDescription = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("code", PrimitiveType.Text)
                .addNullableColumn("size", PrimitiveType.Float)
                .addNullableColumn("created", PrimitiveType.Timestamp)
                .addNullableColumn("data", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(
                session -> session.createTable(tablePath, tableDescription, new CreateTableSettings())
        ).join();
        Assert.assertTrue("Create table " + createStatus, createStatus.isSuccess());

        // --------------------- describe table after creating -----------------------------
        Result<TableDescription> describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table " + describeResult.getStatus(), describeResult.isSuccess());

        // No changefeeds
        Assert.assertTrue(describeResult.getValue().getChangefeeds().isEmpty());

        // --------------------- alter table with add changefeeds -----------------------------
        // only one changefeed can be added  by one operation

        Status alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .addChangefeed(Changefeed.newBuilder("change1").build())
        )).join();
        Assert.assertTrue("Alter table changefeed 1 " + alterStatus, alterStatus.isSuccess());

        alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .addChangefeed(Changefeed.newBuilder("change2")
                        .withFormat(Changefeed.Format.JSON)
                        .withInitialScan(true)
                        .withMode(Changefeed.Mode.NEW_AND_OLD_IMAGES)
                        .withVirtualTimestamps(true)
                        .withRetentionPeriod(Duration.ofDays(5))
                        .withResolvedTimestampsInterval(Duration.ofSeconds(3))
                        .build())
        )).join();
        Assert.assertTrue("Alter table changefeed 2 " + alterStatus, alterStatus.isSuccess());

        // --------------------- describe table after create new changefeeds -----------------------------
        describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        List<ChangefeedDescription> changefeeds = describeResult.getValue().getChangefeeds();

        Assert.assertEquals(2, changefeeds.size());

        ChangefeedDescription ch1 = changefeeds.get(0);
        Assert.assertEquals("change1", ch1.getName());
        Assert.assertEquals(Changefeed.Mode.KEYS_ONLY, ch1.getMode());
        Assert.assertEquals(Changefeed.Format.JSON, ch1.getFormat());
        Assert.assertEquals(ChangefeedDescription.State.ENABLED, ch1.getState());
        Assert.assertFalse(ch1.hasVirtualTimestamps());
        Assert.assertEquals(ch1.getResolvedTimestampsInterval(), Duration.ZERO);  // zero is default when not specified
        Assert.assertEquals(
                "Changefeed['change1']{state=ENABLED, format=JSON, mode=KEYS_ONLY, virtual timestamps=false, resolved timestamps=PT0S}",
                ch1.toString()
        );

        ChangefeedDescription ch2 = changefeeds.get(1);
        Assert.assertEquals("change2", ch2.getName());
        Assert.assertEquals(Changefeed.Mode.NEW_AND_OLD_IMAGES, ch2.getMode());
        Assert.assertEquals(Changefeed.Format.JSON, ch2.getFormat());
        Assert.assertTrue(ch2.hasVirtualTimestamps());
        Assert.assertEquals(ch2.getResolvedTimestampsInterval(), Duration.ofSeconds(3));

        // State may be flaky
        Assert.assertTrue(ch2.getState() == ChangefeedDescription.State.INITIAL_SCAN ||
                ch2.getState() == ChangefeedDescription.State.ENABLED);

        // --------------------- drop one changefeed and add another -------------------------------------
        alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .dropChangefeed("change2")
        )).join();
        Assert.assertTrue("Alter table changefeed 3 " + alterStatus, alterStatus.isSuccess());

        alterStatus = ctx.supplyStatus(session -> session.alterTable(tablePath, new AlterTableSettings()
                .addChangefeed(Changefeed.fromDescription(ch2)
                        .withInitialScan(false)
                        .withMode(Changefeed.Mode.UPDATES)
                        .withVirtualTimestamps(false)
                        .build())
        )).join();
        Assert.assertTrue("Alter table changefeed 3 " + alterStatus, alterStatus.isSuccess());

        describeResult = ctx.supplyResult(session ->session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table after altering " + describeResult.getStatus(), describeResult.isSuccess());

        changefeeds = describeResult.getValue().getChangefeeds();

        Assert.assertEquals(2, changefeeds.size());

        ch1 = changefeeds.get(0);
        Assert.assertEquals(ch1, changefeeds.get(0));
        Assert.assertEquals(Changefeed.Mode.KEYS_ONLY, ch1.getMode());
        Assert.assertEquals(Changefeed.Format.JSON, ch1.getFormat());
        Assert.assertEquals(ChangefeedDescription.State.ENABLED, ch1.getState());
        Assert.assertFalse(ch1.hasVirtualTimestamps());

        ChangefeedDescription ch3 = changefeeds.get(1);
        Assert.assertNotEquals(ch2, ch3);
        Assert.assertEquals("change2", ch3.getName());
        Assert.assertEquals(Changefeed.Mode.UPDATES, ch3.getMode());
        Assert.assertEquals(Changefeed.Format.JSON, ch3.getFormat());
        Assert.assertFalse(ch3.hasVirtualTimestamps());
    }
}
