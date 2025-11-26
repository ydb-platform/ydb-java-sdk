package tech.ydb.table.result.impl;

import java.util.Arrays;
import java.util.NoSuchElementException;

import com.google.protobuf.NullValue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ResultSetReader;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.OptionalValue;
import tech.ydb.table.values.PrimitiveValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OptionalReaderTest {
    private final static String NOT_NULL = "NOT_NULL_VALUE";

    private final static ValueProtos.Type TEXT_TYPE = ValueProtos.Type.newBuilder()
            .setTypeId(ValueProtos.Type.PrimitiveTypeId.UTF8)
            .build();

    private final static ValueProtos.Type OPTIONAL_TYPE = ValueProtos.Type.newBuilder()
            .setOptionalType(ValueProtos.OptionalType.newBuilder()
                    .setItem(TEXT_TYPE)
                    .build()
            ).build();

    private final static ValueProtos.Type DOUBLE_TYPE = ValueProtos.Type.newBuilder()
            .setOptionalType(ValueProtos.OptionalType.newBuilder()
                    .setItem(OPTIONAL_TYPE)
                    .build()
            ).build();

    @Test
    public void parseNotNullValue() {
        ValueProtos.Value pbValue = ValueProtos.Value.newBuilder()
                .setTextValue(NOT_NULL)
                .build();

        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(ValueProtos.Column.newBuilder().setName("value").setType(OPTIONAL_TYPE).build())
            .addRows(ValueProtos.Value.newBuilder().addItems(pbValue).build())
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertTrue("read next row", reader.next());

        ValueReader optionalReader = reader.getColumn(0);
        check("not-empty optional reader", optionalReader).isNotNull()
                .hasOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasTextValue(NOT_NULL);

        Value<?> optional = optionalReader.getValue();
        check("not-empty optional value", optional).isNotNull()
                .isOptionalValue()
                .hasItem()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasValuePb(pbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");

        ValueReader valueReader = optionalReader.getOptionalItem();
        check("value reader", valueReader).isNotNull()
                .hasType(Type.Kind.PRIMITIVE, TEXT_TYPE)
                .hasTextValue(NOT_NULL);
        checkIllegalStateException("value reader is not optional", valueReader::isOptionalItemPresent)
                .hasInMessage("cannot call isOptionalItemPresent, actual type: type_id: UTF8");

        Value<?> value = valueReader.getValue();
        check("value", value).isNotNull()
                .isPrimitiveValue()
                .hasType(Type.Kind.PRIMITIVE, TEXT_TYPE)
                .hasValuePb(pbValue)
                .hasTextValue(NOT_NULL);
        checkCastException("value is not optional", value::asOptional)
                .hasOneOfMessage("PrimitiveValue$Text cannot be cast ", "PrimitiveValue$Text incompatible with");

        Assert.assertEquals("optional item equals value", value, optional.asOptional().get());
    }

    @Test
    public void parseNullValue() {
        ValueProtos.Value pbValue = ValueProtos.Value.newBuilder()
                .setNullFlagValue(NullValue.NULL_VALUE)
                .build();

        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(ValueProtos.Column.newBuilder().setName("value").setType(OPTIONAL_TYPE).build())
            .addRows(ValueProtos.Value.newBuilder().addItems(pbValue).build())
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertTrue("read next row", reader.next());

        ValueReader optionalReader = reader.getColumn(0);
        check("empty optional reader", optionalReader).isNotNull()
                .hasNotOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasTextValue(""); // grpc protobuf return empty string instead of null

        Value<?> optional = optionalReader.getValue();
        check("empty optional value", optional).isNotNull()
                .isOptionalValue()
                .hasNotItem()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasValuePb(pbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");
        checkNoSuchElementException("optional has not value", () -> optional.asOptional().get())
                .hasInMessage("No value present");

        ValueReader valueReader = optionalReader.getOptionalItem();
        check("value reader", valueReader).isNull();
    }

    @Test
    public void parseDoubleNullValue() {
        ValueProtos.Value pbValue = ValueProtos.Value.newBuilder()
                .setNullFlagValue(NullValue.NULL_VALUE)
                .build();

        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(ValueProtos.Column.newBuilder().setName("value").setType(DOUBLE_TYPE).build())
            .addRows(ValueProtos.Value.newBuilder().addItems(pbValue).build())
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertTrue("read next row", reader.next());

        ValueReader optionalReader = reader.getColumn(0);
        check("empty optional reader", optionalReader).isNotNull()
                .hasNotOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE);
        checkIllegalStateException("double optional reader hasn't text", optionalReader::getText)
                .hasInMessage("cannot call getText, actual type: optional_type");

        Value<?> optional = optionalReader.getValue();
        check("empty optional value", optional).isNotNull()
                .isOptionalValue()
                .hasNotItem()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE)
                .hasValuePb(pbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");
        checkNoSuchElementException("optional has not value", () -> optional.asOptional().get())
                .hasInMessage("No value present");

        ValueReader valueReader = optionalReader.getOptionalItem();
        check("value reader", valueReader).isNull();
    }

    @Test
    public void parseDoubleInnerNullValue() {
        ValueProtos.Value innerPbValue = ValueProtos.Value.newBuilder()
                .setNullFlagValue(NullValue.NULL_VALUE)
                .build();

        ValueProtos.Value pbValue = ValueProtos.Value.newBuilder()
                .setNestedValue(innerPbValue)
                .build();

        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(ValueProtos.Column.newBuilder().setName("value").setType(DOUBLE_TYPE).build())
            .addRows(ValueProtos.Value.newBuilder().addItems(pbValue).build())
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertTrue("read next row", reader.next());

        ValueReader optionalReader = reader.getColumn(0);
        check("double optional reader", optionalReader).isNotNull()
                .hasOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE);
        checkIllegalStateException("double optional reader hasn't text", optionalReader::getText)
                .hasInMessage("cannot call getText, actual type: optional_type");

        Value<?> optional = optionalReader.getValue();
        check("double optional value", optional).isNotNull()
                .isOptionalValue()
                .hasItem()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE)
                .hasValuePb(pbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");

        ValueReader innerOptionalReader = optionalReader.getOptionalItem();
        check("inner optional reader", innerOptionalReader).isNotNull()
                .hasNotOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasTextValue(""); // grpc protobuf return empty string instead of null

        Value<?> innerOptional = innerOptionalReader.getValue();
        check("inner optional value", innerOptional).isNotNull()
                .isOptionalValue()
                .hasNotItem()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasValuePb(innerPbValue);
        checkCastException("inner optional is not primitive", innerOptional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");
        checkNoSuchElementException("inner optional has not value", () -> innerOptional.asOptional().get())
                .hasInMessage("No value present");

        ValueReader valueReader = innerOptionalReader.getOptionalItem();
        check("value reader", valueReader).isNull();
    }

    @Test
    public void parseDoubleNotNullValue() {
        ValueProtos.Value innerPbValue = ValueProtos.Value.newBuilder()
                .setTextValue(NOT_NULL)
                .build();

        ValueProtos.Value pbValue = ValueProtos.Value.newBuilder()
                .setNestedValue(innerPbValue)
                .build();

        ValueProtos.ResultSet resultSet = ValueProtos.ResultSet.newBuilder()
            .addColumns(ValueProtos.Column.newBuilder().setName("value").setType(DOUBLE_TYPE).build())
            .addRows(ValueProtos.Value.newBuilder().addItems(pbValue).build())
            .build();

        ResultSetReader reader = ProtoValueReaders.forResultSet(resultSet);
        Assert.assertTrue("read next row", reader.next());

        ValueReader optionalReader = reader.getColumn(0);
        check("double optional reader", optionalReader).isNotNull()
                .hasOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE);
        checkIllegalStateException("double optional reader hasn't text", optionalReader::getText)
                .hasInMessage("cannot call getText, actual type: optional_type");

        Value<?> optional = optionalReader.getValue();
        check("double optional value", optional).isNotNull()
                .isOptionalValue()
                .hasItem()
                .hasType(Type.Kind.OPTIONAL, DOUBLE_TYPE)
                .hasValuePb(pbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");

        ValueReader innerOptionalReader = optionalReader.getOptionalItem();
        check("inner optional reader", innerOptionalReader).isNotNull()
                .hasOptionalItemPresent()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasTextValue(NOT_NULL);

        Value<?> innerOptional = innerOptionalReader.getValue();
        check("inner optional value", innerOptional).isNotNull()
                .isOptionalValue()
                .hasItem()
                .hasType(Type.Kind.OPTIONAL, OPTIONAL_TYPE)
                .hasValuePb(innerPbValue);
        checkCastException("optional is not primitive", optional::asData)
                .hasOneOfMessage("OptionalValue cannot be cast", "OptionalValue incompatible with");

        ValueReader valueReader = innerOptionalReader.getOptionalItem();
        check("inner value reader", valueReader).isNotNull()
                .hasType(Type.Kind.PRIMITIVE, TEXT_TYPE)
                .hasTextValue(NOT_NULL);
        checkIllegalStateException("inner value reader is not optional", valueReader::isOptionalItemPresent)
                .hasInMessage("cannot call isOptionalItemPresent, actual type: type_id: UTF8");

        Value<?> value = valueReader.getValue();
        check("inner value", value).isNotNull()
                .isPrimitiveValue()
                .hasType(Type.Kind.PRIMITIVE, TEXT_TYPE)
                .hasValuePb(innerPbValue)
                .hasTextValue(NOT_NULL);
        checkCastException("inner value is not optional", value::asOptional)
                .hasOneOfMessage("PrimitiveValue$Text cannot be cast ", "PrimitiveValue$Text incompatible with");

        Assert.assertEquals("inner optional item equals inner value", value, innerOptional.asOptional().get());
    }

    private ReaderChecker check(String name, ValueReader reader) {
        return new ReaderChecker(name, reader);
    }

    private ValueChecker check(String name, Value<?> value) {
        return new ValueChecker(name, value);
    }

    private ExceptionChecker<ClassCastException> checkCastException(String message, ThrowingRunnable runnable) {
        ClassCastException exception = Assert.assertThrows(message, ClassCastException.class, runnable);
        return new ExceptionChecker<>(exception);
    }

    private ExceptionChecker<IllegalStateException> checkIllegalStateException(
            String message, ThrowingRunnable runnable) {
        IllegalStateException exception = Assert.assertThrows(message, IllegalStateException.class, runnable);
        return new ExceptionChecker<>(exception);
    }

    private ExceptionChecker<NoSuchElementException> checkNoSuchElementException(
            String message, ThrowingRunnable runnable) {
        NoSuchElementException exception = Assert.assertThrows(message, NoSuchElementException.class, runnable);
        return new ExceptionChecker<>(exception);
    }

    private static class ReaderChecker {
        private final String name;
        private final ValueReader reader;

        public ReaderChecker(String name, ValueReader reader) {
            this.name = name;
            this.reader = reader;
        }

        public void isNull() {
            Assert.assertNull(name + " is null", reader);
        }

        public ReaderChecker isNotNull() {
            Assert.assertNotNull(name + " is not null", reader);
            return this;
        }

        public ReaderChecker hasOptionalItemPresent() {
            Assert.assertTrue("check " + name + " has optional item", reader.isOptionalItemPresent());
            return this;
        }

        public ReaderChecker hasNotOptionalItemPresent() {
            Assert.assertFalse("check " + name + " has optional item", reader.isOptionalItemPresent());
            return this;
        }

        public ReaderChecker hasType(Type.Kind kind, ValueProtos.Type pb) {
            Assert.assertEquals("check " + name + " type kind", kind, reader.getType().getKind());
            Assert.assertEquals("check " + name + " type pb", pb, reader.getType().toPb());
            return this;
        }

        public ReaderChecker hasTextValue(String value) {
            Assert.assertEquals("check " + name + " text value", value, reader.getText());
            return this;
        }
    }

    private static class ValueChecker {
        private final String name;
        private final Value<?> value;

        public ValueChecker(String name, Value<?> value) {
            this.name = name;
            this.value = value;
        }

        public void isNull() {
            Assert.assertNull(name + " is null", value);
        }

        public ValueChecker isNotNull() {
            Assert.assertNotNull(name + " is not null", value);
            return this;
        }

        public ValueChecker isOptionalValue() {
            Assert.assertTrue(name + " is optional", value instanceof OptionalValue);
            Assert.assertFalse(name + " isn't primitive", value instanceof PrimitiveValue);
            return this;
        }

        public ValueChecker isPrimitiveValue() {
            Assert.assertFalse(name + " isn't optional", value instanceof OptionalValue);
            Assert.assertTrue(name + " is primitive", value instanceof PrimitiveValue);
            return this;
        }

        private ValueChecker hasNotItem() {
            Assert.assertFalse("check " + name + " hasn't item", value.asOptional().isPresent());
            return this;
        }

        private ValueChecker hasItem() {
            Assert.assertTrue("check " + name + " has item", value.asOptional().isPresent());
            return this;
        }

        public ValueChecker hasType(Type.Kind kind, ValueProtos.Type pb) {
            Assert.assertEquals("check " + name + " type kind", kind, value.getType().getKind());
            Assert.assertEquals("check " + name + " type pb", pb, value.getType().toPb());
            return this;
        }

        public ValueChecker hasTextValue(String text) {
            Assert.assertEquals("check " + name + " text value", text, value.asData().getText());
            return this;
        }

        public ValueChecker hasValuePb(ValueProtos.Value pb) {
            Assert.assertEquals("check " + name + " value pb", pb, value.toPb());
            return this;
        }

    }

    private static class ExceptionChecker<T extends Exception> {
        private final T exception;

        public ExceptionChecker(T exception) {
            this.exception = exception;
        }

        private void hasInMessage(String part) {
            String message = exception.getMessage();
            String info = "cast exception message '" + message + "' hasn't '" + part + "'";
            Assert.assertTrue(info, message.contains(part));
        }

        private void hasOneOfMessage(String... parts) {
            String message = exception.getMessage();

            for (String part : parts) {
                if (message.contains(part)) {
                    return;
                }
            }

            String info = "cast exception message '" + message + "' hasn't any of'" + Arrays.toString(parts) + "'";
            Assert.fail(info);
        }
    }
}
