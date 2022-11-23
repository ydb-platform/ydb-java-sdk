package tech.ydb.table.integration;

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
import tech.ydb.table.settings.AlterTableSettings;
import tech.ydb.table.settings.CreateTableSettings;
import tech.ydb.table.settings.DescribeTableSettings;
import tech.ydb.table.settings.PartitioningSettings;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.test.junit4.YdbInstanceRule;

/**
 *
 * @author Alexandr Gorshenin
 */
public class TablePartitioningSettingsTest {
    @ClassRule
    public final static YdbInstanceRule ydb = new YdbInstanceRule();

    private final String TABLE_NAME = "test1_table";

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydb.transport())
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    private final String tablePath = ydb.transport().getDatabase() + "/" + TABLE_NAME;


    @Test
    public void testPartitioningSettings() {
        PartitioningSettings initSettings = new PartitioningSettings();
        initSettings.setPartitionSize(2500);       // 2000 by default
        initSettings.setMinPartitionsCount(5);     // 1 by default
        initSettings.setMaxPartitionsCount(500);   // 50 by default
        initSettings.setPartitioningByLoad(true);  // false by default
        initSettings.setPartitioningBySize(true);  // true by default

        TableDescription tableDescription = TableDescription.newBuilder()
                .addNullableColumn("id", PrimitiveType.Uint64)
                .addNullableColumn("code", PrimitiveType.Text)
                .addNullableColumn("size", PrimitiveType.Float)
                .addNullableColumn("created", PrimitiveType.Timestamp)
                .addNullableColumn("data", PrimitiveType.Json)
                .setPrimaryKey("id")
                .setPartitioningSettings(initSettings)
                .build();

        PartitioningSettings updateSettings = new PartitioningSettings();
        updateSettings.setMinPartitionsCount(2);
        updateSettings.setMaxPartitionsCount(300);
        updateSettings.setPartitioningByLoad(false);

        PartitioningSettings mergedSettings = new PartitioningSettings();
        mergedSettings.setPartitionSize(2500);       // init value
        mergedSettings.setMinPartitionsCount(2);     // updated value
        mergedSettings.setMaxPartitionsCount(300);   // updated value
        mergedSettings.setPartitioningBySize(true);  // init value
        mergedSettings.setPartitioningByLoad(false); // updated value

        createTable(tableDescription);
        describeTable(tableDescription, initSettings, false);

        alterTable(updateSettings);
        describeTable(tableDescription, mergedSettings, true);

        dropTable();
    }

    private void createTable(TableDescription tableDescription) {
        Status status = ctx.supplyStatus(
                session -> session.createTable(tablePath, tableDescription, new CreateTableSettings())
        ).join();

        Assert.assertTrue("Create table with PartitioningSettings", status.isSuccess());
    }

    private void describeTable(
            TableDescription tableDescription,
            PartitioningSettings partitioning,
            boolean fetchStats) {

        Result<TableDescription> describeResult = ctx.supplyResult(session -> {
            if (!fetchStats) {
                return session.describeTable(tablePath);
            }

            DescribeTableSettings settings = new DescribeTableSettings();
            settings.setIncludeTableStats(true);
            settings.setIncludePartitionStats(true);
            return session.describeTable(tablePath, settings);
        }).join();
        Assert.assertTrue("Describe table", describeResult.isSuccess());

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

        PartitioningSettings settings = description.getPartitioningSettings();
        Assert.assertNotNull("Table partitioning settings", settings);

        assert (settings != null);
        Assert.assertEquals("Partition Size Mb",
                partitioning.getPartitionSizeMb(), settings.getPartitionSizeMb());
        Assert.assertEquals("Min Partitions Count",
                partitioning.getMinPartitionsCount(), settings.getMinPartitionsCount());
        Assert.assertEquals("Max Partitions Count",
                partitioning.getMaxPartitionsCount(), settings.getMaxPartitionsCount());
        Assert.assertEquals("Partitioning By Load",
                partitioning.getPartitioningByLoad(), settings.getPartitioningByLoad());
        Assert.assertEquals("Partitioning By Size",
                partitioning.getPartitioningBySize(), settings.getPartitioningBySize());

        if (fetchStats) {
            Assert.assertNotNull("Table description table stats are not null", description.getTableStats());
            Assert.assertFalse("Table description partition stats are not empty",
                    description.getPartitionStats().isEmpty());
        } else {
            Assert.assertNull("Table description table stats are null", description.getTableStats());
            Assert.assertTrue("Table description partition stats are empty",
                    description.getPartitionStats().isEmpty());
        }
    }

    private void alterTable(PartitioningSettings partitioning) {
        AlterTableSettings settings = new AlterTableSettings();
        settings.setPartitioningSettings(partitioning);

        Status status = ctx.supplyStatus(
                session -> session.alterTable(tablePath, settings)
        ).join();

        Assert.assertTrue("Alter table with PartitioningSettings", status.isSuccess());
    }

    private void dropTable() {
        Status status = ctx.supplyStatus(
                session -> session.dropTable(tablePath)
        ).join();

        Assert.assertTrue("Drop table with PartitioningSettings", status.isSuccess());
    }

}
