package tech.ydb.table.query.stats;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import com.google.protobuf.RepeatedFieldBuilderV3;
import com.google.protobuf.UnknownFieldSet;

public final class QueryPhaseStats extends GeneratedMessageV3 implements QueryPhaseStatsOrBuilder {
    private static final long serialVersionUID = 0L;
    public static final int DURATION_US_FIELD_NUMBER = 1;
    private long durationUs;
    public static final int TABLE_ACCESS_FIELD_NUMBER = 2;
    private List<TableAccessStats> tableAccess;
    public static final int CPU_TIME_US_FIELD_NUMBER = 3;
    private long cpuTimeUs;
    public static final int AFFECTED_SHARDS_FIELD_NUMBER = 4;
    private long affectedShards;
    public static final int LITERAL_PHASE_FIELD_NUMBER = 5;
    private boolean literalPhase;
    private byte memoizedIsInitialized;
    private static final QueryPhaseStats DEFAULT_INSTANCE = new QueryPhaseStats();
    private static final Parser<QueryPhaseStats> PARSER = new AbstractParser<QueryPhaseStats>() {
        public QueryPhaseStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                throws InvalidProtocolBufferException {
            return new QueryPhaseStats(input, extensionRegistry);
        }
    };

    private QueryPhaseStats(GeneratedMessageV3.Builder<?> builder) {
        super(builder);
        this.memoizedIsInitialized = -1;
    }

    private QueryPhaseStats() {
        this.memoizedIsInitialized = -1;
        this.tableAccess = Collections.emptyList();
    }

    private QueryPhaseStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        this();
        if (extensionRegistry == null) {
            throw new NullPointerException();
        } else {
            boolean mutableBitField0 = false;
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
                            this.durationUs = input.readUInt64();
                            break;
                        case 18:
                            if (!(mutableBitField0)) {
                                this.tableAccess = new ArrayList<>();
                                mutableBitField0 = true;
                            }

                            this.tableAccess.add(input.readMessage(TableAccessStats.parser(), extensionRegistry));
                            break;
                        case 24:
                            this.cpuTimeUs = input.readUInt64();
                            break;
                        case 32:
                            this.affectedShards = input.readUInt64();
                            break;
                        case 40:
                            this.literalPhase = input.readBool();
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
                if (mutableBitField0) {
                    this.tableAccess = Collections.unmodifiableList(this.tableAccess);
                }

                this.unknownFields = unknownFields.build();
                this.makeExtensionsImmutable();
            }

        }
    }

    public QueryPhaseStats(tech.ydb.proto.YdbQueryStats.QueryPhaseStats protoAutoGenQueryPhaseStats) {
        this.durationUs = protoAutoGenQueryPhaseStats.getDurationUs();
        this.tableAccess = protoAutoGenQueryPhaseStats.getTableAccessList().stream().map(TableAccessStats::new)
                .collect(Collectors.toList());
        this.cpuTimeUs = protoAutoGenQueryPhaseStats.getCpuTimeUs();
        this.affectedShards = protoAutoGenQueryPhaseStats.getAffectedShards();
        this.literalPhase = protoAutoGenQueryPhaseStats.getLiteralPhase();
    }

    protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
        return new QueryPhaseStats();
    }

    public UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    public static Descriptors.Descriptor getDescriptor() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR;
    }

    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(QueryPhaseStats.class, Builder.class);
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public List<TableAccessStats> getTableAccessList() {
        return this.tableAccess;
    }

    public List<? extends TableAccessStatsOrBuilder> getTableAccessOrBuilderList() {
        return this.tableAccess;
    }

    public int getTableAccessCount() {
        return this.tableAccess.size();
    }

    public TableAccessStats getTableAccess(int index) {
        return this.tableAccess.get(index);
    }

    public TableAccessStatsOrBuilder getTableAccessOrBuilder(int index) {
        return this.tableAccess.get(index);
    }

    public long getCpuTimeUs() {
        return this.cpuTimeUs;
    }

    public long getAffectedShards() {
        return this.affectedShards;
    }

    public boolean getLiteralPhase() {
        return this.literalPhase;
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
        if (this.durationUs != 0L) {
            output.writeUInt64(1, this.durationUs);
        }

        for (TableAccessStats access : this.tableAccess) {
            output.writeMessage(2, access);
        }

        if (this.cpuTimeUs != 0L) {
            output.writeUInt64(3, this.cpuTimeUs);
        }

        if (this.affectedShards != 0L) {
            output.writeUInt64(4, this.affectedShards);
        }

        if (this.literalPhase) {
            output.writeBool(5, this.literalPhase);
        }

        this.unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
        int size = this.memoizedSize;
        if (size == -1) {
            size = 0;
            if (this.durationUs != 0L) {
                size += CodedOutputStream.computeUInt64Size(1, this.durationUs);
            }

            for (TableAccessStats access : this.tableAccess) {
                size += CodedOutputStream.computeMessageSize(2, access);
            }

            if (this.cpuTimeUs != 0L) {
                size += CodedOutputStream.computeUInt64Size(3, this.cpuTimeUs);
            }

            if (this.affectedShards != 0L) {
                size += CodedOutputStream.computeUInt64Size(4, this.affectedShards);
            }

            if (this.literalPhase) {
                size += CodedOutputStream.computeBoolSize(5, this.literalPhase);
            }

            size += this.unknownFields.getSerializedSize();
            this.memoizedSize = size;
        }
        return size;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryPhaseStats)) {
            return super.equals(obj);
        } else {
            QueryPhaseStats other = (QueryPhaseStats) obj;
            if (this.getDurationUs() != other.getDurationUs()) {
                return false;
            } else if (!this.getTableAccessList().equals(other.getTableAccessList())) {
                return false;
            } else if (this.getCpuTimeUs() != other.getCpuTimeUs()) {
                return false;
            } else if (this.getAffectedShards() != other.getAffectedShards()) {
                return false;
            } else if (this.getLiteralPhase() != other.getLiteralPhase()) {
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
            hash = 53 * hash + Internal.hashLong(this.getDurationUs());
            if (this.getTableAccessCount() > 0) {
                hash = 37 * hash + 2;
                hash = 53 * hash + this.getTableAccessList().hashCode();
            }

            hash = 37 * hash + 3;
            hash = 53 * hash + Internal.hashLong(this.getCpuTimeUs());
            hash = 37 * hash + 4;
            hash = 53 * hash + Internal.hashLong(this.getAffectedShards());
            hash = 37 * hash + 5;
            hash = 53 * hash + Internal.hashBoolean(this.getLiteralPhase());
            hash = 29 * hash + this.unknownFields.hashCode();
            this.memoizedHashCode = hash;
            return hash;
        }
    }

    public static QueryPhaseStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryPhaseStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryPhaseStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryPhaseStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryPhaseStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryPhaseStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryPhaseStats parseFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static QueryPhaseStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static QueryPhaseStats parseDelimitedFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static QueryPhaseStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static QueryPhaseStats parseFrom(CodedInputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static QueryPhaseStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() {
        return newBuilder();
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(QueryPhaseStats prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
    }

    protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
        return new Builder(parent);
    }

    public static QueryPhaseStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Parser<QueryPhaseStats> parser() {
        return PARSER;
    }

    public Parser<QueryPhaseStats> getParserForType() {
        return PARSER;
    }

    public QueryPhaseStats getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    public static final class Builder extends GeneratedMessageV3.Builder<Builder> implements QueryPhaseStatsOrBuilder {
        private int bitField0;
        private long durationUs;
        private List<TableAccessStats> tableAccess;
        private RepeatedFieldBuilderV3<TableAccessStats, TableAccessStats.Builder, TableAccessStatsOrBuilder>
                tableAccessBuilder;
        private long cpuTimeUs;
        private long affectedShards;
        private boolean literalPhase;

        private Builder() {
            this.tableAccess = Collections.emptyList();
            this.maybeForceBuilderInitialization();
        }

        private Builder(GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            this.tableAccess = Collections.emptyList();
            this.maybeForceBuilderInitialization();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(QueryPhaseStats.class, Builder.class);
        }

        private void maybeForceBuilderInitialization() {
            if (QueryPhaseStats.alwaysUseFieldBuilders) {
                this.getTableAccessFieldBuilder();
            }

        }

        public Builder clear() {
            super.clear();
            this.durationUs = 0L;
            if (this.tableAccessBuilder == null) {
                this.tableAccess = Collections.emptyList();
                this.bitField0 &= -2;
            } else {
                this.tableAccessBuilder.clear();
            }

            this.cpuTimeUs = 0L;
            this.affectedShards = 0L;
            this.literalPhase = false;
            return this;
        }

        public Descriptors.Descriptor getDescriptorForType() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR;
        }

        public QueryPhaseStats getDefaultInstanceForType() {
            return QueryPhaseStats.getDefaultInstance();
        }

        public QueryPhaseStats build() {
            QueryPhaseStats result = this.buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            } else {
                return result;
            }
        }

        public QueryPhaseStats buildPartial() {
            QueryPhaseStats result = new QueryPhaseStats(this);
            result.durationUs = this.durationUs;
            if (this.tableAccessBuilder == null) {
                if ((this.bitField0 & 1) != 0) {
                    this.tableAccess = Collections.unmodifiableList(this.tableAccess);
                    this.bitField0 &= -2;
                }
                result.tableAccess = this.tableAccess;
            } else {
                result.tableAccess = this.tableAccessBuilder.build();
            }
            result.cpuTimeUs = this.cpuTimeUs;
            result.affectedShards = this.affectedShards;
            result.literalPhase = this.literalPhase;
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
            if (other instanceof QueryPhaseStats) {
                return this.mergeFrom((QueryPhaseStats) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(QueryPhaseStats other) {
            if (other != QueryPhaseStats.getDefaultInstance()) {
                if (other.getDurationUs() != 0L) {
                    this.setDurationUs(other.getDurationUs());
                }

                if (this.tableAccessBuilder == null) {
                    if (!other.tableAccess.isEmpty()) {
                        if (this.tableAccess.isEmpty()) {
                            this.tableAccess = other.tableAccess;
                            this.bitField0 &= -2;
                        } else {
                            this.ensureTableAccessIsMutable();
                            this.tableAccess.addAll(other.tableAccess);
                        }

                        this.onChanged();
                    }
                } else if (!other.tableAccess.isEmpty()) {
                    if (this.tableAccessBuilder.isEmpty()) {
                        this.tableAccessBuilder.dispose();
                        this.tableAccessBuilder = null;
                        this.tableAccess = other.tableAccess;
                        this.bitField0 &= -2;
                        this.tableAccessBuilder =
                                QueryPhaseStats.alwaysUseFieldBuilders ? this.getTableAccessFieldBuilder() : null;
                    } else {
                        this.tableAccessBuilder.addAllMessages(other.tableAccess);
                    }
                }

                if (other.getCpuTimeUs() != 0L) {
                    this.setCpuTimeUs(other.getCpuTimeUs());
                }

                if (other.getAffectedShards() != 0L) {
                    this.setAffectedShards(other.getAffectedShards());
                }

                if (other.getLiteralPhase()) {
                    this.setLiteralPhase(other.getLiteralPhase());
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
            QueryPhaseStats parsedMessage = null;

            try {
                parsedMessage = QueryPhaseStats.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
                parsedMessage = (QueryPhaseStats) var8.getUnfinishedMessage();
                throw var8.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    this.mergeFrom(parsedMessage);
                }

            }

            return this;
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public Builder setDurationUs(long value) {
            this.durationUs = value;
            this.onChanged();
            return this;
        }

        public Builder clearDurationUs() {
            this.durationUs = 0L;
            this.onChanged();
            return this;
        }

        private void ensureTableAccessIsMutable() {
            if ((this.bitField0 & 1) == 0) {
                this.tableAccess = new ArrayList<>(this.tableAccess);
                this.bitField0 |= 1;
            }

        }

        public List<TableAccessStats> getTableAccessList() {
            return this.tableAccessBuilder == null ? Collections.unmodifiableList(this.tableAccess) :
                    this.tableAccessBuilder.getMessageList();
        }

        public int getTableAccessCount() {
            return this.tableAccessBuilder == null ? this.tableAccess.size() : this.tableAccessBuilder.getCount();
        }

        public TableAccessStats getTableAccess(int index) {
            return this.tableAccessBuilder == null ? this.tableAccess.get(index) :
                    this.tableAccessBuilder.getMessage(index);
        }

        public Builder setTableAccess(int index, TableAccessStats value) {
            if (this.tableAccessBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureTableAccessIsMutable();
                this.tableAccess.set(index, value);
                this.onChanged();
            } else {
                this.tableAccessBuilder.setMessage(index, value);
            }

            return this;
        }

        public Builder setTableAccess(int index, TableAccessStats.Builder builderForValue) {
            if (this.tableAccessBuilder == null) {
                this.ensureTableAccessIsMutable();
                this.tableAccess.set(index, builderForValue.build());
                this.onChanged();
            } else {
                this.tableAccessBuilder.setMessage(index, builderForValue.build());
            }

            return this;
        }

        public Builder addTableAccess(TableAccessStats value) {
            if (this.tableAccessBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureTableAccessIsMutable();
                this.tableAccess.add(value);
                this.onChanged();
            } else {
                this.tableAccessBuilder.addMessage(value);
            }

            return this;
        }

        public Builder addTableAccess(int index, TableAccessStats value) {
            if (this.tableAccessBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureTableAccessIsMutable();
                this.tableAccess.add(index, value);
                this.onChanged();
            } else {
                this.tableAccessBuilder.addMessage(index, value);
            }

            return this;
        }

        public Builder addTableAccess(TableAccessStats.Builder builderForValue) {
            if (this.tableAccessBuilder == null) {
                this.ensureTableAccessIsMutable();
                this.tableAccess.add(builderForValue.build());
                this.onChanged();
            } else {
                this.tableAccessBuilder.addMessage(builderForValue.build());
            }

            return this;
        }

        public Builder addTableAccess(int index, TableAccessStats.Builder builderForValue) {
            if (this.tableAccessBuilder == null) {
                this.ensureTableAccessIsMutable();
                this.tableAccess.add(index, builderForValue.build());
                this.onChanged();
            } else {
                this.tableAccessBuilder.addMessage(index, builderForValue.build());
            }

            return this;
        }

        public Builder addAllTableAccess(Iterable<? extends TableAccessStats> values) {
            if (this.tableAccessBuilder == null) {
                this.ensureTableAccessIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(values, this.tableAccess);
                this.onChanged();
            } else {
                this.tableAccessBuilder.addAllMessages(values);
            }

            return this;
        }

        public Builder clearTableAccess() {
            if (this.tableAccessBuilder == null) {
                this.tableAccess = Collections.emptyList();
                this.bitField0 &= -2;
                this.onChanged();
            } else {
                this.tableAccessBuilder.clear();
            }

            return this;
        }

        public Builder removeTableAccess(int index) {
            if (this.tableAccessBuilder == null) {
                this.ensureTableAccessIsMutable();
                this.tableAccess.remove(index);
                this.onChanged();
            } else {
                this.tableAccessBuilder.remove(index);
            }

            return this;
        }

        public TableAccessStats.Builder getTableAccessBuilder(int index) {
            return this.getTableAccessFieldBuilder().getBuilder(index);
        }

        public TableAccessStatsOrBuilder getTableAccessOrBuilder(int index) {
            return this.tableAccessBuilder == null ? this.tableAccess.get(index) :
                    this.tableAccessBuilder.getMessageOrBuilder(index);
        }

        public List<? extends TableAccessStatsOrBuilder> getTableAccessOrBuilderList() {
            return this.tableAccessBuilder != null ? this.tableAccessBuilder.getMessageOrBuilderList() :
                    Collections.unmodifiableList(this.tableAccess);
        }

        public TableAccessStats.Builder addTableAccessBuilder() {
            return this.getTableAccessFieldBuilder().addBuilder(TableAccessStats.getDefaultInstance());
        }

        public TableAccessStats.Builder addTableAccessBuilder(int index) {
            return this.getTableAccessFieldBuilder().addBuilder(index, TableAccessStats.getDefaultInstance());
        }

        public List<TableAccessStats.Builder> getTableAccessBuilderList() {
            return this.getTableAccessFieldBuilder().getBuilderList();
        }

        private RepeatedFieldBuilderV3<TableAccessStats, TableAccessStats.Builder, TableAccessStatsOrBuilder>
        getTableAccessFieldBuilder() {
            if (this.tableAccessBuilder == null) {
                this.tableAccessBuilder = new RepeatedFieldBuilderV3<>(this.tableAccess, (this.bitField0 & 1) != 0,
                        this.getParentForChildren(), this.isClean());
                this.tableAccess = null;
            }

            return this.tableAccessBuilder;
        }

        public long getCpuTimeUs() {
            return this.cpuTimeUs;
        }

        public Builder setCpuTimeUs(long value) {
            this.cpuTimeUs = value;
            this.onChanged();
            return this;
        }

        public Builder clearCpuTimeUs() {
            this.cpuTimeUs = 0L;
            this.onChanged();
            return this;
        }

        public long getAffectedShards() {
            return this.affectedShards;
        }

        public Builder setAffectedShards(long value) {
            this.affectedShards = value;
            this.onChanged();
            return this;
        }

        public Builder clearAffectedShards() {
            this.affectedShards = 0L;
            this.onChanged();
            return this;
        }

        public boolean getLiteralPhase() {
            return this.literalPhase;
        }

        public Builder setLiteralPhase(boolean value) {
            this.literalPhase = value;
            this.onChanged();
            return this;
        }

        public Builder clearLiteralPhase() {
            this.literalPhase = false;
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
