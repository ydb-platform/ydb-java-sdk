package tech.ydb.table.result.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import tech.ydb.ValueProtos;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.DecimalValue;
import tech.ydb.table.values.Type;
import tech.ydb.table.values.Value;
import tech.ydb.table.values.proto.ProtoType;
import tech.ydb.table.values.proto.ProtoValue;


/**
 * @author Sergey Polovko
 */
abstract class AbstractValueReader implements ValueReader {

    protected abstract ValueProtos.Type getProtoType();

    protected abstract ValueProtos.Value getProtoValue();
    protected abstract void setProtoValue(ValueProtos.Value value);

    private RuntimeException error(String methodName) {
        throw new IllegalStateException("cannot call " + methodName + ", actual type: " + ProtoType.toString(
            getProtoType()));
    }

    @Override
    public Type getType() {
        return ProtoType.fromPb(getProtoType());
    }

    @Override
    public Value<?> getValue() {
        return ProtoValue.fromPb(getType(), getProtoValue());
    }

    @Override
    public boolean getBool() {
        throw error("getBool");
    }

    @Override
    public byte getInt8() {
        throw error("getInt8");
    }

    @Override
    public int getUint8() {
        throw error("getUint8");
    }

    @Override
    public short getInt16() {
        throw error("getInt16");
    }

    @Override
    public int getUint16() {
        throw error("getUint16");
    }

    @Override
    public int getInt32() {
        throw error("getInt32");
    }

    @Override
    public long getUint32() {
        throw error("getUint32");
    }

    @Override
    public long getInt64() {
        throw error("getInt64");
    }

    @Override
    public long getUint64() {
        throw error("getUint64");
    }

    @Override
    public float getFloat() {
        throw error("getFloat");
    }

    @Override
    public double getDouble() {
        throw error("getDouble");
    }

    @Override
    public LocalDate getDate() {
        throw error("getDate");
    }

    @Override
    public LocalDateTime getDatetime() {
        throw error("getDatetime");
    }

    @Override
    public Instant getTimestamp() {
        throw error("getTimestamp");
    }

    @Override
    public Duration getInterval() {
        throw error("getInterval");
    }

    @Override
    public ZonedDateTime getTzDate() {
        throw error("getTzDate");
    }

    @Override
    public ZonedDateTime getTzDatetime() {
        throw error("getTzDatetime");
    }

    @Override
    public ZonedDateTime getTzTimestamp() {
        throw error("getTzTimestamp");
    }

    @Override
    public byte[] getBytes() {
        throw error("getBytes");
    }

    @Override
    public UUID getUuid() {
        throw error("getUuid");
    }

    @Override
    public String getText() {
        throw error("getText");
    }

    @Override
    public byte[] getYson() {
        throw error("getYson");
    }

    @Override
    public String getJson() {
        throw error("getJson");
    }

    @Override
    public String getJsonDocument() {
        throw error("getJsonDocument");
    }

    @Override
    public DecimalValue getDecimal() {
        throw error("getDecimal");
    }

    @Override
    public int getDictItemsCount() {
        throw error("getDictItemsCount");
    }

    @Override
    public ValueReader getDictKey(int index) {
        throw error("getDictKey");
    }

    @Override
    public ValueReader getDictValue(int index) {
        throw error("getDictValue");
    }

    @Override
    public int getListItemsCount() {
        throw error("getListItemsCount");
    }

    @Override
    public ValueReader getListItem(int index) {
        throw error("getListItem");
    }

    @Override
    public boolean isOptionalItemPresent() {
        throw error("isOptionalItemPresent");
    }

    @Override
    public ValueReader getOptionalItem() {
        throw error("getOptionalItem");
    }

    @Override
    public int getStructMembersCount() {
        throw error("getStructMembersCount");
    }

    @Override
    public String getStructMemberName(int index) {
        throw error("getStructMemberName");
    }

    @Override
    public ValueReader getStructMember(int index) {
        throw error("getStructMember");
    }

    @Override
    public ValueReader getStructMember(String name) {
        throw error("getStructMember");
    }

    @Override
    public int getTupleElementsCount() {
        throw error("getTupleElementsCount");
    }

    @Override
    public ValueReader getTupleElement(int index) {
        throw error("getTupleElement");
    }

    @Override
    public int getVariantTypeIndex() {
        throw error("getVariantTypeIndex");
    }

    @Override
    public ValueReader getVariantItem() {
        throw error("getVariantItem");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }
}
