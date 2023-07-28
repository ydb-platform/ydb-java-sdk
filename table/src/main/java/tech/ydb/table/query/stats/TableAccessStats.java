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
import com.google.protobuf.SingleFieldBuilderV3;
import com.google.protobuf.UnknownFieldSet;

public final class TableAccessStats extends GeneratedMessageV3 implements TableAccessStatsOrBuilder {
    public static final int NAME_FIELD_NUMBER = 1;
    public static final int READS_FIELD_NUMBER = 3;
    public static final int UPDATES_FIELD_NUMBER = 4;
    public static final int DELETES_FIELD_NUMBER = 5;
    public static final int PARTITIONS_COUNT_FIELD_NUMBER = 6;
    private static final long serialVersionUID = 0L;
    private static final TableAccessStats DEFAULT_INSTANCE = new TableAccessStats();
    private static final Parser<TableAccessStats> PARSER = new AbstractParser<TableAccessStats>() {
        public TableAccessStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                throws InvalidProtocolBufferException {
            return new TableAccessStats(input, extensionRegistry);
        }
    };
    private volatile Object name;
    private OperationStats reads;
    private OperationStats updates;
    private OperationStats deletes;
    private long partitionsCount;
    private byte memoizedIsInitialized;

    private TableAccessStats(GeneratedMessageV3.Builder<?> builder) {
        super(builder);
        this.memoizedIsInitialized = -1;
    }

    private TableAccessStats() {
        this.memoizedIsInitialized = -1;
        this.name = "";
    }

    public TableAccessStats(tech.ydb.proto.YdbQueryStats.TableAccessStats protoAutoGenTableAccessStats) {
        this.name = protoAutoGenTableAccessStats.getName();
        this.reads = new OperationStats(protoAutoGenTableAccessStats.getReads());
        this.updates = new OperationStats(protoAutoGenTableAccessStats.getUpdates());
        this.deletes = new OperationStats(protoAutoGenTableAccessStats.getDeletes());
        this.partitionsCount = protoAutoGenTableAccessStats.getPartitionsCount();
    }

    private TableAccessStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
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
                    OperationStats.Builder subBuilder;
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10:
                            String s = input.readStringRequireUtf8();
                            this.name = s;
                            break;
                        case 26:
                            subBuilder = null;
                            if (this.reads != null) {
                                subBuilder = this.reads.toBuilder();
                            }

                            this.reads = input.readMessage(OperationStats.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(this.reads);
                                this.reads = subBuilder.buildPartial();
                            }
                            break;
                        case 34:
                            subBuilder = null;
                            if (this.updates != null) {
                                subBuilder = this.updates.toBuilder();
                            }

                            this.updates = input.readMessage(OperationStats.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(this.updates);
                                this.updates = subBuilder.buildPartial();
                            }
                            break;
                        case 42:
                            subBuilder = null;
                            if (this.deletes != null) {
                                subBuilder = this.deletes.toBuilder();
                            }

                            this.deletes = input.readMessage(OperationStats.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(this.deletes);
                                this.deletes = subBuilder.buildPartial();
                            }
                            break;
                        case 48:
                            this.partitionsCount = input.readUInt64();
                            break;
                        default:
                            if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                    }
                }
            } catch (InvalidProtocolBufferException var11) {
                throw var11.setUnfinishedMessage(this);
            } catch (IOException var12) {
                throw (new InvalidProtocolBufferException(var12)).setUnfinishedMessage(this);
            } finally {
                this.unknownFields = unknownFields.build();
                this.makeExtensionsImmutable();
            }

        }
    }

    public static Descriptors.Descriptor getDescriptor() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR;
    }

    public static TableAccessStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static TableAccessStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TableAccessStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static TableAccessStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TableAccessStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static TableAccessStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static TableAccessStats parseFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static TableAccessStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static TableAccessStats parseDelimitedFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static TableAccessStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static TableAccessStats parseFrom(CodedInputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static TableAccessStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(TableAccessStats prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static TableAccessStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Parser<TableAccessStats> parser() {
        return PARSER;
    }

    protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
        return new TableAccessStats();
    }

    public UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(TableAccessStats.class, Builder.class);
    }

    public String getName() {
        Object ref = this.name;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            ByteString bs = (ByteString) ref;
            String s = bs.toStringUtf8();
            this.name = s;
            return s;
        }
    }

    public ByteString getNameBytes() {
        Object ref = this.name;
        if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String) ref);
            this.name = b;
            return b;
        } else {
            return (ByteString) ref;
        }
    }

    public boolean hasReads() {
        return this.reads != null;
    }

    public OperationStats getReads() {
        return this.reads == null ? OperationStats.getDefaultInstance() : this.reads;
    }

    public OperationStatsOrBuilder getReadsOrBuilder() {
        return this.getReads();
    }

    public boolean hasUpdates() {
        return this.updates != null;
    }

    public OperationStats getUpdates() {
        return this.updates == null ? OperationStats.getDefaultInstance() : this.updates;
    }

    public OperationStatsOrBuilder getUpdatesOrBuilder() {
        return this.getUpdates();
    }

    public boolean hasDeletes() {
        return this.deletes != null;
    }

    public OperationStats getDeletes() {
        return this.deletes == null ? OperationStats.getDefaultInstance() : this.deletes;
    }

    public OperationStatsOrBuilder getDeletesOrBuilder() {
        return this.getDeletes();
    }

    public long getPartitionsCount() {
        return this.partitionsCount;
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
        if (!GeneratedMessageV3.isStringEmpty(this.name)) {
            GeneratedMessageV3.writeString(output, 1, this.name);
        }

        if (this.reads != null) {
            output.writeMessage(3, this.getReads());
        }

        if (this.updates != null) {
            output.writeMessage(4, this.getUpdates());
        }

        if (this.deletes != null) {
            output.writeMessage(5, this.getDeletes());
        }

        if (this.partitionsCount != 0L) {
            output.writeUInt64(6, this.partitionsCount);
        }

        this.unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
        int size = this.memoizedSize;
        if (size != -1) {
            return size;
        } else {
            size = 0;
            if (!GeneratedMessageV3.isStringEmpty(this.name)) {
                size += GeneratedMessageV3.computeStringSize(1, this.name);
            }

            if (this.reads != null) {
                size += CodedOutputStream.computeMessageSize(3, this.getReads());
            }

            if (this.updates != null) {
                size += CodedOutputStream.computeMessageSize(4, this.getUpdates());
            }

            if (this.deletes != null) {
                size += CodedOutputStream.computeMessageSize(5, this.getDeletes());
            }

            if (this.partitionsCount != 0L) {
                size += CodedOutputStream.computeUInt64Size(6, this.partitionsCount);
            }

            size += this.unknownFields.getSerializedSize();
            this.memoizedSize = size;
            return size;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof TableAccessStats)) {
            return super.equals(obj);
        } else {
            TableAccessStats other = (TableAccessStats) obj;
            if (!this.getName().equals(other.getName())) {
                return false;
            } else if (this.hasReads() != other.hasReads()) {
                return false;
            } else if (this.hasReads() && !this.getReads().equals(other.getReads())) {
                return false;
            } else if (this.hasUpdates() != other.hasUpdates()) {
                return false;
            } else if (this.hasUpdates() && !this.getUpdates().equals(other.getUpdates())) {
                return false;
            } else if (this.hasDeletes() != other.hasDeletes()) {
                return false;
            } else if (this.hasDeletes() && !this.getDeletes().equals(other.getDeletes())) {
                return false;
            } else if (this.getPartitionsCount() != other.getPartitionsCount()) {
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
            hash = 53 * hash + this.getName().hashCode();
            if (this.hasReads()) {
                hash = 37 * hash + 3;
                hash = 53 * hash + this.getReads().hashCode();
            }

            if (this.hasUpdates()) {
                hash = 37 * hash + 4;
                hash = 53 * hash + this.getUpdates().hashCode();
            }

            if (this.hasDeletes()) {
                hash = 37 * hash + 5;
                hash = 53 * hash + this.getDeletes().hashCode();
            }

            hash = 37 * hash + 6;
            hash = 53 * hash + Internal.hashLong(this.getPartitionsCount());
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

    public Parser<TableAccessStats> getParserForType() {
        return PARSER;
    }

    public TableAccessStats getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    public static final class Builder extends GeneratedMessageV3.Builder<Builder> implements TableAccessStatsOrBuilder {
        private Object name;
        private OperationStats reads;
        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> readsBuilder;
        private OperationStats updates;
        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> updatesBuilder;
        private OperationStats deletes;
        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> deletesBuilder;
        private long partitionsCount;

        private Builder() {
            this.name = "";
            this.maybeForceBuilderInitialization();
        }

        private Builder(GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            this.name = "";
            this.maybeForceBuilderInitialization();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(TableAccessStats.class, Builder.class);
        }

        private void maybeForceBuilderInitialization() {
        }

        public Builder clear() {
            super.clear();
            this.name = "";
            if (this.readsBuilder == null) {
                this.reads = null;
            } else {
                this.reads = null;
                this.readsBuilder = null;
            }

            if (this.updatesBuilder == null) {
                this.updates = null;
            } else {
                this.updates = null;
                this.updatesBuilder = null;
            }

            if (this.deletesBuilder == null) {
                this.deletes = null;
            } else {
                this.deletes = null;
                this.deletesBuilder = null;
            }

            this.partitionsCount = 0L;
            return this;
        }

        public Descriptors.Descriptor getDescriptorForType() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR;
        }

        public TableAccessStats getDefaultInstanceForType() {
            return TableAccessStats.getDefaultInstance();
        }

        public TableAccessStats build() {
            TableAccessStats result = this.buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            } else {
                return result;
            }
        }

        public TableAccessStats buildPartial() {
            TableAccessStats result = new TableAccessStats(this);
            result.name = this.name;
            if (this.readsBuilder == null) {
                result.reads = this.reads;
            } else {
                result.reads = this.readsBuilder.build();
            }

            if (this.updatesBuilder == null) {
                result.updates = this.updates;
            } else {
                result.updates = this.updatesBuilder.build();
            }

            if (this.deletesBuilder == null) {
                result.deletes = this.deletes;
            } else {
                result.deletes = this.deletesBuilder.build();
            }

            result.partitionsCount = this.partitionsCount;
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
            if (other instanceof TableAccessStats) {
                return this.mergeFrom((TableAccessStats) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(TableAccessStats other) {
            if (other != TableAccessStats.getDefaultInstance()) {
                if (!other.getName().isEmpty()) {
                    this.name = other.name;
                    this.onChanged();
                }

                if (other.hasReads()) {
                    this.mergeReads(other.getReads());
                }

                if (other.hasUpdates()) {
                    this.mergeUpdates(other.getUpdates());
                }

                if (other.hasDeletes()) {
                    this.mergeDeletes(other.getDeletes());
                }

                if (other.getPartitionsCount() != 0L) {
                    this.setPartitionsCount(other.getPartitionsCount());
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
            TableAccessStats parsedMessage = null;

            try {
                parsedMessage = TableAccessStats.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
                parsedMessage = (TableAccessStats) var8.getUnfinishedMessage();
                throw var8.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    this.mergeFrom(parsedMessage);
                }

            }

            return this;
        }

        public String getName() {
            Object ref = this.name;
            if (!(ref instanceof String)) {
                ByteString bs = (ByteString) ref;
                String s = bs.toStringUtf8();
                this.name = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        public Builder setName(String value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                this.name = value;
                this.onChanged();
                return this;
            }
        }

        public ByteString getNameBytes() {
            Object ref = this.name;
            if (ref instanceof String) {
                ByteString b = ByteString.copyFromUtf8((String) ref);
                this.name = b;
                return b;
            } else {
                return (ByteString) ref;
            }
        }

        public Builder setNameBytes(ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                TableAccessStats.checkByteStringIsUtf8(value);
                this.name = value;
                this.onChanged();
                return this;
            }
        }

        public Builder clearName() {
            this.name = TableAccessStats.getDefaultInstance().getName();
            this.onChanged();
            return this;
        }

        public boolean hasReads() {
            return this.readsBuilder != null || this.reads != null;
        }

        public OperationStats getReads() {
            if (this.readsBuilder == null) {
                return this.reads == null ? OperationStats.getDefaultInstance() : this.reads;
            } else {
                return this.readsBuilder.getMessage();
            }
        }

        public Builder setReads(OperationStats value) {
            if (this.readsBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.reads = value;
                this.onChanged();
            } else {
                this.readsBuilder.setMessage(value);
            }

            return this;
        }

        public Builder setReads(OperationStats.Builder builderForValue) {
            if (this.readsBuilder == null) {
                this.reads = builderForValue.build();
                this.onChanged();
            } else {
                this.readsBuilder.setMessage(builderForValue.build());
            }

            return this;
        }

        public Builder mergeReads(OperationStats value) {
            if (this.readsBuilder == null) {
                if (this.reads != null) {
                    this.reads = OperationStats.newBuilder(this.reads).mergeFrom(value).buildPartial();
                } else {
                    this.reads = value;
                }

                this.onChanged();
            } else {
                this.readsBuilder.mergeFrom(value);
            }

            return this;
        }

        public Builder clearReads() {
            if (this.readsBuilder == null) {
                this.reads = null;
                this.onChanged();
            } else {
                this.reads = null;
                this.readsBuilder = null;
            }

            return this;
        }

        public OperationStats.Builder getReadsBuilder() {
            this.onChanged();
            return this.getReadsFieldBuilder().getBuilder();
        }

        public OperationStatsOrBuilder getReadsOrBuilder() {
            if (this.readsBuilder != null) {
                return this.readsBuilder.getMessageOrBuilder();
            } else {
                return this.reads == null ? OperationStats.getDefaultInstance() : this.reads;
            }
        }

        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder>
        getReadsFieldBuilder() {
            if (this.readsBuilder == null) {
                this.readsBuilder =
                        new SingleFieldBuilderV3<>(this.getReads(), this.getParentForChildren(), this.isClean());
                this.reads = null;
            }

            return this.readsBuilder;
        }

        public boolean hasUpdates() {
            return this.updatesBuilder != null || this.updates != null;
        }

        public OperationStats getUpdates() {
            if (this.updatesBuilder == null) {
                return this.updates == null ? OperationStats.getDefaultInstance() : this.updates;
            } else {
                return this.updatesBuilder.getMessage();
            }
        }

        public Builder setUpdates(OperationStats value) {
            if (this.updatesBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.updates = value;
                this.onChanged();
            } else {
                this.updatesBuilder.setMessage(value);
            }

            return this;
        }

        public Builder setUpdates(OperationStats.Builder builderForValue) {
            if (this.updatesBuilder == null) {
                this.updates = builderForValue.build();
                this.onChanged();
            } else {
                this.updatesBuilder.setMessage(builderForValue.build());
            }

            return this;
        }

        public Builder mergeUpdates(OperationStats value) {
            if (this.updatesBuilder == null) {
                if (this.updates != null) {
                    this.updates = OperationStats.newBuilder(this.updates).mergeFrom(value).buildPartial();
                } else {
                    this.updates = value;
                }

                this.onChanged();
            } else {
                this.updatesBuilder.mergeFrom(value);
            }

            return this;
        }

        public Builder clearUpdates() {
            if (this.updatesBuilder == null) {
                this.updates = null;
                this.onChanged();
            } else {
                this.updates = null;
                this.updatesBuilder = null;
            }

            return this;
        }

        public OperationStats.Builder getUpdatesBuilder() {
            this.onChanged();
            return this.getUpdatesFieldBuilder().getBuilder();
        }

        public OperationStatsOrBuilder getUpdatesOrBuilder() {
            if (this.updatesBuilder != null) {
                return this.updatesBuilder.getMessageOrBuilder();
            } else {
                return this.updates == null ? OperationStats.getDefaultInstance() : this.updates;
            }
        }

        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder>
        getUpdatesFieldBuilder() {
            if (this.updatesBuilder == null) {
                this.updatesBuilder =
                        new SingleFieldBuilderV3<>(this.getUpdates(), this.getParentForChildren(), this.isClean());
                this.updates = null;
            }

            return this.updatesBuilder;
        }

        public boolean hasDeletes() {
            return this.deletesBuilder != null || this.deletes != null;
        }

        public OperationStats getDeletes() {
            if (this.deletesBuilder == null) {
                return this.deletes == null ? OperationStats.getDefaultInstance() : this.deletes;
            } else {
                return this.deletesBuilder.getMessage();
            }
        }

        public Builder setDeletes(OperationStats value) {
            if (this.deletesBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.deletes = value;
                this.onChanged();
            } else {
                this.deletesBuilder.setMessage(value);
            }

            return this;
        }

        public Builder setDeletes(OperationStats.Builder builderForValue) {
            if (this.deletesBuilder == null) {
                this.deletes = builderForValue.build();
                this.onChanged();
            } else {
                this.deletesBuilder.setMessage(builderForValue.build());
            }

            return this;
        }

        public Builder mergeDeletes(OperationStats value) {
            if (this.deletesBuilder == null) {
                if (this.deletes != null) {
                    this.deletes = OperationStats.newBuilder(this.deletes).mergeFrom(value).buildPartial();
                } else {
                    this.deletes = value;
                }

                this.onChanged();
            } else {
                this.deletesBuilder.mergeFrom(value);
            }

            return this;
        }

        public Builder clearDeletes() {
            if (this.deletesBuilder == null) {
                this.deletes = null;
                this.onChanged();
            } else {
                this.deletes = null;
                this.deletesBuilder = null;
            }

            return this;
        }

        public OperationStats.Builder getDeletesBuilder() {
            this.onChanged();
            return this.getDeletesFieldBuilder().getBuilder();
        }

        public OperationStatsOrBuilder getDeletesOrBuilder() {
            if (this.deletesBuilder != null) {
                return this.deletesBuilder.getMessageOrBuilder();
            } else {
                return this.deletes == null ? OperationStats.getDefaultInstance() : this.deletes;
            }
        }

        private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder>
        getDeletesFieldBuilder() {
            if (this.deletesBuilder == null) {
                this.deletesBuilder =
                        new SingleFieldBuilderV3<>(this.getDeletes(), this.getParentForChildren(), this.isClean());
                this.deletes = null;
            }

            return this.deletesBuilder;
        }

        public long getPartitionsCount() {
            return this.partitionsCount;
        }

        public Builder setPartitionsCount(long value) {
            this.partitionsCount = value;
            this.onChanged();
            return this;
        }

        public Builder clearPartitionsCount() {
            this.partitionsCount = 0L;
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
