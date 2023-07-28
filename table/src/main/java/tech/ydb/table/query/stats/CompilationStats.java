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

public final class CompilationStats extends GeneratedMessageV3 implements CompilationStatsOrBuilder {
    public static final int FROM_CACHE_FIELD_NUMBER = 1;
    public static final int DURATION_US_FIELD_NUMBER = 2;
    public static final int CPU_TIME_US_FIELD_NUMBER = 3;
    private static final long serialVersionUID = 0L;
    private static final CompilationStats DEFAULT_INSTANCE = new CompilationStats();
    private static final Parser<CompilationStats> PARSER = new AbstractParser<CompilationStats>() {
        public CompilationStats parsePartialFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
                throws InvalidProtocolBufferException {
            return new CompilationStats(input, extensionRegistry);
        }
    };
    private boolean fromCache;
    private long durationUs;
    private long cpuTimeUs;
    private byte memoizedIsInitialized;

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

    private CompilationStats(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
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
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR;
    }

    public static CompilationStats parseFrom(ByteBuffer data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CompilationStats parseFrom(ByteBuffer data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CompilationStats parseFrom(ByteString data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CompilationStats parseFrom(ByteString data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CompilationStats parseFrom(byte[] data) throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static CompilationStats parseFrom(byte[] data, ExtensionRegistryLite extensionRegistry)
            throws InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static CompilationStats parseFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static CompilationStats parseFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static CompilationStats parseDelimitedFrom(InputStream input) throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
    }

    public static CompilationStats parseDelimitedFrom(InputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static CompilationStats parseFrom(CodedInputStream input) throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input);
    }

    public static CompilationStats parseFrom(CodedInputStream input, ExtensionRegistryLite extensionRegistry)
            throws IOException {
        return GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(CompilationStats prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static CompilationStats getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Parser<CompilationStats> parser() {
        return PARSER;
    }

    protected Object newInstance(GeneratedMessageV3.UnusedPrivateParameter unused) {
        return new CompilationStats();
    }

    public UnknownFieldSet getUnknownFields() {
        return this.unknownFields;
    }

    protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
        return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_FIELD_ACCESSOR_TABLE
                .ensureFieldAccessorsInitialized(CompilationStats.class, Builder.class);
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

    public Builder newBuilderForType() {
        return newBuilder();
    }

    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE ? new Builder() : (new Builder()).mergeFrom(this);
    }

    protected Builder newBuilderForType(GeneratedMessageV3.BuilderParent parent) {
        return new Builder(parent);
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

        private Builder() {
            this.maybeForceBuilderInitialization();
        }

        private Builder(GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            this.maybeForceBuilderInitialization();
        }

        public static Descriptors.Descriptor getDescriptor() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR;
        }

        protected GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_FIELD_ACCESSOR_TABLE
                    .ensureFieldAccessorsInitialized(CompilationStats.class, Builder.class);
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
            return YdbQueryStats.INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR;
        }

        public CompilationStats getDefaultInstanceForType() {
            return CompilationStats.getDefaultInstance();
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
            if (other == CompilationStats.getDefaultInstance()) {
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
