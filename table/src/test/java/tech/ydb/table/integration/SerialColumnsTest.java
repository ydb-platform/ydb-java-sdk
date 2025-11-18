package tech.ydb.table.integration;


import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.SequenceDescription;
import tech.ydb.table.description.TableColumn;
import tech.ydb.table.description.TableDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.query.DataQueryResult;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.transaction.TxControl;
import tech.ydb.table.values.PrimitiveType;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.test.junit4.GrpcTransportRule;

/**
 * @author Kirill Kurdyukov
 */
public class SerialColumnsTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydbTransport)
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();
    private final static String TABLE = "create_table_test";

    private String tablePath() {
        return ydbTransport.getDatabase() + "/" + TABLE;
    }

    @After
    public void dropTable() {
        ctx.supplyStatus(session -> session.dropTable(tablePath())).join();
    }

    @Test
    public void nullableSerialColumnTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addColumn(new TableColumn("id", PrimitiveType.Int32.makeOptional(), null, SequenceDescription.DEFAULT))
                .addNullableColumn("value", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32.makeOptional(), id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt32());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void addSmallSerialColumnTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addSmallSerialColumn("id")
                .addNullableColumn("value", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int16, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt16());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt16());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt16());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void addSerialColumnTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addSerialColumn("id")
                .addNullableColumn("value", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt32());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void addBigSerialColumnTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addBigSerialColumn("id")
                .addNullableColumn("value", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int64, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt64());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt64());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt64());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void smallSerialYqlTest() {
        ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE " + TABLE + "("
                + " id SmallSerial,"
                + " value Text,"
                + " PRIMARY KEY(id))"
        )).join().expectSuccess("cannot create table " + TABLE);

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int16, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt16());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt16());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt16());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void serialYqlTest() {
        ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE " + TABLE + "("
                + " id Serial,"
                + " value Text,"
                + " PRIMARY KEY(id))"
        )).join().expectSuccess("cannot create table " + TABLE);

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt32());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void bigSerialYqlTest() {
        ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE " + TABLE + "("
                + " id BigSerial,"
                + " value Text,"
                + " PRIMARY KEY(id))"
        )).join().expectSuccess("cannot create table " + TABLE);

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int64, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("_serial_column_id", sequence.getName());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt64());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt64());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt64());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void addColumnWithDefaultValueTest() {
        TableDescription createTable = TableDescription.newBuilder()
                .addNonnullColumn("id", PrimitiveType.Int32)
                .addNonnullColumn("v1", PrimitiveType.Text, PrimitiveValue.newText("non"))
                .addNullableColumn("v2", PrimitiveType.Text, PrimitiveValue.newText("null"))
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();

        Assert.assertEquals(3, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn v1 = desc.getColumns().get(1);
        TableColumn v2 = desc.getColumns().get(2);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32, id.getType());

        Assert.assertEquals("v1", v1.getName());
        Assert.assertNotNull(v1.getLiteralDefaultValue());
        Assert.assertNull(v1.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text, v1.getType());
        Assert.assertEquals(PrimitiveValue.newText("non"), v1.getLiteralDefaultValue());

        Assert.assertEquals("v2", v2.getName());
        Assert.assertNotNull(v2.getLiteralDefaultValue());
        Assert.assertNull(v2.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), v2.getType());
        Assert.assertEquals(PrimitiveValue.newText("null"), v2.getLiteralDefaultValue());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(""
                + "INSERT INTO " + TABLE + " (id) VALUES (1);"
                + "INSERT INTO " + TABLE + " (id, v1) VALUES (2, 'v1');"
                + "INSERT INTO " + TABLE + " (id, v2) VALUES (3, 'v2'), (4, NULL);"
                + "SELECT id, v1, v2 FROM " + TABLE + " ORDER BY id",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertEquals("null", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt32());
        Assert.assertEquals("v1", rs.getColumn("v1").getText());
        Assert.assertEquals("null", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertEquals("v2", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(4, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertFalse(rs.getColumn("v2").isOptionalItemPresent());

        Assert.assertFalse(rs.next());
    }

    @Test
    public void defaultValueYqlTest() {
        ctx.supplyStatus(session -> session.executeSchemeQuery(""
                + "CREATE TABLE " + TABLE + "("
                + " id Int32 NOT NULl,"
                + " v1 Text NOT NULL DEFAULT 'non',"
                + " v2 Text DEFAULT 'null',"
                + " PRIMARY KEY(id))"
        )).join().expectSuccess("cannot create table " + TABLE);

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();

        Assert.assertEquals(3, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn v1 = desc.getColumns().get(1);
        TableColumn v2 = desc.getColumns().get(2);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32, id.getType());

        Assert.assertEquals("v1", v1.getName());
        Assert.assertNotNull(v1.getLiteralDefaultValue());
        Assert.assertNull(v1.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text, v1.getType());
        Assert.assertEquals(PrimitiveValue.newText("non"), v1.getLiteralDefaultValue());

        Assert.assertEquals("v2", v2.getName());
        Assert.assertNotNull(v2.getLiteralDefaultValue());
        Assert.assertNull(v2.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), v2.getType());
        Assert.assertEquals(PrimitiveValue.newText("null"), v2.getLiteralDefaultValue());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(""
                + "INSERT INTO " + TABLE + " (id) VALUES (1);"
                + "INSERT INTO " + TABLE + " (id, v1) VALUES (2, 'v1');"
                + "INSERT INTO " + TABLE + " (id, v2) VALUES (3, 'v2'), (4, NULL);"
                + "SELECT id, v1, v2 FROM " + TABLE + " ORDER BY id",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertEquals("null", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(2, rs.getColumn("id").getInt32());
        Assert.assertEquals("v1", rs.getColumn("v1").getText());
        Assert.assertEquals("null", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(3, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertEquals("v2", rs.getColumn("v2").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(4, rs.getColumn("id").getInt32());
        Assert.assertEquals("non", rs.getColumn("v1").getText());
        Assert.assertFalse(rs.getColumn("v2").isOptionalItemPresent());

        Assert.assertFalse(rs.next());
    }

    @Test
    public void customSequenceDescriptionTest() {
        SequenceDescription sequenceConfig = SequenceDescription.newBuilder()
                .setCache(5L)
                .setMaxValue(Long.MAX_VALUE)
                .setMinValue(10L)
                .setName("custom_sequence")
                .setCycle(true)
                .setIncrement(2L)
                .setStartValue(12L)
                .build();

        TableDescription createTable = TableDescription.newBuilder()
                .addColumn(new TableColumn("id", PrimitiveType.Int32, null, sequenceConfig))
                .addNullableColumn("value", PrimitiveType.Text)
                .setPrimaryKey("id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        TableDescription desc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        Assert.assertEquals(2, desc.getColumns().size());

        TableColumn id = desc.getColumns().get(0);
        TableColumn value = desc.getColumns().get(1);

        Assert.assertEquals("id", id.getName());
        Assert.assertNull(id.getLiteralDefaultValue());
        Assert.assertNotNull(id.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Int32, id.getType());

        Assert.assertEquals("value", value.getName());
        Assert.assertNull(value.getLiteralDefaultValue());
        Assert.assertNull(value.getSequenceDescription());
        Assert.assertEquals(PrimitiveType.Text.makeOptional(), value.getType());

        SequenceDescription sequence = id.getSequenceDescription();
        Assert.assertNotNull(sequence);
        Assert.assertEquals("custom_sequence", sequence.getName());
        Assert.assertEquals(Long.valueOf(Long.MAX_VALUE), sequence.getMaxValue());
        Assert.assertEquals(Long.valueOf(10L), sequence.getMinValue());
        Assert.assertEquals(Long.valueOf(5L), sequence.getCache());
        Assert.assertTrue(sequence.getCycle());
        Assert.assertEquals(Long.valueOf(2L), sequence.getIncrement());
        Assert.assertEquals(Long.valueOf(12L), sequence.getStartValue());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (value) VALUES ('1'), ('2'), ('3') RETURNING id;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(12, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(14, rs.getColumn("id").getInt32());
        Assert.assertTrue(rs.next());
        Assert.assertEquals(16, rs.getColumn("id").getInt32());
        Assert.assertFalse(rs.next());
    }

    @Test
    public void copyTableWithSequenceAndDefaultValue() {
        SequenceDescription sequenceConfig = SequenceDescription.newBuilder()
                .setCache(5L)
                .setMaxValue(Long.MAX_VALUE)
                .setMinValue(12L)
                .setName("custom_sequence")
                .setCycle(true)
                .setIncrement(5L)
                .setStartValue(12L)
                .build();

        TableDescription createTable = TableDescription.newBuilder()
                .addNonnullColumn("version", PrimitiveType.Int8)
                .addColumn(new TableColumn("id", PrimitiveType.Int32, null, sequenceConfig))
                .addNullableColumn("value", PrimitiveType.Text, PrimitiveValue.newText("123"))
                .setPrimaryKeys("version", "id")
                .build();

        ctx.supplyStatus(session -> session.createTable(tablePath(), createTable)).join()
                .expectSuccess("cannot create table " + tablePath());

        DataQueryResult dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (version) VALUES (1), (1), (1) RETURNING version, id, value;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        ResultSetReader rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(12, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(17, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(22, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertFalse(rs.next());

        TableDescription tableDesc = ctx.supplyResult(session -> session.describeTable(tablePath())).join().getValue();
        ctx.supplyStatus(session -> session.dropTable(tablePath())).join().expectSuccess("cannot drop table " + TABLE);

        ctx.supplyStatus(session -> session.createTable(tablePath(), tableDesc)).join()
                .expectSuccess("cannot create table " + tablePath());

        dataQueryResult = ctx.supplyResult(session -> session.executeDataQuery(
                "INSERT INTO " + TABLE + " (version) VALUES (1), (1), (1) RETURNING version, id, value;",
                TxControl.serializableRw()
        )).join().getValue();

        Assert.assertEquals(1, dataQueryResult.getResultSetCount());
        rs = dataQueryResult.getResultSet(0);
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(12, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(17, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getColumn("version").getInt8());
        Assert.assertEquals(22, rs.getColumn("id").getInt32());
        Assert.assertEquals("123", rs.getColumn("value").getText());

        Assert.assertFalse(rs.next());
    }
}
