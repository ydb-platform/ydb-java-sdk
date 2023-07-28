package tech.ydb.table.query;

import com.google.protobuf.AbstractParser;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Parser;
import com.google.protobuf.RepeatedFieldBuilderV3;
import com.google.protobuf.SingleFieldBuilderV3;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.Descriptors.FileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class YdbQueryStats {
    private static final Descriptors.Descriptor internal_static_Ydb_TableStats_OperationStats_descriptor;
    private static final GeneratedMessageV3.FieldAccessorTable internal_static_Ydb_TableStats_OperationStats_fieldAccessorTable;
    private static final Descriptors.Descriptor internal_static_Ydb_TableStats_TableAccessStats_descriptor;
    private static final GeneratedMessageV3.FieldAccessorTable internal_static_Ydb_TableStats_TableAccessStats_fieldAccessorTable;
    private static final Descriptors.Descriptor internal_static_Ydb_TableStats_QueryPhaseStats_descriptor;
    private static final GeneratedMessageV3.FieldAccessorTable internal_static_Ydb_TableStats_QueryPhaseStats_fieldAccessorTable;
    private static final Descriptors.Descriptor internal_static_Ydb_TableStats_CompilationStats_descriptor;
    private static final GeneratedMessageV3.FieldAccessorTable internal_static_Ydb_TableStats_CompilationStats_fieldAccessorTable;
    private static final Descriptors.Descriptor internal_static_Ydb_TableStats_QueryStats_descriptor;
    private static final GeneratedMessageV3.FieldAccessorTable internal_static_Ydb_TableStats_QueryStats_fieldAccessorTable;
    private static final Descriptors.FileDescriptor descriptor;

    private YdbQueryStats() {
    }

    public static void registerAllExtensions(ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(ExtensionRegistry registry) {
        registerAllExtensions((ExtensionRegistryLite) registry);
    }

    public static Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    static {
        String[] descriptorData = new String[]{"\n\u001cprotos/ydb_query_stats.proto\u0012\u000eYdb.TableStats\"-\n\u000eOperationStats\u0012\f\n\u0004rows\u0018\u0001 \u0001(\u0004\u0012\r\n\u0005bytes\u0018\u0002 \u0001(\u0004\"Ñ\u0001\n\u0010TableAccessStats\u0012\f\n\u0004name\u0018\u0001 \u0001(\t\u0012-\n\u0005reads\u0018\u0003 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats\u0012/\n\u0007updates\u0018\u0004 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats\u0012/\n\u0007deletes\u0018\u0005 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats\u0012\u0018\n\u0010partitions_count\u0018\u0006 \u0001(\u0004J\u0004\b\u0002\u0010\u0003\"£\u0001\n\u000fQueryPhaseStats\u0012\u0013\n\u000bduration_us\u0018\u0001 \u0001(\u0004\u00126\n\ftable_access\u0018\u0002 \u0003(\u000b2 .Ydb.TableStats.TableAccessStats\u0012\u0013\n\u000bcpu_time_us\u0018\u0003 \u0001(\u0004\u0012\u0017\n\u000faffected_shards\u0018\u0004 \u0001(\u0004\u0012\u0015\n\rliteral_phase\u0018\u0005 \u0001(\b\"P\n\u0010CompilationStats\u0012\u0012\n\nfrom_cache\u0018\u0001 \u0001(\b\u0012\u0013\n\u000bduration_us\u0018\u0002 \u0001(\u0004\u0012\u0013\n\u000bcpu_time_us\u0018\u0003 \u0001(\u0004\"ô\u0001\n\nQueryStats\u00125\n\fquery_phases\u0018\u0001 \u0003(\u000b2\u001f.Ydb.TableStats.QueryPhaseStats\u00125\n\u000bcompilation\u0018\u0002 \u0001(\u000b2 .Ydb.TableStats.CompilationStats\u0012\u001b\n\u0013process_cpu_time_us\u0018\u0003 \u0001(\u0004\u0012\u0012\n\nquery_plan\u0018\u0004 \u0001(\t\u0012\u0011\n\tquery_ast\u0018\u0005 \u0001(\t\u0012\u0019\n\u0011total_duration_us\u0018\u0006 \u0001(\u0004\u0012\u0019\n\u0011total_cpu_time_us\u0018\u0007 \u0001(\u0004BR\n\u000etech.ydb.protoZ=github.com/ydb-platform/ydb-go-genproto/protos/Ydb_TableStatsø\u0001\u0001b\u0006proto3"};
        descriptor = FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new Descriptors.FileDescriptor[0]);
        internal_static_Ydb_TableStats_OperationStats_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_Ydb_TableStats_OperationStats_fieldAccessorTable = new GeneratedMessageV3.FieldAccessorTable(internal_static_Ydb_TableStats_OperationStats_descriptor, new String[]{"Rows", "Bytes"});
        internal_static_Ydb_TableStats_TableAccessStats_descriptor = getDescriptor().getMessageTypes().get(1);
        internal_static_Ydb_TableStats_TableAccessStats_fieldAccessorTable = new GeneratedMessageV3.FieldAccessorTable(internal_static_Ydb_TableStats_TableAccessStats_descriptor, new String[]{"Name", "Reads", "Updates", "Deletes", "PartitionsCount"});
        internal_static_Ydb_TableStats_QueryPhaseStats_descriptor = getDescriptor().getMessageTypes().get(2);
        internal_static_Ydb_TableStats_QueryPhaseStats_fieldAccessorTable = new GeneratedMessageV3.FieldAccessorTable(internal_static_Ydb_TableStats_QueryPhaseStats_descriptor, new String[]{"DurationUs", "TableAccess", "CpuTimeUs", "AffectedShards", "LiteralPhase"});
        internal_static_Ydb_TableStats_CompilationStats_descriptor = getDescriptor().getMessageTypes().get(3);
        internal_static_Ydb_TableStats_CompilationStats_fieldAccessorTable = new GeneratedMessageV3.FieldAccessorTable(internal_static_Ydb_TableStats_CompilationStats_descriptor, new String[]{"FromCache", "DurationUs", "CpuTimeUs"});
        internal_static_Ydb_TableStats_QueryStats_descriptor = getDescriptor().getMessageTypes().get(4);
        internal_static_Ydb_TableStats_QueryStats_fieldAccessorTable = new GeneratedMessageV3.FieldAccessorTable(internal_static_Ydb_TableStats_QueryStats_descriptor, new String[]{"QueryPhases", "Compilation", "ProcessCpuTimeUs", "QueryPlan", "QueryAst", "TotalDurationUs", "TotalCpuTimeUs"});
    }

    public static final class QueryStats extends GeneratedMessageV3 implements QueryStatsOrBuilder {
        private static final long serialVersionUID = 0L;
        public static final int QUERY_PHASES_FIELD_NUMBER = 1;
        private List<QueryPhaseStats> queryPhases;
        public static final int COMPILATION_FIELD_NUMBER = 2;
        private CompilationStats compilation;
        public static final int PROCESS_CPU_TIME_US_FIELD_NUMBER = 3;
        private long processCpuTimeUs;
        public static final int QUERY_PLAN_FIELD_NUMBER = 4;
        private volatile Object queryPlan;
        public static final int QUERY_AST_FIELD_NUMBER = 5;
        private volatile Object queryAst;
        public static final int TOTAL_DURATION_US_FIELD_NUMBER = 6;
        private long totalDurationUs;
        public static final int TOTAL_CPU_TIME_US_FIELD_NUMBER = 7;
        private long totalCpuTimeUs;
        private byte memoizedIsInitialized;
        private static final QueryStats DEFAULT_INSTANCE = new QueryStats();
        private static final Parser<QueryStats> PARSER = new AbstractParser<QueryStats>() {
            public QueryStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                    throws InvalidProtocolBufferException {
                return new QueryStats(input, extensionRegistry);
            }
        };

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
            this.queryPhases = protoAutoGenQueryStats.getQueryPhasesList().stream()
                    .map(QueryPhaseStats::new)
                    .collect(Collectors.toList());
            this.compilation = new CompilationStats(protoAutoGenQueryStats.getCompilation());
            this.processCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
            this.queryPlan = protoAutoGenQueryStats.getQueryPlan();
            this.queryAst = protoAutoGenQueryStats.getQueryAst();
            this.totalDurationUs = protoAutoGenQueryStats.getTotalDurationUs();
            this.totalCpuTimeUs = protoAutoGenQueryStats.getProcessCpuTimeUs();
        }

        protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
            return new QueryStats();
        }

        public UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
        }

        private QueryStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            this();
            if (extensionRegistry == null) {
                throw new NullPointerException();
            } else {
                boolean mutable_bitField0_ = false;
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
                                if (!mutable_bitField0_) {
                                    this.queryPhases = new ArrayList<>();
                                    mutable_bitField0_ = true;
                                }

                                this.queryPhases.add(input.readMessage(YdbQueryStats.QueryPhaseStats.parser(), extensionRegistry));
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
                    if (mutable_bitField0_) {
                        this.queryPhases = Collections.unmodifiableList(this.queryPhases);
                    }

                    this.unknownFields = unknownFields.build();
                    this.makeExtensionsImmutable();
                }

            }
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.internal_static_Ydb_TableStats_QueryStats_descriptor;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.internal_static_Ydb_TableStats_QueryStats_fieldAccessorTable.ensureFieldAccessorsInitialized(QueryStats.class, Builder.class);
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
            return this.compilation == null ? YdbQueryStats.CompilationStats.getDefaultInstance() : this.compilation;
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

        public static QueryStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static QueryStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static QueryStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static QueryStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static QueryStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static QueryStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
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

        public static QueryStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static QueryStats parseFrom(CodedInputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static QueryStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(QueryStats prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
        }

        protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
            return new Builder(parent);
        }

        public static QueryStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<QueryStats> parser() {
            return PARSER;
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
            private RepeatedFieldBuilderV3<QueryPhaseStats, QueryPhaseStats.Builder, QueryPhaseStatsOrBuilder> queryPhasesBuilder;
            private CompilationStats compilation;
            private SingleFieldBuilderV3<CompilationStats, CompilationStats.Builder, CompilationStatsOrBuilder> compilationBuilder;
            private long processCpuTimeUs;
            private Object queryPlan;
            private Object queryAst;
            private long totalDurationUs;
            private long totalCpuTimeUs;

            public static Descriptors.Descriptor getDescriptor() {
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryStats_descriptor;
            }

            protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryStats_fieldAccessorTable.ensureFieldAccessorsInitialized(QueryStats.class, Builder.class);
            }

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

            private void maybeForceBuilderInitialization() {
                if (YdbQueryStats.QueryStats.alwaysUseFieldBuilders) {
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
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryStats_descriptor;
            }

            public QueryStats getDefaultInstanceForType() {
                return YdbQueryStats.QueryStats.getDefaultInstance();
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
                int from_bitField0_ = this.bitField0;
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
                            this.queryPhasesBuilder = QueryStats.alwaysUseFieldBuilders ? this.getQueryPhasesFieldBuilder() : null;
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
                return this.queryPhasesBuilder == null ? Collections.unmodifiableList(this.queryPhases) : this.queryPhasesBuilder.getMessageList();
            }

            public int getQueryPhasesCount() {
                return this.queryPhasesBuilder == null ? this.queryPhases.size() : this.queryPhasesBuilder.getCount();
            }

            public QueryPhaseStats getQueryPhases(int index) {
                return this.queryPhasesBuilder == null ? this.queryPhases.get(index) : this.queryPhasesBuilder.getMessage(index);
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
                return this.queryPhasesBuilder == null ? this.queryPhases.get(index) : this.queryPhasesBuilder.getMessageOrBuilder(index);
            }

            public List<? extends QueryPhaseStatsOrBuilder> getQueryPhasesOrBuilderList() {
                return this.queryPhasesBuilder != null ? this.queryPhasesBuilder.getMessageOrBuilderList() : Collections.unmodifiableList(this.queryPhases);
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

            private RepeatedFieldBuilderV3<QueryPhaseStats, QueryPhaseStats.Builder, QueryPhaseStatsOrBuilder> getQueryPhasesFieldBuilder() {
                if (this.queryPhasesBuilder == null) {
                    this.queryPhasesBuilder = new RepeatedFieldBuilderV3<>(this.queryPhases, (this.bitField0 & 1) != 0, this.getParentForChildren(), this.isClean());
                    this.queryPhases = null;
                }

                return this.queryPhasesBuilder;
            }

            public boolean hasCompilation() {
                return this.compilationBuilder != null || this.compilation != null;
            }

            public CompilationStats getCompilation() {
                if (this.compilationBuilder == null) {
                    return this.compilation == null ? YdbQueryStats.CompilationStats.getDefaultInstance() : this.compilation;
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
                        this.compilation = YdbQueryStats.CompilationStats.newBuilder(this.compilation).mergeFrom(value).buildPartial();
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
                    return this.compilation == null ? YdbQueryStats.CompilationStats.getDefaultInstance() : this.compilation;
                }
            }

            private SingleFieldBuilderV3<CompilationStats, CompilationStats.Builder, CompilationStatsOrBuilder> getCompilationFieldBuilder() {
                if (this.compilationBuilder == null) {
                    this.compilationBuilder = new SingleFieldBuilderV3(this.getCompilation(), this.getParentForChildren(), this.isClean());
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

            public Builder setQueryPlan(String value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    this.queryPlan = value;
                    this.onChanged();
                    return this;
                }
            }

            public Builder clearQueryPlan() {
                this.queryPlan = YdbQueryStats.QueryStats.getDefaultInstance().getQueryPlan();
                this.onChanged();
                return this;
            }

            public Builder setQueryPlanBytes(ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    YdbQueryStats.QueryStats.checkByteStringIsUtf8(value);
                    this.queryPlan = value;
                    this.onChanged();
                    return this;
                }
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

            public Builder setQueryAst(String value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    this.queryAst = value;
                    this.onChanged();
                    return this;
                }
            }

            public Builder clearQueryAst() {
                this.queryAst = YdbQueryStats.QueryStats.getDefaultInstance().getQueryAst();
                this.onChanged();
                return this;
            }

            public Builder setQueryAstBytes(ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    YdbQueryStats.QueryStats.checkByteStringIsUtf8(value);
                    this.queryAst = value;
                    this.onChanged();
                    return this;
                }
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

    public interface QueryStatsOrBuilder extends MessageOrBuilder {
        List<QueryPhaseStats> getQueryPhasesList();

        QueryPhaseStats getQueryPhases(int var1);

        int getQueryPhasesCount();

        List<? extends QueryPhaseStatsOrBuilder> getQueryPhasesOrBuilderList();

        QueryPhaseStatsOrBuilder getQueryPhasesOrBuilder(int var1);

        boolean hasCompilation();

        CompilationStats getCompilation();

        CompilationStatsOrBuilder getCompilationOrBuilder();

        long getProcessCpuTimeUs();

        String getQueryPlan();

        ByteString getQueryPlanBytes();

        String getQueryAst();

        ByteString getQueryAstBytes();

        long getTotalDurationUs();

        long getTotalCpuTimeUs();
    }

    public static final class CompilationStats extends GeneratedMessageV3 implements CompilationStatsOrBuilder {
        private static final long serialVersionUID = 0L;
        public static final int FROM_CACHE_FIELD_NUMBER = 1;
        private boolean fromCache;
        public static final int DURATION_US_FIELD_NUMBER = 2;
        private long durationUs;
        public static final int CPU_TIME_US_FIELD_NUMBER = 3;
        private long cpuTimeUs;
        private byte memoizedIsInitialized;
        private static final CompilationStats DEFAULT_INSTANCE = new CompilationStats();
        private static final Parser<CompilationStats> PARSER = new AbstractParser<CompilationStats>() {
            public CompilationStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
                return new CompilationStats(input, extensionRegistry);
            }
        };

        private CompilationStats(GeneratedMessageV3.Builder<?> builder) {
            super(builder);
            this.memoizedIsInitialized = -1;
        }

        private CompilationStats() {
            this.memoizedIsInitialized = -1;
        }

        public CompilationStats(tech.ydb.proto.YdbQueryStats.CompilationStats protoAutoGenCompilationStats) {
            this.fromCache = protoAutoGenCompilationStats.getFromCache();
            this.durationUs = protoAutoGenCompilationStats.getDurationUs();
            this.cpuTimeUs = protoAutoGenCompilationStats.getCpuTimeUs();
        }

        protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
            return new CompilationStats();
        }

        public UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
        }

        private CompilationStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
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
                                this.fromCache = input.readBool();
                                break;
                            case 16:
                                this.durationUs = input.readUInt64();
                                break;
                            case 24:
                                this.cpuTimeUs = input.readUInt64();
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
            return YdbQueryStats.internal_static_Ydb_TableStats_CompilationStats_descriptor;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.internal_static_Ydb_TableStats_CompilationStats_fieldAccessorTable.ensureFieldAccessorsInitialized(CompilationStats.class, Builder.class);
        }

        public boolean getFromCache() {
            return this.fromCache;
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long getCpuTimeUs() {
            return this.cpuTimeUs;
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
            if (this.fromCache) {
                output.writeBool(1, this.fromCache);
            }

            if (this.durationUs != 0L) {
                output.writeUInt64(2, this.durationUs);
            }

            if (this.cpuTimeUs != 0L) {
                output.writeUInt64(3, this.cpuTimeUs);
            }

            this.unknownFields.writeTo(output);
        }

        public int getSerializedSize() {
            int size = this.memoizedSize;
            if (size != -1) {
                return size;
            } else {
                size = 0;
                if (this.fromCache) {
                    size += CodedOutputStream.computeBoolSize(1, this.fromCache);
                }

                if (this.durationUs != 0L) {
                    size += CodedOutputStream.computeUInt64Size(2, this.durationUs);
                }

                if (this.cpuTimeUs != 0L) {
                    size += CodedOutputStream.computeUInt64Size(3, this.cpuTimeUs);
                }

                size += this.unknownFields.getSerializedSize();
                this.memoizedSize = size;
                return size;
            }
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (!(obj instanceof CompilationStats)) {
                return super.equals(obj);
            } else {
                CompilationStats other = (CompilationStats) obj;
                if (this.getFromCache() != other.getFromCache()) {
                    return false;
                } else if (this.getDurationUs() != other.getDurationUs()) {
                    return false;
                } else if (this.getCpuTimeUs() != other.getCpuTimeUs()) {
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
                hash = 53 * hash + Internal.hashBoolean(this.getFromCache());
                hash = 37 * hash + 2;
                hash = 53 * hash + Internal.hashLong(this.getDurationUs());
                hash = 37 * hash + 3;
                hash = 53 * hash + Internal.hashLong(this.getCpuTimeUs());
                hash = 29 * hash + this.unknownFields.hashCode();
                this.memoizedHashCode = hash;
                return hash;
            }
        }

        public static CompilationStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static CompilationStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static CompilationStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static CompilationStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static CompilationStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static CompilationStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static CompilationStats parseFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static CompilationStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static CompilationStats parseDelimitedFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
        }

        public static CompilationStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static CompilationStats parseFrom(CodedInputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static CompilationStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(CompilationStats prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
        }

        protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
            return new Builder(parent);
        }

        public static CompilationStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<CompilationStats> parser() {
            return PARSER;
        }

        public Parser<CompilationStats> getParserForType() {
            return PARSER;
        }

        public CompilationStats getDefaultInstanceForType() {
            return DEFAULT_INSTANCE;
        }

        public static final class Builder extends GeneratedMessageV3.Builder<Builder> implements CompilationStatsOrBuilder {
            private boolean fromCache;
            private long durationUs;
            private long cpuTimeUs;

            public static Descriptors.Descriptor getDescriptor() {
                return YdbQueryStats.internal_static_Ydb_TableStats_CompilationStats_descriptor;
            }

            protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return YdbQueryStats.internal_static_Ydb_TableStats_CompilationStats_fieldAccessorTable.ensureFieldAccessorsInitialized(CompilationStats.class, Builder.class);
            }

            private Builder() {
                this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
            }

            public Builder clear() {
                super.clear();
                this.fromCache = false;
                this.durationUs = 0L;
                this.cpuTimeUs = 0L;
                return this;
            }

            public Descriptors.Descriptor getDescriptorForType() {
                return YdbQueryStats.internal_static_Ydb_TableStats_CompilationStats_descriptor;
            }

            public CompilationStats getDefaultInstanceForType() {
                return YdbQueryStats.CompilationStats.getDefaultInstance();
            }

            public CompilationStats build() {
                CompilationStats result = this.buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                } else {
                    return result;
                }
            }

            public CompilationStats buildPartial() {
                CompilationStats result = new CompilationStats(this);
                result.fromCache = this.fromCache;
                result.durationUs = this.durationUs;
                result.cpuTimeUs = this.cpuTimeUs;
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
                if (other instanceof CompilationStats) {
                    return this.mergeFrom((CompilationStats) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(CompilationStats other) {
                if (other == YdbQueryStats.CompilationStats.getDefaultInstance()) {
                    return this;
                } else {
                    if (other.getFromCache()) {
                        this.setFromCache(other.getFromCache());
                    }

                    if (other.getDurationUs() != 0L) {
                        this.setDurationUs(other.getDurationUs());
                    }

                    if (other.getCpuTimeUs() != 0L) {
                        this.setCpuTimeUs(other.getCpuTimeUs());
                    }

                    this.mergeUnknownFields(other.unknownFields);
                    this.onChanged();
                    return this;
                }
            }

            public boolean isInitialized() {
                return true;
            }

            public Builder mergeFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
                CompilationStats parsedMessage = null;

                try {
                    parsedMessage = CompilationStats.PARSER.parsePartialFrom(input, extensionRegistry);
                } catch (InvalidProtocolBufferException var8) {
                    parsedMessage = (CompilationStats) var8.getUnfinishedMessage();
                    throw var8.unwrapIOException();
                } finally {
                    if (parsedMessage != null) {
                        this.mergeFrom(parsedMessage);
                    }

                }

                return this;
            }

            public boolean getFromCache() {
                return this.fromCache;
            }

            public Builder setFromCache(boolean value) {
                this.fromCache = value;
                this.onChanged();
                return this;
            }

            public Builder clearFromCache() {
                this.fromCache = false;
                this.onChanged();
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

            public Builder setUnknownFields(UnknownFieldSet unknownFields) {
                return super.setUnknownFields(unknownFields);
            }

            public Builder mergeUnknownFields(UnknownFieldSet unknownFields) {
                return super.mergeUnknownFields(unknownFields);
            }
        }
    }

    public interface CompilationStatsOrBuilder extends MessageOrBuilder {
        boolean getFromCache();

        long getDurationUs();

        long getCpuTimeUs();
    }

    public static final class QueryPhaseStats extends GeneratedMessageV3 implements QueryPhaseStatsOrBuilder {
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
            public QueryPhaseStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
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

        protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
            return new QueryPhaseStats();
        }

        public UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
        }

        private QueryPhaseStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            this();
            if (extensionRegistry == null) {
                throw new NullPointerException();
            } else {
                boolean mutable_bitField0_ = false;
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
                                if (!(mutable_bitField0_)) {
                                    this.tableAccess = new ArrayList<>();
                                    mutable_bitField0_ = true;
                                }

                                this.tableAccess.add(input.readMessage(YdbQueryStats.TableAccessStats.parser(), extensionRegistry));
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
                    if (mutable_bitField0_) {
                        this.tableAccess = Collections.unmodifiableList(this.tableAccess);
                    }

                    this.unknownFields = unknownFields.build();
                    this.makeExtensionsImmutable();
                }

            }
        }

        public QueryPhaseStats(tech.ydb.proto.YdbQueryStats.QueryPhaseStats protoAutoGenQueryPhaseStats) {
            this.durationUs = protoAutoGenQueryPhaseStats.getDurationUs();
            this.tableAccess = protoAutoGenQueryPhaseStats.getTableAccessList().stream()
                    .map(TableAccessStats::new)
                    .collect(Collectors.toList());
            this.cpuTimeUs = protoAutoGenQueryPhaseStats.getCpuTimeUs();
            this.affectedShards = protoAutoGenQueryPhaseStats.getAffectedShards();
            this.literalPhase = protoAutoGenQueryPhaseStats.getLiteralPhase();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.internal_static_Ydb_TableStats_QueryPhaseStats_descriptor;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.internal_static_Ydb_TableStats_QueryPhaseStats_fieldAccessorTable.ensureFieldAccessorsInitialized(QueryPhaseStats.class, Builder.class);
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

            for (int i = 0; i < this.tableAccess.size(); ++i) {
                output.writeMessage(2, this.tableAccess.get(i));
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
            if (size != -1) {
                return size;
            } else {
                size = 0;
                if (this.durationUs != 0L) {
                    size += CodedOutputStream.computeUInt64Size(1, this.durationUs);
                }

                for (int i = 0; i < this.tableAccess.size(); ++i) {
                    size += CodedOutputStream.computeMessageSize(2, this.tableAccess.get(i));
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
                return size;
            }
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

        public static QueryPhaseStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static QueryPhaseStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static QueryPhaseStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static QueryPhaseStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static QueryPhaseStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static QueryPhaseStats parseFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static QueryPhaseStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static QueryPhaseStats parseDelimitedFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
        }

        public static QueryPhaseStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static QueryPhaseStats parseFrom(CodedInputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static QueryPhaseStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
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
            Builder builder = new Builder(parent);
            return builder;
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
            private RepeatedFieldBuilderV3<TableAccessStats, TableAccessStats.Builder, TableAccessStatsOrBuilder> tableAccessBuilder;
            private long cpuTimeUs;
            private long affectedShards;
            private boolean literalPhase;

            public static Descriptors.Descriptor getDescriptor() {
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryPhaseStats_descriptor;
            }

            protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryPhaseStats_fieldAccessorTable.ensureFieldAccessorsInitialized(QueryPhaseStats.class, Builder.class);
            }

            private Builder() {
                this.tableAccess = Collections.emptyList();
                this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                this.tableAccess = Collections.emptyList();
                this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
                if (YdbQueryStats.QueryPhaseStats.alwaysUseFieldBuilders) {
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
                return YdbQueryStats.internal_static_Ydb_TableStats_QueryPhaseStats_descriptor;
            }

            public QueryPhaseStats getDefaultInstanceForType() {
                return YdbQueryStats.QueryPhaseStats.getDefaultInstance();
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
                int from_bitField0_ = this.bitField0;
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
                            this.tableAccessBuilder = QueryPhaseStats.alwaysUseFieldBuilders ? this.getTableAccessFieldBuilder() : null;
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
                    this.tableAccess = new ArrayList(this.tableAccess);
                    this.bitField0 |= 1;
                }

            }

            public List<TableAccessStats> getTableAccessList() {
                return this.tableAccessBuilder == null ? Collections.unmodifiableList(this.tableAccess) : this.tableAccessBuilder.getMessageList();
            }

            public int getTableAccessCount() {
                return this.tableAccessBuilder == null ? this.tableAccess.size() : this.tableAccessBuilder.getCount();
            }

            public TableAccessStats getTableAccess(int index) {
                return this.tableAccessBuilder == null ? this.tableAccess.get(index) : this.tableAccessBuilder.getMessage(index);
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
                return this.tableAccessBuilder == null ? this.tableAccess.get(index) : this.tableAccessBuilder.getMessageOrBuilder(index);
            }

            public List<? extends TableAccessStatsOrBuilder> getTableAccessOrBuilderList() {
                return this.tableAccessBuilder != null ? this.tableAccessBuilder.getMessageOrBuilderList() : Collections.unmodifiableList(this.tableAccess);
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

            private RepeatedFieldBuilderV3<TableAccessStats, TableAccessStats.Builder, TableAccessStatsOrBuilder> getTableAccessFieldBuilder() {
                if (this.tableAccessBuilder == null) {
                    this.tableAccessBuilder = new RepeatedFieldBuilderV3(this.tableAccess, (this.bitField0 & 1) != 0, this.getParentForChildren(), this.isClean());
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

    public interface QueryPhaseStatsOrBuilder extends MessageOrBuilder {
        long getDurationUs();

        List<TableAccessStats> getTableAccessList();

        TableAccessStats getTableAccess(int var1);

        int getTableAccessCount();

        List<? extends TableAccessStatsOrBuilder> getTableAccessOrBuilderList();

        TableAccessStatsOrBuilder getTableAccessOrBuilder(int var1);

        long getCpuTimeUs();

        long getAffectedShards();

        boolean getLiteralPhase();
    }

    public static final class TableAccessStats extends GeneratedMessageV3 implements TableAccessStatsOrBuilder {
        private static final long serialVersionUID = 0L;
        public static final int NAME_FIELD_NUMBER = 1;
        private volatile Object name;
        public static final int READS_FIELD_NUMBER = 3;
        private OperationStats reads;
        public static final int UPDATES_FIELD_NUMBER = 4;
        private OperationStats updates;
        public static final int DELETES_FIELD_NUMBER = 5;
        private OperationStats deletes;
        public static final int PARTITIONS_COUNT_FIELD_NUMBER = 6;
        private long partitionsCount;
        private byte memoizedIsInitialized;
        private static final TableAccessStats DEFAULT_INSTANCE = new TableAccessStats();
        private static final Parser<TableAccessStats> PARSER = new AbstractParser<TableAccessStats>() {
            public TableAccessStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
                return new TableAccessStats(input, extensionRegistry);
            }
        };

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

        protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
            return new TableAccessStats();
        }

        public UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
        }

        private TableAccessStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
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
            return YdbQueryStats.internal_static_Ydb_TableStats_TableAccessStats_descriptor;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.internal_static_Ydb_TableStats_TableAccessStats_fieldAccessorTable.ensureFieldAccessorsInitialized(TableAccessStats.class, Builder.class);
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
            return this.reads == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.reads;
        }

        public OperationStatsOrBuilder getReadsOrBuilder() {
            return this.getReads();
        }

        public boolean hasUpdates() {
            return this.updates != null;
        }

        public OperationStats getUpdates() {
            return this.updates == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.updates;
        }

        public OperationStatsOrBuilder getUpdatesOrBuilder() {
            return this.getUpdates();
        }

        public boolean hasDeletes() {
            return this.deletes != null;
        }

        public OperationStats getDeletes() {
            return this.deletes == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.deletes;
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

        public static TableAccessStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static TableAccessStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static TableAccessStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static TableAccessStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static TableAccessStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static TableAccessStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static TableAccessStats parseFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static TableAccessStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static TableAccessStats parseDelimitedFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
        }

        public static TableAccessStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static TableAccessStats parseFrom(CodedInputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static TableAccessStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(TableAccessStats prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
        }

        protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
            return new Builder(parent);
        }

        public static TableAccessStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<TableAccessStats> parser() {
            return PARSER;
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
            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> readsBuilder_;
            private OperationStats updates;
            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> updatesBuilder_;
            private OperationStats deletes;
            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> deletesBuilder_;
            private long partitionsCount;

            public static Descriptors.Descriptor getDescriptor() {
                return YdbQueryStats.internal_static_Ydb_TableStats_TableAccessStats_descriptor;
            }

            protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return YdbQueryStats.internal_static_Ydb_TableStats_TableAccessStats_fieldAccessorTable.ensureFieldAccessorsInitialized(TableAccessStats.class, Builder.class);
            }

            private Builder() {
                this.name = "";
                this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                this.name = "";
                this.maybeForceBuilderInitialization();
            }

            private void maybeForceBuilderInitialization() {
                if (YdbQueryStats.TableAccessStats.alwaysUseFieldBuilders) {
                }

            }

            public Builder clear() {
                super.clear();
                this.name = "";
                if (this.readsBuilder_ == null) {
                    this.reads = null;
                } else {
                    this.reads = null;
                    this.readsBuilder_ = null;
                }

                if (this.updatesBuilder_ == null) {
                    this.updates = null;
                } else {
                    this.updates = null;
                    this.updatesBuilder_ = null;
                }

                if (this.deletesBuilder_ == null) {
                    this.deletes = null;
                } else {
                    this.deletes = null;
                    this.deletesBuilder_ = null;
                }

                this.partitionsCount = 0L;
                return this;
            }

            public Descriptors.Descriptor getDescriptorForType() {
                return YdbQueryStats.internal_static_Ydb_TableStats_TableAccessStats_descriptor;
            }

            public TableAccessStats getDefaultInstanceForType() {
                return YdbQueryStats.TableAccessStats.getDefaultInstance();
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
                if (this.readsBuilder_ == null) {
                    result.reads = this.reads;
                } else {
                    result.reads = this.readsBuilder_.build();
                }

                if (this.updatesBuilder_ == null) {
                    result.updates = this.updates;
                } else {
                    result.updates = this.updatesBuilder_.build();
                }

                if (this.deletesBuilder_ == null) {
                    result.deletes = this.deletes;
                } else {
                    result.deletes = this.deletesBuilder_.build();
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

            public Builder setName(String value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    this.name = value;
                    this.onChanged();
                    return this;
                }
            }

            public Builder clearName() {
                this.name = YdbQueryStats.TableAccessStats.getDefaultInstance().getName();
                this.onChanged();
                return this;
            }

            public Builder setNameBytes(ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                } else {
                    YdbQueryStats.TableAccessStats.checkByteStringIsUtf8(value);
                    this.name = value;
                    this.onChanged();
                    return this;
                }
            }

            public boolean hasReads() {
                return this.readsBuilder_ != null || this.reads != null;
            }

            public OperationStats getReads() {
                if (this.readsBuilder_ == null) {
                    return this.reads == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.reads;
                } else {
                    return this.readsBuilder_.getMessage();
                }
            }

            public Builder setReads(OperationStats value) {
                if (this.readsBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    this.reads = value;
                    this.onChanged();
                } else {
                    this.readsBuilder_.setMessage(value);
                }

                return this;
            }

            public Builder setReads(OperationStats.Builder builderForValue) {
                if (this.readsBuilder_ == null) {
                    this.reads = builderForValue.build();
                    this.onChanged();
                } else {
                    this.readsBuilder_.setMessage(builderForValue.build());
                }

                return this;
            }

            public Builder mergeReads(OperationStats value) {
                if (this.readsBuilder_ == null) {
                    if (this.reads != null) {
                        this.reads = YdbQueryStats.OperationStats.newBuilder(this.reads).mergeFrom(value).buildPartial();
                    } else {
                        this.reads = value;
                    }

                    this.onChanged();
                } else {
                    this.readsBuilder_.mergeFrom(value);
                }

                return this;
            }

            public Builder clearReads() {
                if (this.readsBuilder_ == null) {
                    this.reads = null;
                    this.onChanged();
                } else {
                    this.reads = null;
                    this.readsBuilder_ = null;
                }

                return this;
            }

            public OperationStats.Builder getReadsBuilder() {
                this.onChanged();
                return this.getReadsFieldBuilder().getBuilder();
            }

            public OperationStatsOrBuilder getReadsOrBuilder() {
                if (this.readsBuilder_ != null) {
                    return this.readsBuilder_.getMessageOrBuilder();
                } else {
                    return this.reads == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.reads;
                }
            }

            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> getReadsFieldBuilder() {
                if (this.readsBuilder_ == null) {
                    this.readsBuilder_ = new SingleFieldBuilderV3<>(this.getReads(), this.getParentForChildren(), this.isClean());
                    this.reads = null;
                }

                return this.readsBuilder_;
            }

            public boolean hasUpdates() {
                return this.updatesBuilder_ != null || this.updates != null;
            }

            public OperationStats getUpdates() {
                if (this.updatesBuilder_ == null) {
                    return this.updates == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.updates;
                } else {
                    return this.updatesBuilder_.getMessage();
                }
            }

            public Builder setUpdates(OperationStats value) {
                if (this.updatesBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    this.updates = value;
                    this.onChanged();
                } else {
                    this.updatesBuilder_.setMessage(value);
                }

                return this;
            }

            public Builder setUpdates(OperationStats.Builder builderForValue) {
                if (this.updatesBuilder_ == null) {
                    this.updates = builderForValue.build();
                    this.onChanged();
                } else {
                    this.updatesBuilder_.setMessage(builderForValue.build());
                }

                return this;
            }

            public Builder mergeUpdates(OperationStats value) {
                if (this.updatesBuilder_ == null) {
                    if (this.updates != null) {
                        this.updates = YdbQueryStats.OperationStats.newBuilder(this.updates).mergeFrom(value).buildPartial();
                    } else {
                        this.updates = value;
                    }

                    this.onChanged();
                } else {
                    this.updatesBuilder_.mergeFrom(value);
                }

                return this;
            }

            public Builder clearUpdates() {
                if (this.updatesBuilder_ == null) {
                    this.updates = null;
                    this.onChanged();
                } else {
                    this.updates = null;
                    this.updatesBuilder_ = null;
                }

                return this;
            }

            public OperationStats.Builder getUpdatesBuilder() {
                this.onChanged();
                return this.getUpdatesFieldBuilder().getBuilder();
            }

            public OperationStatsOrBuilder getUpdatesOrBuilder() {
                if (this.updatesBuilder_ != null) {
                    return this.updatesBuilder_.getMessageOrBuilder();
                } else {
                    return this.updates == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.updates;
                }
            }

            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> getUpdatesFieldBuilder() {
                if (this.updatesBuilder_ == null) {
                    this.updatesBuilder_ = new SingleFieldBuilderV3<>(this.getUpdates(), this.getParentForChildren(), this.isClean());
                    this.updates = null;
                }

                return this.updatesBuilder_;
            }

            public boolean hasDeletes() {
                return this.deletesBuilder_ != null || this.deletes != null;
            }

            public OperationStats getDeletes() {
                if (this.deletesBuilder_ == null) {
                    return this.deletes == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.deletes;
                } else {
                    return this.deletesBuilder_.getMessage();
                }
            }

            public Builder setDeletes(OperationStats value) {
                if (this.deletesBuilder_ == null) {
                    if (value == null) {
                        throw new NullPointerException();
                    }

                    this.deletes = value;
                    this.onChanged();
                } else {
                    this.deletesBuilder_.setMessage(value);
                }

                return this;
            }

            public Builder setDeletes(OperationStats.Builder builderForValue) {
                if (this.deletesBuilder_ == null) {
                    this.deletes = builderForValue.build();
                    this.onChanged();
                } else {
                    this.deletesBuilder_.setMessage(builderForValue.build());
                }

                return this;
            }

            public Builder mergeDeletes(OperationStats value) {
                if (this.deletesBuilder_ == null) {
                    if (this.deletes != null) {
                        this.deletes = YdbQueryStats.OperationStats.newBuilder(this.deletes).mergeFrom(value).buildPartial();
                    } else {
                        this.deletes = value;
                    }

                    this.onChanged();
                } else {
                    this.deletesBuilder_.mergeFrom(value);
                }

                return this;
            }

            public Builder clearDeletes() {
                if (this.deletesBuilder_ == null) {
                    this.deletes = null;
                    this.onChanged();
                } else {
                    this.deletes = null;
                    this.deletesBuilder_ = null;
                }

                return this;
            }

            public OperationStats.Builder getDeletesBuilder() {
                this.onChanged();
                return this.getDeletesFieldBuilder().getBuilder();
            }

            public OperationStatsOrBuilder getDeletesOrBuilder() {
                if (this.deletesBuilder_ != null) {
                    return this.deletesBuilder_.getMessageOrBuilder();
                } else {
                    return this.deletes == null ? YdbQueryStats.OperationStats.getDefaultInstance() : this.deletes;
                }
            }

            private SingleFieldBuilderV3<OperationStats, OperationStats.Builder, OperationStatsOrBuilder> getDeletesFieldBuilder() {
                if (this.deletesBuilder_ == null) {
                    this.deletesBuilder_ = new SingleFieldBuilderV3<>(this.getDeletes(), this.getParentForChildren(), this.isClean());
                    this.deletes = null;
                }

                return this.deletesBuilder_;
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

    public interface TableAccessStatsOrBuilder extends MessageOrBuilder {
        String getName();

        ByteString getNameBytes();

        boolean hasReads();

        OperationStats getReads();

        OperationStatsOrBuilder getReadsOrBuilder();

        boolean hasUpdates();

        OperationStats getUpdates();

        OperationStatsOrBuilder getUpdatesOrBuilder();

        boolean hasDeletes();

        OperationStats getDeletes();

        OperationStatsOrBuilder getDeletesOrBuilder();

        long getPartitionsCount();
    }


    public static final class OperationStats extends GeneratedMessageV3 implements OperationStatsOrBuilder {
        private static final long serialVersionUID = 0L;
        public static final int ROWS_FIELD_NUMBER = 1;
        private long rows;
        public static final int BYTES_FIELD_NUMBER = 2;
        private long bytes;
        private byte memoizedIsInitialized;
        private static final OperationStats DEFAULT_INSTANCE = new OperationStats();

        private static final Parser<OperationStats> PARSER = new AbstractParser<OperationStats>() {
            public OperationStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
                return new OperationStats(input, extensionRegistry);
            }
        };

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

        protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
            return new OperationStats();
        }

        public UnknownFieldSet getUnknownFields() {
            return this.unknownFields;
        }

        private OperationStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
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
            return YdbQueryStats.internal_static_Ydb_TableStats_OperationStats_descriptor;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.internal_static_Ydb_TableStats_OperationStats_fieldAccessorTable.ensureFieldAccessorsInitialized(OperationStats.class, Builder.class);
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

        // TODO
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

        // TODO
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

        public static OperationStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static OperationStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static OperationStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static OperationStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static OperationStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static OperationStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry) throws InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static OperationStats parseFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static OperationStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static OperationStats parseDelimitedFrom(InputStream input) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
        }

        public static OperationStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
        }

        public static OperationStats parseFrom(CodedInputStream input) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static OperationStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry) throws IOException {
            return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(OperationStats prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
        }

        protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
            return new Builder(parent);
        }

        public static OperationStats getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        public static Parser<OperationStats> parser() {
            return PARSER;
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

            public static Descriptors.Descriptor getDescriptor() {
                return YdbQueryStats.internal_static_Ydb_TableStats_OperationStats_descriptor;
            }

            protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return YdbQueryStats.internal_static_Ydb_TableStats_OperationStats_fieldAccessorTable.ensureFieldAccessorsInitialized(OperationStats.class, Builder.class);
            }

            private Builder() {
                this.maybeForceBuilderInitialization();
            }

            private Builder(GeneratedMessageV3.BuilderParent parent) {
                super(parent);
                this.maybeForceBuilderInitialization();
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
                return YdbQueryStats.internal_static_Ydb_TableStats_OperationStats_descriptor;
            }

            public OperationStats getDefaultInstanceForType() {
                return YdbQueryStats.OperationStats.getDefaultInstance();
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

    public interface OperationStatsOrBuilder extends MessageOrBuilder {
        long getRows();

        long getBytes();
    }
}
