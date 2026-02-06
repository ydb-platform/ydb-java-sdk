package tech.ydb.table.result.impl;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import tech.ydb.proto.ValueProtos;
import tech.ydb.table.result.ValueReader;
import tech.ydb.table.values.DecimalValue;


/**
 * @author Sergey Polovko
 */
final class ProtoOptionalValueReader extends AbstractValueReader {

    private final ValueProtos.Type type;
    private final AbstractValueReader valueReader;
    private boolean present = false;
    private ValueProtos.Value value;

    ProtoOptionalValueReader(ValueProtos.Type type, AbstractValueReader itemReader) {
        this.type = type;
        this.valueReader = itemReader;
    }

    @Override
    protected ValueProtos.Type getProtoType() {
        return type;
    }

    @Override
    protected ValueProtos.Value getProtoValue() {
        return value;
    }

    @Override
    public boolean isOptionalItemPresent() {
        return present;
    }

    @Override
    public ValueReader getOptionalItem() {
        return present ? valueReader : null;
    }

    @Override
    protected void setProtoValue(ValueProtos.Value value) {
        this.value = value;
        switch (value.getValueCase()) {
            case NESTED_VALUE:
                present = true;
                valueReader.setProtoValue(value.getNestedValue());
                break;
            case NULL_FLAG_VALUE:
                present = false;
                valueReader.setProtoValue(value);
                break;
            default:
                present = true;
                valueReader.setProtoValue(value);
                break;
        }
    }

    private void checkNotNull(String methodName) {
        if (!present) {
            throw new NullPointerException("cannot call " + methodName + " for NULL value");
        }
    }

    @Override
    public boolean getBool() {
        boolean v = valueReader.getBool();
        checkNotNull("getBool");
        return v;
    }

    @Override
    public byte getInt8() {
        byte v = valueReader.getInt8();
        checkNotNull("getInt8");
        return v;
    }

    @Override
    public int getUint8() {
        int v = valueReader.getUint8();
        checkNotNull("getUint8");
        return v;
    }

    @Override
    public short getInt16() {
        short v = valueReader.getInt16();
        checkNotNull("getInt16");
        return v;
    }

    @Override
    public int getUint16() {
        int v = valueReader.getUint16();
        checkNotNull("getUint16");
        return v;
    }

    @Override
    public int getInt32() {
        int v = valueReader.getInt32();
        checkNotNull("getInt32");
        return v;
    }

    @Override
    public long getUint32() {
        long v = valueReader.getUint32();
        checkNotNull("getUint32");
        return v;
    }

    @Override
    public long getInt64() {
        long v = valueReader.getInt64();
        checkNotNull("getInt64");
        return v;
    }

    @Override
    public long getUint64() {
        long v = valueReader.getUint64();
        checkNotNull("getUint64");
        return v;
    }

    @Override
    public float getFloat() {
        float v = valueReader.getFloat();
        checkNotNull("getFloat");
        return v;
    }

    @Override
    public double getDouble() {
        double v = valueReader.getDouble();
        checkNotNull("getDouble");
        return v;
    }

    @Override
    public LocalDate getDate() {
        LocalDate v = valueReader.getDate();
        return present ? v : null;
    }

    @Override
    public LocalDateTime getDatetime() {
        LocalDateTime v = valueReader.getDatetime();
        return present ? v : null;
    }

    @Override
    public Instant getTimestamp() {
        Instant v = valueReader.getTimestamp();
        return present ? v : null;
    }

    @Override
    public Duration getInterval() {
        Duration v = valueReader.getInterval();
        return present ? v : null;
    }

    @Override
    public LocalDate getDate32() {
        LocalDate v = valueReader.getDate32();
        return present ? v : null;
    }

    @Override
    public LocalDateTime getDatetime64() {
        LocalDateTime v = valueReader.getDatetime64();
        return present ? v : null;
    }

    @Override
    public Instant getTimestamp64() {
        Instant v = valueReader.getTimestamp64();
        return present ? v : null;
    }

    @Override
    public Duration getInterval64() {
        Duration v = valueReader.getInterval64();
        return present ? v : null;
    }

    @Override
    public ZonedDateTime getTzDate() {
        ZonedDateTime v = valueReader.getTzDate();
        return present ? v : null;
    }

    @Override
    public ZonedDateTime getTzDatetime() {
        ZonedDateTime v = valueReader.getTzDatetime();
        return present ? v : null;
    }

    @Override
    public ZonedDateTime getTzTimestamp() {
        ZonedDateTime v = valueReader.getTzTimestamp();
        return present ? v : null;
    }

    @Override
    public byte[] getBytes() {
        byte[] v = valueReader.getBytes();
        return present ? v : null;
    }

    @Override
    public String getBytesAsString(Charset charset) {
        String v = valueReader.getBytesAsString(charset);
        return present ? v : null;
    }

    @Override
    public UUID getUuid() {
        UUID v = valueReader.getUuid();
        return present ? v : null;
    }

    @Override
    public String getText() {
        String v = valueReader.getText();
        return present ? v : null;
    }

    @Override
    public byte[] getYson() {
        byte[] v = valueReader.getYson();
        return present ? v : null;
    }

    @Override
    public String getJson() {
        String v = valueReader.getJson();
        return present ? v : null;
    }

    @Override
    public String getJsonDocument() {
        String v = valueReader.getJsonDocument();
        return present ? v : null;
    }

    @Override
    public DecimalValue getDecimal() {
        DecimalValue v = valueReader.getDecimal();
        return present ? v : null;
    }

    @Override
    public int getDictItemsCount() {
        int v = valueReader.getDictItemsCount();
        checkNotNull("getDictItemsCount");
        return v;
    }

    @Override
    public ValueReader getDictKey(int index) {
        ValueReader v = valueReader.getDictKey(index);
        checkNotNull("getDictKey");
        return v;
    }

    @Override
    public ValueReader getDictValue(int index) {
        ValueReader v = valueReader.getDictValue(index);
        checkNotNull("getDictValue");
        return v;
    }

    @Override
    public int getListItemsCount() {
        int v = valueReader.getListItemsCount();
        checkNotNull("getListItemsCount");
        return v;
    }

    @Override
    public ValueReader getListItem(int index) {
        ValueReader v = valueReader.getListItem(index);
        checkNotNull("getListItem");
        return v;
    }

    @Override
    public int getStructMembersCount() {
        int v = valueReader.getStructMembersCount();
        checkNotNull("getStructMembersCount");
        return v;
    }

    @Override
    public String getStructMemberName(int index) {
        String v = valueReader.getStructMemberName(index);
        checkNotNull("getStructMemberName");
        return v;
    }

    @Override
    public ValueReader getStructMember(int index) {
        ValueReader v = valueReader.getStructMember(index);
        checkNotNull("getStructMember");
        return v;
    }

    @Override
    public ValueReader getStructMember(String name) {
        ValueReader v = valueReader.getStructMember(name);
        checkNotNull("getStructMember");
        return v;
    }

    @Override
    public int getTupleElementsCount() {
        int v = valueReader.getTupleElementsCount();
        checkNotNull("getTupleElementsCount");
        return v;
    }

    @Override
    public ValueReader getTupleElement(int index) {
        ValueReader v = valueReader.getTupleElement(index);
        checkNotNull("getTupleElement");
        return v;
    }

    @Override
    public int getVariantTypeIndex() {
        int v = valueReader.getVariantTypeIndex();
        checkNotNull("getVariantTypeIndex");
        return v;
    }

    @Override
    public ValueReader getVariantItem() {
        ValueReader v = valueReader.getVariantItem();
        checkNotNull("getVariantItem");
        return v;
    }

    @Override
    public void toString(StringBuilder sb) {
        if (isOptionalItemPresent()) {
            sb.append("Some[");
            getOptionalItem().toString(sb);
            sb.append(']');
        } else {
            sb.append("Empty[]");
        }
    }
}
