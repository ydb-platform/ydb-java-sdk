package tech.ydb.table.query.stats;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.GeneratedMessageV3;

public final class YdbQueryStats {
    public static final Descriptors.Descriptor INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR;
    public static final GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_FIELD_ACCESSOR_TABLE;
    public static final Descriptors.Descriptor INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR;
    public static final GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_FIELD_ACCESSOR_TABLE;
    public static final Descriptors.Descriptor INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR;
    public static final GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_FIELD_ACCESSOR_TABLE;
    public static final Descriptors.Descriptor INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR;
    public static final GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_FIELD_ACCESSOR_TABLE;
    public static final Descriptors.Descriptor INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR;
    public static final GeneratedMessageV3.FieldAccessorTable
            INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_FIELD_ACCESSOR_TABLE;
    public static final Descriptors.FileDescriptor DESCRIPTOR;

    static {
        String[] descriptorData = new String[]{"\n\u001cprotos/ydb_query_stats.proto\u0012\u000eYdb.TableStats\"" +
                "-\n\u000eOperationStats\u0012\f\n\u0004rows\u0018\u0001 \u0001(\u0004\u0012\r\n\u0005bytes" +
                "\u0018\u0002 \u0001(\u0004\"Ñ\u0001\n\u0010TableAccessStats\u0012\f\n\u0004name\u0018\u0001 " +
                "\u0001(\t\u0012-\n\u0005reads\u0018\u0003 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats" +
                "\u0012/\n\u0007updates\u0018\u0004 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats\u0012/" +
                "\n\u0007deletes\u0018\u0005 \u0001(\u000b2\u001e.Ydb.TableStats.OperationStats\u0012\u0018\n\u0010" +
                "partitions_count\u0018\u0006 \u0001(\u0004J\u0004\b\u0002\u0010\u0003\"£\u0001\n\u000f" +
                "QueryPhaseStats\u0012\u0013\n\u000bduration_us\u0018\u0001 \u0001(\u0004\u00126\n\ftable_access" +
                "\u0018\u0002 \u0003(\u000b2 .Ydb.TableStats.TableAccessStats\u0012\u0013\n\u000bcpu_time_us\u0018" +
                "\u0003 \u0001(\u0004\u0012\u0017\n\u000faffected_shards\u0018\u0004 \u0001(\u0004\u0012\u0015" +
                "\n\rliteral_phase\u0018\u0005 \u0001(\b\"P\n\u0010CompilationStats\u0012\u0012\n\nfrom_cache" +
                "\u0018\u0001 \u0001(\b\u0012\u0013\n\u000bduration_us\u0018\u0002 \u0001(\u0004\u0012\u0013" +
                "\n\u000bcpu_time_us\u0018\u0003 \u0001(\u0004\"ô\u0001\n\nQueryStats\u00125\n\fquery_phases" +
                "\u0018\u0001 \u0003(\u000b2\u001f.Ydb.TableStats.QueryPhaseStats\u00125\n\u000bcompilation" +
                "\u0018\u0002 \u0001(\u000b2 .Ydb.TableStats.CompilationStats\u0012\u001b\n\u0013process_cpu_time_us" +
                "\u0018\u0003 \u0001(\u0004\u0012\u0012\n\nquery_plan\u0018\u0004 \u0001(\t\u0012\u0011\n\tquery_ast" +
                "\u0018\u0005 \u0001(\t\u0012\u0019\n\u0011total_duration_us\u0018\u0006 \u0001(\u0004\u0012\u0019\n" +
                "\u0011total_cpu_time_us\u0018\u0007 \u0001(\u0004BR\n\u000etech.ydb.protoZ=github.com/ydb-platform" +
                "/ydb-go-genproto/protos/Ydb_TableStatsø\u0001\u0001b\u0006proto3"};
        DESCRIPTOR = FileDescriptor.internalBuildGeneratedFileFrom(descriptorData, new Descriptors.FileDescriptor[0]);
        INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR = getDescriptor().getMessageTypes().get(0);
        INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_FIELD_ACCESSOR_TABLE =
                new GeneratedMessageV3.FieldAccessorTable(INTERNAL_STATIC_YDB_TABLE_STATS_OPERATION_STATS_DESCRIPTOR,
                        new String[]{"Rows", "Bytes"});
        INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR = getDescriptor().getMessageTypes().get(1);
        INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_FIELD_ACCESSOR_TABLE =
                new GeneratedMessageV3.FieldAccessorTable(INTERNAL_STATIC_YDB_TABLE_STATS_TABLE_ACCESS_STATS_DESCRIPTOR,
                        new String[]{"Name", "Reads", "Updates", "Deletes", "PartitionsCount"});
        INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR = getDescriptor().getMessageTypes().get(2);
        INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_FIELD_ACCESSOR_TABLE =
                new GeneratedMessageV3.FieldAccessorTable(INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_PHASE_STATS_DESCRIPTOR,
                        new String[]{"DurationUs", "TableAccess", "CpuTimeUs", "AffectedShards", "LiteralPhase"});
        INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR = getDescriptor().getMessageTypes().get(3);
        INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_FIELD_ACCESSOR_TABLE =
                new GeneratedMessageV3.FieldAccessorTable(INTERNAL_STATIC_YDB_TABLE_STATS_COMPILATION_STATS_DESCRIPTOR,
                        new String[]{"FromCache", "DurationUs", "CpuTimeUs"});
        INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR = getDescriptor().getMessageTypes().get(4);
        INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_FIELD_ACCESSOR_TABLE =
                new GeneratedMessageV3.FieldAccessorTable(INTERNAL_STATIC_YDB_TABLE_STATS_QUERY_STATS_DESCRIPTOR,
                        new String[]{"QueryPhases", "Compilation", "ProcessCpuTimeUs", "QueryPlan", "QueryAst",
                                "TotalDurationUs", "TotalCpuTimeUs"});
    }

    private YdbQueryStats() {
    }

    public static void registerAllExtensions(ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(ExtensionRegistry registry) {
        registerAllExtensions((ExtensionRegistryLite) registry);
    }

    public static Descriptors.FileDescriptor getDescriptor() {
        return DESCRIPTOR;
    }
}
