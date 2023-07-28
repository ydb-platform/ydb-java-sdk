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
import com.google.protobuf.SingleFieldBuilderV3;
import com.google.protobuf.UnknownFieldSet;

public final class QueryStats extends GeneratedMessageV3 implements QueryStatsOrBuilder {
    public static final int QUERY_PHASES_FIELD_NUMBER = 1;
    public static final int COMPILATION_FIELD_NUMBER = 2;
    public static final int PROCESS_CPU_TIME_US_FIELD_NUMBER = 3;
    public static final int QUERY_PLAN_FIELD_NUMBER = 4;
    public static final int QUERY_AST_FIELD_NUMBER = 5;
    public static final int TOTAL_DURATION_US_FIELD_NUMBER = 6;
    public static final int TOTAL_CPU_TIME_US_FIELD_NUMBER = 7;
    private static final long serialVersionUID = 0L;
    private static final QueryStats DEFAULT_INSTANCE = new QueryStats();
    private static final Parser<QueryStats> PARSER = new AbstractParser<QueryStats>() {
        public QueryStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                throws InvalidProtocolBufferException {
            return new QueryStats(input, extensionRegistry);
        }
    };
    private List<QueryPhaseStats> queryPhases;
    private CompilationStats compilation;
    private long processCpuTimeUs;
    private volatile Object queryPlan;
    private volatile Object queryAst;
    private long totalDurationUs;
    private long totalCpuTimeUs;
    private byte memoizedIsInitialized;

    private QueryStats(GeneratedMessageV3.Builder<?> builder) {
        super(builder);
        this.memoizedIsInitialized = -1;
    }

    private QueryStats() {
        this.memoizedIsInitialized = -1;
        this.queryPhases = Collections.emptyList();
        this.queryPlan = "";
        this.queryAst = "";
    }

    public QueryStats(tech.ydb.proto.YdbQueryStats.QueryStats protoAutoGenQueryStats) {
        this.queryPhases = protoAutoGenQueryStats.getQueryPhasesList().stream().map(QueryPhaseStats::new)
                .collect(Collectors.toList());
        this.compilation = new CompilationStats(protoAutoGenQueryStats.getCompilation());
        this.processCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
        this.queryPlan = protoAutoGenQueryStats.getQueryPlan();
        this.queryAst = protoAutoGenQueryStats.getQueryAst();
        this.totalDurationUs = protoAutoGenQueryStats.getTotalDurationUs();
        this.totalCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
    }

    private QueryStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
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
                    String s;
                    switch (tag) {
                        case 0:
                            done = true;
                            break;
                        case 10:
                            if (!mutableBitField0) {
                                this.queryPhases = new ArrayList<>();
                                mutableBitField0 = true;
                            }

                            this.queryPhases.add(input.readMessage(QueryPhaseStats.parser(), extensionRegistry));
                            break;
                        case 18:
                            CompilationStats.Builder subBuilder = null;
                            if (this.compilation != null) {
                                subBuilder = this.compilation.toBuilder();
                            }

                            this.compilation = input.readMessage(CompilationStats.parser(), extensionRegistry);
                            if (subBuilder != null) {
                                subBuilder.mergeFrom(this.compilation);
                                this.compilation = subBuilder.buildPartial();
                            }
                            break;
                        case 24:
                            this.processCpuTimeUs = input.readUInt64();
                            break;
                        case 34:
                            s = input.readStringRequireUtf8();
                            this.queryPlan = s;
                            break;
                        case 42:
                            s = input.readStringRequireUtf8();
                            this.queryAst = s;
                            break;
                        case 48:
                            this.totalDurationUs = input.readUInt64();
                            break;
                        case 56:
                            this.totalCpuTimeUs = input.readUInt64();
                            break;
                        default:
                            if (!this.parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
                                done = true;
                            }
                    }
                }
            } catch (InvalidProtocolBufferException var12) {
                throw var12.setUnfinishedMessage(this);
            } catch (IOException var13) {
                throw (new InvalidProtocolBufferException(var13)).setUnfinishedMessage(this);
            } finally {
                if (mutableBitField0) {
                    this.queryPhases = Collections.unmodifiableList(this.queryPhases);
                }

                this.unknownFields = unknownFields.build();
                this.makeExtensionsImmutable();
            }

        }
    }

    public static Descriptors.Descriptor getDescriptor() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR;
    }

    public static QueryStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static QueryStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static QueryStats parseFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static QueryStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static QueryStats parseDelimitedFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static QueryStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static QueryStats parseFrom(CodedInputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static QueryStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(QueryStats prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static QueryStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Parser<QueryStats> parser() {
        return PARSER;
    }

    protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
        return new QueryStats();
    }

    public UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(QueryStats.class, Builder.class);
    }

    public List<QueryPhaseStats> getQueryPhasesList() {
        return this.queryPhases;
    }

    public List<? extends QueryPhaseStatsOrBuilder> getQueryPhasesOrBuilderList() {
        return this.queryPhases;
    }

    public int getQueryPhasesCount() {
        return this.queryPhases.size();
    }

    public QueryPhaseStats getQueryPhases(int index) {
        return this.queryPhases.get(index);
    }

    public QueryPhaseStatsOrBuilder getQueryPhasesOrBuilder(int index) {
        return this.queryPhases.get(index);
    }

    public boolean hasCompilation() {
        return this.compilation != null;
    }

    public CompilationStats getCompilation() {
        return this.compilation == null ? CompilationStats.getDefaultInstance() : this.compilation;
    }

    public CompilationStatsOrBuilder getCompilationOrBuilder() {
        return this.getCompilation();
    }

    public long getProcessCpuTimeUs() {
        return this.processCpuTimeUs;
    }

    public String getQueryPlan() {
        Object ref = this.queryPlan;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            ByteString bs = (ByteString) ref;
            String s = bs.toStringUtf8();
            this.queryPlan = s;
            return s;
        }
    }

    public ByteString getQueryPlanBytes() {
        Object ref = this.queryPlan;
        if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String) ref);
            this.queryPlan = b;
            return b;
        } else {
            return (ByteString) ref;
        }
    }

    public String getQueryAst() {
        Object ref = this.queryAst;
        if (ref instanceof String) {
            return (String) ref;
        } else {
            ByteString bs = (ByteString) ref;
            String s = bs.toStringUtf8();
            this.queryAst = s;
            return s;
        }
    }

    public ByteString getQueryAstBytes() {
        Object ref = this.queryAst;
        if (ref instanceof String) {
            ByteString b = ByteString.copyFromUtf8((String) ref);
            this.queryAst = b;
            return b;
        } else {
            return (ByteString) ref;
        }
    }

    public long getTotalDurationUs() {
        return this.totalDurationUs;
    }

    public long getTotalCpuTimeUs() {
        return this.totalCpuTimeUs;
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
        for (QueryPhaseStats queryPhaseStats : this.queryPhases) {
            output.writeMessage(1, queryPhaseStats);
        }

        if (this.compilation != null) {
            output.writeMessage(2, this.getCompilation());
        }

        if (this.processCpuTimeUs != 0L) {
            output.writeUInt64(3, this.processCpuTimeUs);
        }

        if (!GeneratedMessageV3.isStringEmpty(this.queryPlan)) {
            GeneratedMessageV3.writeString(output, 4, this.queryPlan);
        }

        if (!GeneratedMessageV3.isStringEmpty(this.queryAst)) {
            GeneratedMessageV3.writeString(output, 5, this.queryAst);
        }

        if (this.totalDurationUs != 0L) {
            output.writeUInt64(6, this.totalDurationUs);
        }

        if (this.totalCpuTimeUs != 0L) {
            output.writeUInt64(7, this.totalCpuTimeUs);
        }

        this.unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
        int size = this.memoizedSize;
        if (size == -1) {
            size = 0;

            for (QueryPhaseStats queryPhaseStats : this.queryPhases) {
                size += CodedOutputStream.computeMessageSize(1, queryPhaseStats);
            }

            if (this.compilation != null) {
                size += CodedOutputStream.computeMessageSize(2, this.getCompilation());
            }

            if (this.processCpuTimeUs != 0L) {
                size += CodedOutputStream.computeUInt64Size(3, this.processCpuTimeUs);
            }

            if (!GeneratedMessageV3.isStringEmpty(this.queryPlan)) {
                size += GeneratedMessageV3.computeStringSize(4, this.queryPlan);
            }

            if (!GeneratedMessageV3.isStringEmpty(this.queryAst)) {
                size += GeneratedMessageV3.computeStringSize(5, this.queryAst);
            }

            if (this.totalDurationUs != 0L) {
                size += CodedOutputStream.computeUInt64Size(6, this.totalDurationUs);
            }

            if (this.totalCpuTimeUs != 0L) {
                size += CodedOutputStream.computeUInt64Size(7, this.totalCpuTimeUs);
            }

            size += this.unknownFields.getSerializedSize();
            this.memoizedSize = size;
        }
        return size;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof QueryStats)) {
            return super.equals(obj);
        } else {
            QueryStats other = (QueryStats) obj;
            if (!this.getQueryPhasesList().equals(other.getQueryPhasesList())) {
                return false;
            } else if (this.hasCompilation() != other.hasCompilation()) {
                return false;
            } else if (this.hasCompilation() && !this.getCompilation().equals(other.getCompilation())) {
                return false;
            } else if (this.getProcessCpuTimeUs() != other.getProcessCpuTimeUs()) {
                return false;
            } else if (!this.getQueryPlan().equals(other.getQueryPlan())) {
                return false;
            } else if (!this.getQueryAst().equals(other.getQueryAst())) {
                return false;
            } else if (this.getTotalDurationUs() != other.getTotalDurationUs()) {
                return false;
            } else if (this.getTotalCpuTimeUs() != other.getTotalCpuTimeUs()) {
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
            if (this.getQueryPhasesCount() > 0) {
                hash = 37 * hash + 1;
                hash = 53 * hash + this.getQueryPhasesList().hashCode();
            }

            if (this.hasCompilation()) {
                hash = 37 * hash + 2;
                hash = 53 * hash + this.getCompilation().hashCode();
            }

            hash = 37 * hash + 3;
            hash = 53 * hash + Internal.hashLong(this.getProcessCpuTimeUs());
            hash = 37 * hash + 4;
            hash = 53 * hash + this.getQueryPlan().hashCode();
            hash = 37 * hash + 5;
            hash = 53 * hash + this.getQueryAst().hashCode();
            hash = 37 * hash + 6;
            hash = 53 * hash + Internal.hashLong(this.getTotalDurationUs());
            hash = 37 * hash + 7;
            hash = 53 * hash + Internal.hashLong(this.getTotalCpuTimeUs());
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

    public Parser<QueryStats> getParserForType() {
        return PARSER;
    }

    public QueryStats getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    public static final class Builder extends GeneratedMessageV3.Builder<Builder> implements QueryStatsOrBuilder {
        private int bitField0;
        private List<QueryPhaseStats> queryPhases;
        private RepeatedFieldBuilderV3<QueryPhaseStats, QueryPhaseStats.Builder, QueryPhaseStatsOrBuilder>
                queryPhasesBuilder;
        private CompilationStats compilation;
        private SingleFieldBuilderV3<CompilationStats, CompilationStats.Builder, CompilationStatsOrBuilder>
                compilationBuilder;
        private long processCpuTimeUs;
        private Object queryPlan;
        private Object queryAst;
        private long totalDurationUs;
        private long totalCpuTimeUs;

        private Builder() {
            this.queryPhases = Collections.emptyList();
            this.queryPlan = "";
            this.queryAst = "";
            this.maybeForceBuilderInitialization();
        }

        private Builder(GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            this.queryPhases = Collections.emptyList();
            this.queryPlan = "";
            this.queryAst = "";
            this.maybeForceBuilderInitialization();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(QueryStats.class, Builder.class);
        }

        private void maybeForceBuilderInitialization() {
            if (QueryStats.alwaysUseFieldBuilders) {
                this.getQueryPhasesFieldBuilder();
            }

        }

        public Builder clear() {
            super.clear();
            if (this.queryPhasesBuilder == null) {
                this.queryPhases = Collections.emptyList();
                this.bitField0 &= -2;
            } else {
                this.queryPhasesBuilder.clear();
            }

            if (this.compilationBuilder == null) {
                this.compilation = null;
            } else {
                this.compilation = null;
                this.compilationBuilder = null;
            }

            this.processCpuTimeUs = 0L;
            this.queryPlan = "";
            this.queryAst = "";
            this.totalDurationUs = 0L;
            this.totalCpuTimeUs = 0L;
            return this;
        }

        public Descriptors.Descriptor getDescriptorForType() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR;
        }

        public QueryStats getDefaultInstanceForType() {
            return QueryStats.getDefaultInstance();
        }

        public QueryStats build() {
            QueryStats result = this.buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            } else {
                return result;
            }
        }

        public QueryStats buildPartial() {
            QueryStats result = new QueryStats(this);
            if (this.queryPhasesBuilder == null) {
                if ((this.bitField0 & 1) != 0) {
                    this.queryPhases = Collections.unmodifiableList(this.queryPhases);
                    this.bitField0 &= -2;
                }

                result.queryPhases = this.queryPhases;
            } else {
                result.queryPhases = this.queryPhasesBuilder.build();
            }

            if (this.compilationBuilder == null) {
                result.compilation = this.compilation;
            } else {
                result.compilation = this.compilationBuilder.build();
            }

            result.processCpuTimeUs = this.processCpuTimeUs;
            result.queryPlan = this.queryPlan;
            result.queryAst = this.queryAst;
            result.totalDurationUs = this.totalDurationUs;
            result.totalCpuTimeUs = this.totalCpuTimeUs;
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
            if (other instanceof QueryStats) {
                return this.mergeFrom((QueryStats) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(QueryStats other) {
            if (other != QueryStats.getDefaultInstance()) {
                if (this.queryPhasesBuilder == null) {
                    if (!other.queryPhases.isEmpty()) {
                        if (this.queryPhases.isEmpty()) {
                            this.queryPhases = other.queryPhases;
                            this.bitField0 &= -2;
                        } else {
                            this.ensureQueryPhasesIsMutable();
                            this.queryPhases.addAll(other.queryPhases);
                        }

                        this.onChanged();
                    }
                } else if (!other.queryPhases.isEmpty()) {
                    if (this.queryPhasesBuilder.isEmpty()) {
                        this.queryPhasesBuilder.dispose();
                        this.queryPhasesBuilder = null;
                        this.queryPhases = other.queryPhases;
                        this.bitField0 &= -2;
                        this.queryPhasesBuilder =
                                QueryStats.alwaysUseFieldBuilders ? this.getQueryPhasesFieldBuilder() : null;
                    } else {
                        this.queryPhasesBuilder.addAllMessages(other.queryPhases);
                    }
                }

                if (other.hasCompilation()) {
                    this.mergeCompilation(other.getCompilation());
                }

                if (other.getProcessCpuTimeUs() != 0L) {
                    this.setProcessCpuTimeUs(other.getProcessCpuTimeUs());
                }

                if (!other.getQueryPlan().isEmpty()) {
                    this.queryPlan = other.queryPlan;
                    this.onChanged();
                }

                if (!other.getQueryAst().isEmpty()) {
                    this.queryAst = other.queryAst;
                    this.onChanged();
                }

                if (other.getTotalDurationUs() != 0L) {
                    this.setTotalDurationUs(other.getTotalDurationUs());
                }

                if (other.getTotalCpuTimeUs() != 0L) {
                    this.setTotalCpuTimeUs(other.getTotalCpuTimeUs());
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
            QueryStats parsedMessage = null;

            try {
                parsedMessage = QueryStats.PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (InvalidProtocolBufferException var8) {
                parsedMessage = (QueryStats) var8.getUnfinishedMessage();
                throw var8.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    this.mergeFrom(parsedMessage);
                }

            }

            return this;
        }

        private void ensureQueryPhasesIsMutable() {
            if ((this.bitField0 & 1) == 0) {
                this.queryPhases = new ArrayList<>(this.queryPhases);
                this.bitField0 |= 1;
            }

        }

        public List<QueryPhaseStats> getQueryPhasesList() {
            return this.queryPhasesBuilder == null ? Collections.unmodifiableList(this.queryPhases) :
                    this.queryPhasesBuilder.getMessageList();
        }

        public int getQueryPhasesCount() {
            return this.queryPhasesBuilder == null ? this.queryPhases.size() : this.queryPhasesBuilder.getCount();
        }

        public QueryPhaseStats getQueryPhases(int index) {
            return this.queryPhasesBuilder == null ? this.queryPhases.get(index) :
                    this.queryPhasesBuilder.getMessage(index);
        }

        public Builder setQueryPhases(int index, QueryPhaseStats value) {
            if (this.queryPhasesBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureQueryPhasesIsMutable();
                this.queryPhases.set(index, value);
                this.onChanged();
            } else {
                this.queryPhasesBuilder.setMessage(index, value);
            }

            return this;
        }

        public Builder setQueryPhases(int index, QueryPhaseStats.Builder builderForValue) {
            if (this.queryPhasesBuilder == null) {
                this.ensureQueryPhasesIsMutable();
                this.queryPhases.set(index, builderForValue.build());
                this.onChanged();
            } else {
                this.queryPhasesBuilder.setMessage(index, builderForValue.build());
            }

            return this;
        }

        public Builder addQueryPhases(QueryPhaseStats value) {
            if (this.queryPhasesBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureQueryPhasesIsMutable();
                this.queryPhases.add(value);
                this.onChanged();
            } else {
                this.queryPhasesBuilder.addMessage(value);
            }

            return this;
        }

        public Builder addQueryPhases(int index, QueryPhaseStats value) {
            if (this.queryPhasesBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.ensureQueryPhasesIsMutable();
                this.queryPhases.add(index, value);
                this.onChanged();
            } else {
                this.queryPhasesBuilder.addMessage(index, value);
            }

            return this;
        }

        public Builder addQueryPhases(QueryPhaseStats.Builder builderForValue) {
            if (this.queryPhasesBuilder == null) {
                this.ensureQueryPhasesIsMutable();
                this.queryPhases.add(builderForValue.build());
                this.onChanged();
            } else {
                this.queryPhasesBuilder.addMessage(builderForValue.build());
            }

            return this;
        }

        public Builder addQueryPhases(int index, QueryPhaseStats.Builder builderForValue) {
            if (this.queryPhasesBuilder == null) {
                this.ensureQueryPhasesIsMutable();
                this.queryPhases.add(index, builderForValue.build());
                this.onChanged();
            } else {
                this.queryPhasesBuilder.addMessage(index, builderForValue.build());
            }

            return this;
        }

        public Builder addAllQueryPhases(Iterable<? extends QueryPhaseStats> values) {
            if (this.queryPhasesBuilder == null) {
                this.ensureQueryPhasesIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(values, this.queryPhases);
                this.onChanged();
            } else {
                this.queryPhasesBuilder.addAllMessages(values);
            }

            return this;
        }

        public Builder clearQueryPhases() {
            if (this.queryPhasesBuilder == null) {
                this.queryPhases = Collections.emptyList();
                this.bitField0 &= -2;
                this.onChanged();
            } else {
                this.queryPhasesBuilder.clear();
            }

            return this;
        }

        public Builder removeQueryPhases(int index) {
            if (this.queryPhasesBuilder == null) {
                this.ensureQueryPhasesIsMutable();
                this.queryPhases.remove(index);
                this.onChanged();
            } else {
                this.queryPhasesBuilder.remove(index);
            }

            return this;
        }

        public QueryPhaseStats.Builder getQueryPhasesBuilder(int index) {
            return this.getQueryPhasesFieldBuilder().getBuilder(index);
        }

        public QueryPhaseStatsOrBuilder getQueryPhasesOrBuilder(int index) {
            return this.queryPhasesBuilder == null ? this.queryPhases.get(index) :
                    this.queryPhasesBuilder.getMessageOrBuilder(index);
        }

        public List<? extends QueryPhaseStatsOrBuilder> getQueryPhasesOrBuilderList() {
            return this.queryPhasesBuilder != null ? this.queryPhasesBuilder.getMessageOrBuilderList() :
                    Collections.unmodifiableList(this.queryPhases);
        }

        public QueryPhaseStats.Builder addQueryPhasesBuilder() {
            return this.getQueryPhasesFieldBuilder().addBuilder(QueryPhaseStats.getDefaultInstance());
        }

        public QueryPhaseStats.Builder addQueryPhasesBuilder(int index) {
            return this.getQueryPhasesFieldBuilder().addBuilder(index, QueryPhaseStats.getDefaultInstance());
        }

        public List<QueryPhaseStats.Builder> getQueryPhasesBuilderList() {
            return this.getQueryPhasesFieldBuilder().getBuilderList();
        }

        private RepeatedFieldBuilderV3<QueryPhaseStats, QueryPhaseStats.Builder, QueryPhaseStatsOrBuilder>
        getQueryPhasesFieldBuilder() {
            if (this.queryPhasesBuilder == null) {
                this.queryPhasesBuilder = new RepeatedFieldBuilderV3<>(this.queryPhases, (this.bitField0 & 1) != 0,
                        this.getParentForChildren(), this.isClean());
                this.queryPhases = null;
            }

            return this.queryPhasesBuilder;
        }

        public boolean hasCompilation() {
            return this.compilationBuilder != null || this.compilation != null;
        }

        public CompilationStats getCompilation() {
            if (this.compilationBuilder == null) {
                return this.compilation == null ? CompilationStats.getDefaultInstance() : this.compilation;
            } else {
                return this.compilationBuilder.getMessage();
            }
        }

        public Builder setCompilation(CompilationStats value) {
            if (this.compilationBuilder == null) {
                if (value == null) {
                    throw new NullPointerException();
                }

                this.compilation = value;
                this.onChanged();
            } else {
                this.compilationBuilder.setMessage(value);
            }

            return this;
        }

        public Builder setCompilation(CompilationStats.Builder builderForValue) {
            if (this.compilationBuilder == null) {
                this.compilation = builderForValue.build();
                this.onChanged();
            } else {
                this.compilationBuilder.setMessage(builderForValue.build());
            }

            return this;
        }

        public Builder mergeCompilation(CompilationStats value) {
            if (this.compilationBuilder == null) {
                if (this.compilation != null) {
                    this.compilation = CompilationStats.newBuilder(this.compilation).mergeFrom(value).buildPartial();
                } else {
                    this.compilation = value;
                }

                this.onChanged();
            } else {
                this.compilationBuilder.mergeFrom(value);
            }

            return this;
        }

        public Builder clearCompilation() {
            if (this.compilationBuilder == null) {
                this.compilation = null;
                this.onChanged();
            } else {
                this.compilation = null;
                this.compilationBuilder = null;
            }

            return this;
        }

        public CompilationStats.Builder getCompilationBuilder() {
            this.onChanged();
            return this.getCompilationFieldBuilder().getBuilder();
        }

        public CompilationStatsOrBuilder getCompilationOrBuilder() {
            if (this.compilationBuilder != null) {
                return this.compilationBuilder.getMessageOrBuilder();
            } else {
                return this.compilation == null ? CompilationStats.getDefaultInstance() : this.compilation;
            }
        }

        private SingleFieldBuilderV3<CompilationStats, CompilationStats.Builder, CompilationStatsOrBuilder>
        getCompilationFieldBuilder() {
            if (this.compilationBuilder == null) {
                this.compilationBuilder =
                        new SingleFieldBuilderV3(this.getCompilation(), this.getParentForChildren(), this.isClean());
                this.compilation = null;
            }

            return this.compilationBuilder;
        }

        public long getProcessCpuTimeUs() {
            return this.processCpuTimeUs;
        }

        public Builder setProcessCpuTimeUs(long value) {
            this.processCpuTimeUs = value;
            this.onChanged();
            return this;
        }

        public Builder clearProcessCpuTimeUs() {
            this.processCpuTimeUs = 0L;
            this.onChanged();
            return this;
        }

        public String getQueryPlan() {
            Object ref = this.queryPlan;
            if (!(ref instanceof String)) {
                ByteString bs = (ByteString) ref;
                String s = bs.toStringUtf8();
                this.queryPlan = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        public Builder setQueryPlan(String value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                this.queryPlan = value;
                this.onChanged();
                return this;
            }
        }

        public ByteString getQueryPlanBytes() {
            Object ref = this.queryPlan;
            if (ref instanceof String) {
                ByteString b = ByteString.copyFromUtf8((String) ref);
                this.queryPlan = b;
                return b;
            } else {
                return (ByteString) ref;
            }
        }

        public Builder setQueryPlanBytes(ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                QueryStats.checkByteStringIsUtf8(value);
                this.queryPlan = value;
                this.onChanged();
                return this;
            }
        }

        public Builder clearQueryPlan() {
            this.queryPlan = QueryStats.getDefaultInstance().getQueryPlan();
            this.onChanged();
            return this;
        }

        public String getQueryAst() {
            Object ref = this.queryAst;
            if (!(ref instanceof String)) {
                ByteString bs = (ByteString) ref;
                String s = bs.toStringUtf8();
                this.queryAst = s;
                return s;
            } else {
                return (String) ref;
            }
        }

        public Builder setQueryAst(String value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                this.queryAst = value;
                this.onChanged();
                return this;
            }
        }

        public ByteString getQueryAstBytes() {
            Object ref = this.queryAst;
            if (ref instanceof String) {
                ByteString b = ByteString.copyFromUtf8((String) ref);
                this.queryAst = b;
                return b;
            } else {
                return (ByteString) ref;
            }
        }

        public Builder setQueryAstBytes(ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            } else {
                QueryStats.checkByteStringIsUtf8(value);
                this.queryAst = value;
                this.onChanged();
                return this;
            }
        }

        public Builder clearQueryAst() {
            this.queryAst = QueryStats.getDefaultInstance().getQueryAst();
            this.onChanged();
            return this;
        }

        public long getTotalDurationUs() {
            return this.totalDurationUs;
        }

        public Builder setTotalDurationUs(long value) {
            this.totalDurationUs = value;
            this.onChanged();
            return this;
        }

        public Builder clearTotalDurationUs() {
            this.totalDurationUs = 0L;
            this.onChanged();
            return this;
        }

        public long getTotalCpuTimeUs() {
            return this.totalCpuTimeUs;
        }

        public Builder setTotalCpuTimeUs(long value) {
            this.totalCpuTimeUs = value;
            this.onChanged();
            return this;
        }

        public Builder clearTotalCpuTimeUs() {
            this.totalCpuTimeUs = 0L;
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
