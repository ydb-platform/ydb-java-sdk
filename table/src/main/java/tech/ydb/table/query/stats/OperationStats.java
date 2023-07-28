package tech.ydb.table.query.stats;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.protobuf.AbstractParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.google.protobuf.UnknownFieldSet;

public final class OperationStats extends GeneratedMessageV3 implements OperationStatsOrBuilder {
    public static final int ROWS_FIELD_NUMBER = 1;
    public static final int BYTES_FIELD_NUMBER = 2;
    private static final long serialVersionUID = 0L;
    private static final OperationStats DEFAULT_INSTANCE = new OperationStats();
    private static final Parser<OperationStats> PARSER = new AbstractParser<OperationStats>() {
        public OperationStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                throws InvalidProtocolBufferException {
            return new OperationStats(input, extensionRegistry);
        }
    };
    private long rows;
    private long bytes;
    private byte memoizedIsInitialized;

    private OperationStats(GeneratedMessageV3.Builder<?> builder) {
        super(builder);
        this.memoizedIsInitialized = -1;
    }

    private OperationStats() {
        this.memoizedIsInitialized = -1;
    }

    public OperationStats(tech.ydb.proto.YdbQueryStats.OperationStats protoAutoGenOperationStats) {
        this.rows = protoAutoGenOperationStats.getRows();
        this.bytes = protoAutoGenOperationStats.getBytes();
    }

    private OperationStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new NullPointerException();
        } else {
            UnknownFieldSet.Builder unknownFields = UnknownFieldSet.newBuilder();

            try {
                boolean done = false;

                while (!done) {
                    int tag = input.readTag();
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 8:
                            this.rows = input.readUInt64();
                            break;
                        case 16:
                            this.bytes = input.readUInt64();
                            break;
                        default:
                            if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                    }
                }
            } catch (InvalidProtocolBufferException var10) {
                throw var10.setUnfinishedMessage(this);
            } catch (IOException var11) {
                throw (new InvalidProtocolBufferException(var11)).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                this.makeExtensionsImmutable();
            }

        }
    }

    public static Descriptors.Descriptor getDescriptor() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR;
    }

    public static OperationStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OperationStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OperationStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OperationStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OperationStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static OperationStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static OperationStats parseFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static OperationStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static OperationStats parseDelimitedFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static OperationStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static OperationStats parseFrom(CodedInputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static OperationStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(OperationStats prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static OperationStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Parser<OperationStats> parser() {
        return PARSER;
    }

    protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
        return new OperationStats();
    }

    public UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(OperationStats.class, Builder.class);
    }

    public long getRows() {
        return this.rows;
    }

    public long getBytes() {
        return this.bytes;
    }

    public boolean isInitialized() {
        byte isInitialized = this.memoizedIsInitialized;
        if (isInitialized == 1) {
            return true;
        } else if (isInitialized == 0) {
            return false;
        } else {
            this.memoizedIsInitialized = 1;
            return true;
        }
    }

    public void writeTo(CodedOutputStream output) throws IOException {
        if (this.rows != 0L) {
            output.writeUInt64(1, this.rows);
        }

        if (this.bytes != 0L) {
            output.writeUInt64(2, this.bytes);
        }

        this.unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
        int size = this.memoizedSize;
        if (size == -1) {
            size = 0;
            if (this.rows != 0L) {
                size += CodedOutputStream.computeUInt64Size(1, this.rows);
            }

            if (this.bytes != 0L) {
                size += CodedOutputStream.computeUInt64Size(2, this.bytes);
            }

            size += this.unknownFields.getSerializedSize();
            this.memoizedSize = size;
        }
        return size;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof OperationStats)) {
            return super.equals(obj);
        } else {
            OperationStats other = (OperationStats) obj;
            if (this.getRows() != other.getRows()) {
                return false;
            } else if (this.getBytes() != other.getBytes()) {
                return false;
            } else {
                return this.unknownFields.equals(other.unknownFields);
            }
        }
    }

    public int hashCode() {
        if (this.memoizedHashCode != 0) {
            return this.memoizedHashCode;
        } else {
            int hash = 41;
            hash = 19 * hash + getDescriptor().hashCode();
            hash = 37 * hash + 1;
            hash = 53 * hash + Internal.hashLong(this.getRows());
            hash = 37 * hash + 2;
            hash = 53 * hash + Internal.hashLong(this.getBytes());
            hash = 29 * hash + this.unknownFields.hashCode();
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    public Builder newBuilderForType() {
        return newBuilder();
    }

    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
    }

    protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
        return new Builder(parent);
    }

    public Parser<OperationStats> getParserForType() {
        return PARSER;
    }

    public OperationStats getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    public static final class Builder extends GeneratedMessageV3.Builder<Builder> implements OperationStatsOrBuilder {
        private long rows;
        private long bytes;

        private Builder() {
            this.maybeForceBuilderInitialization();
        }

        private Builder(GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            this.maybeForceBuilderInitialization();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(OperationStats.class, Builder.class);
        }

        private void maybeForceBuilderInitialization() {
        }

        public Builder clear() {
            super.clear();
            this.rows = 0L;
            this.bytes = 0L;
            return this;
        }

        public Descriptors.Descriptor getDescriptorForType() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR;
        }

        public OperationStats getDefaultInstanceForType() {
            return OperationStats.getDefaultInstance();
        }

        public OperationStats build() {
            OperationStats result = this.buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            } else {
                return result;
            }
        }

        public OperationStats buildPartial() {
            OperationStats result = new OperationStats(this);
            result.rows = this.rows;
            result.bytes = this.bytes;
            this.onBuilt();
            return result;
        }

        public Builder clone() {
            return super.clone();
        }

        public Builder setField(Descriptors.FieldDescriptor field, Object value) {
            return super.setField(field, value);
        }

        public Builder clearField(Descriptors.FieldDescriptor field) {
            return super.clearField(field);
        }

        public Builder clearOneof(Descriptors.OneofDescriptor oneOf) {
            return super.clearOneof(oneOf);
        }

        public Builder setRepeatedField(Descriptors.FieldDescriptor field, int index, Object value) {
            return super.setRepeatedField(field, index, value);
        }

        public Builder addRepeatedField(Descriptors.FieldDescriptor field, Object value) {
            return super.addRepeatedField(field, value);
        }

        public Builder mergeFrom(Message other) {
            if (other instanceof OperationStats) {
                return this.mergeFrom((OperationStats) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(OperationStats other) {
            if (other != OperationStats.getDefaultInstance()) {
                if (other.getRows() != 0L) {
                    this.setRows(other.getRows());
                }

                if (other.getBytes() != 0L) {
                    this.setBytes(other.getBytes());
                }

                this.mergeUnknownFields(other.unknownFields);
                this.onChanged();
            }
            return this;
        }

        public boolean isInitialized() {
            return true;
        }

        public Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            OperationStats parsedMessage = null;

            try {
                parsedMessage = OperationStats.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
                parsedMessage = (OperationStats) var8.getUnfinishedMessage();
                throw var8.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    this.mergeFrom(parsedMessage);
                }

            }

            return this;
        }

        public long getRows() {
            return this.rows;
        }

        public Builder setRows(long value) {
            this.rows = value;
            this.onChanged();
            return this;
        }

        public Builder clearRows() {
            this.rows = 0L;
            this.onChanged();
            return this;
        }

        public long getBytes() {
            return this.bytes;
        }

        public Builder setBytes(long value) {
            this.bytes = value;
            this.onChanged();
            return this;
        }

        public Builder clearBytes() {
            this.bytes = 0L;
            this.onChanged();
            return this;
        }

        public Builder setUnknownFields(UnknownFieldSet unknownFields) {
            return super.setUnknownFields(unknownFields);
        }

        public Builder mergeUnknownFields(UnknownFieldSet unknownFields) {
            return super.mergeUnknownFields(unknownFields);
        }
    }
}
