package tech.ydb.table.integration;

import org.junit.Assert;
import org.junit.ClassRule;

import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.SequenceDescription;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

import java.util.stream.Collectors;

/**
 * @author Kirill Kurdyukov
 */
public class CreateTableTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydbTransport)
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    @Test
    public void smallSerialTest() {
        String tablePath = ydbTransport.getDatabase() + "/small_serial_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addSmallSerialColumn("id")
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table with small serial " + createStatus, createStatus.isSuccess());

        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with small serial " + describeResult.getStatus(), describeResult.isSuccess());
        TableColumn tableColumn = describeResult.getValue().getColumns().get(0);
        Assert.assertNotNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("sequence_default", tableColumn.getSequenceDescription().getName());
        Assert.assertEquals(PrimitiveType.Int16, tableColumn.getType());
    }

    @Test
    public void serialTest() {
        String tablePath = ydbTransport.getDatabase() + "/serial_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addSerialColumn("id")
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(session ->
                session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table with serial " + createStatus, createStatus.isSuccess());

        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with serial " + describeResult.getStatus(), describeResult.isSuccess());
        TableColumn tableColumn = describeResult.getValue().getColumns().get(0);
        Assert.assertNotNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("sequence_default", tableColumn.getSequenceDescription().getName());
        Assert.assertEquals(PrimitiveType.Int32, tableColumn.getType());

        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void bigSerialTest() {
        String tablePath = ydbTransport.getDatabase() + "/big_serial_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addBigSerialColumn("id")
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(session ->
                session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table with big serial " + createStatus, createStatus.isSuccess());
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with big serial " + describeResult.getStatus(), describeResult.isSuccess());
        TableDescription description = describeResult.getValue();
        TableColumn tableColumn = description.getColumns().get(0);
        Assert.assertNotNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("sequence_default", tableColumn.getSequenceDescription().getName());
        Assert.assertEquals(PrimitiveType.Int64, tableColumn.getType());
        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void defaultValueTest() {
        String tablePath = ydbTransport.getDatabase() + "/default_value_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addColumn("id", PrimitiveType.Text, PrimitiveValue.newText("text"))
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table with indexes " + createStatus, createStatus.isSuccess());
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with indexes " + describeResult.getStatus(), describeResult.isSuccess());
        TableColumn tableColumn = describeResult.getValue().getColumns().get(0);
        Assert.assertNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("text", tableColumn.getLiteralDefaultValue().getText());
        Assert.assertEquals(PrimitiveType.Text, tableColumn.getType());
        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void customSequenceDescriptionTest() {
        String tablePath = ydbTransport.getDatabase() + "/default_value_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addBigSerialColumn("id", SequenceDescription.newBuilder()
                        .setCache(5L)
                        .setMaxValue((long) Integer.MAX_VALUE)
                        .setMinValue(10L)
                        .setName("custom_sequence_description")
                        .setCycle(true)
                        .setIncrement(2L)
                        .setStartValue(12L)
                        .build()
                )
                .setPrimaryKey("id")
                .build();

        Status createStatus = ctx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table with sequence description " + createStatus, createStatus.isSuccess());
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table with sequence description " + describeResult.getStatus(),
                describeResult.isSuccess());
        TableDescription description = describeResult.getValue();
        SequenceDescription sequenceDescription = description.getColumns().get(0).getSequenceDescription();
        Assert.assertNotNull(sequenceDescription);
        Assert.assertEquals("custom_sequence_description", sequenceDescription.getName());
        Assert.assertEquals(Integer.MAX_VALUE, sequenceDescription.getMaxValue().intValue());
        Assert.assertEquals(10L, (long) sequenceDescription.getMinValue());
        Assert.assertEquals(5L, (long) sequenceDescription.getCache());
        Assert.assertEquals(true, sequenceDescription.getCycle());
        Assert.assertEquals(2L, (long) sequenceDescription.getIncrement());
        Assert.assertEquals(12L, (long) sequenceDescription.getStartValue());
        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }

    @Test
    public void copyTableWithSequenceAndDefaultValue() {
        String tablePath = ydbTransport.getDatabase() + "/simple_table";

        TableDescription tableDescription = TableDescription.newBuilder()
                .addBigSerialColumn("id", SequenceDescription.newBuilder()
                        .setCache(5L)
                        .setMaxValue((long) Integer.MAX_VALUE)
                        .setMinValue(10L)
                        .setName("custom_sequence_description")
                        .setCycle(true)
                        .setIncrement(2L)
                        .setStartValue(12L)
                        .build()
                )
                .addColumn("name", PrimitiveType.Text, PrimitiveValue.newText("new_name"))
                .addNonnullColumn("another_name", PrimitiveType.Bool)
                .setPrimaryKey("id")
                .build();
        Status createStatus = ctx.supplyStatus(session -> session.createTable(tablePath, tableDescription)).join();
        Assert.assertTrue("Create table " + createStatus, createStatus.isSuccess());
        Result<TableDescription> describeResult = ctx.supplyResult(session -> session.describeTable(tablePath)).join();
        Assert.assertTrue("Describe table " + describeResult.getStatus(),
                describeResult.isSuccess());
        TableDescription description = describeResult.getValue();
        Assert.assertEquals(3, description.getColumns().size());
        SequenceDescription sequenceDescription = description.getColumns().get(0).getSequenceDescription();
        Assert.assertNotNull(sequenceDescription);
        Assert.assertEquals("custom_sequence_description", sequenceDescription.getName());
        Assert.assertEquals(Integer.MAX_VALUE, sequenceDescription.getMaxValue().intValue());
        Assert.assertEquals(10L, (long) sequenceDescription.getMinValue());
        Assert.assertEquals(5L, (long) sequenceDescription.getCache());
        Assert.assertEquals(true, sequenceDescription.getCycle());
        Assert.assertEquals(2L, (long) sequenceDescription.getIncrement());
        Assert.assertEquals(12L, (long) sequenceDescription.getStartValue());
        TableColumn tableColumn = describeResult.getValue().getColumns().get(1);
        Assert.assertNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("new_name", tableColumn.getLiteralDefaultValue().getText());
        Assert.assertEquals(PrimitiveType.Text, tableColumn.getType());
        TableColumn anotherTableColumn = describeResult.getValue().getColumns().get(2);
        Assert.assertNull(anotherTableColumn.getSequenceDescription());
        Assert.assertNull(anotherTableColumn.getLiteralDefaultValue());
        Assert.assertEquals("new_name", tableColumn.getLiteralDefaultValue().getText());
        Assert.assertEquals(PrimitiveType.Text, tableColumn.getType());

        TableDescription.Builder copyTableDescription = TableDescription.newBuilder();

        for (TableColumn columnTable : description.getColumns()) {
            if (columnTable.getSequenceDescription() != null) {
                copyTableDescription.addSequenceColumn("copy_" + columnTable.getName(),
                        columnTable.getType(), columnTable.getFamily(), columnTable.getSequenceDescription());

                continue;
            }

            copyTableDescription.addColumn(columnTable.getName(), columnTable.getType(), columnTable.getFamily(),
                    columnTable.getLiteralDefaultValue());
        }

        copyTableDescription.setPrimaryKeys(description.getPrimaryKeys().stream()
                .map(key -> "copy_" + key).collect(Collectors.toList()));

        createStatus = ctx.supplyStatus(session -> session
                .createTable(tablePath + "_copy", copyTableDescription.build())).join();
        Assert.assertTrue("Create copy table " + createStatus, createStatus.isSuccess());
        describeResult = ctx.supplyResult(session -> session.describeTable(tablePath + "_copy")).join();
        Assert.assertTrue("Describe copy table " + describeResult.getStatus(),
                describeResult.isSuccess());
        description = describeResult.getValue();
        Assert.assertEquals(3, description.getColumns().size());
        sequenceDescription = description.getColumns().get(0).getSequenceDescription();
        Assert.assertNotNull(sequenceDescription);
        Assert.assertEquals("custom_sequence_description", sequenceDescription.getName());
        Assert.assertEquals(Integer.MAX_VALUE, sequenceDescription.getMaxValue().intValue());
        Assert.assertEquals(10L, (long) sequenceDescription.getMinValue());
        Assert.assertEquals(5L, (long) sequenceDescription.getCache());
        Assert.assertEquals(true, sequenceDescription.getCycle());
        Assert.assertEquals(2L, (long) sequenceDescription.getIncrement());
        Assert.assertEquals(12L, (long) sequenceDescription.getStartValue());
        tableColumn = describeResult.getValue().getColumns().get(1);
        Assert.assertNull(tableColumn.getSequenceDescription());
        Assert.assertEquals("new_name", tableColumn.getLiteralDefaultValue().getText());
        Assert.assertEquals(PrimitiveType.Text, tableColumn.getType());
        anotherTableColumn = describeResult.getValue().getColumns().get(2);
        Assert.assertNull(anotherTableColumn.getSequenceDescription());
        Assert.assertNull(anotherTableColumn.getLiteralDefaultValue());
        Assert.assertEquals("new_name", tableColumn.getLiteralDefaultValue().getText());
        Assert.assertEquals(PrimitiveType.Text, tableColumn.getType());

        ctx.supplyStatus(session -> session.dropTable(tablePath)).join();
    }
}
