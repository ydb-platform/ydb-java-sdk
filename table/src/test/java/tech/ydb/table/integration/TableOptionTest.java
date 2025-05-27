package tech.ydb.table.integration;

import org.junit.Assert;
import org.junit.ClassRule;

import org.junit.Test;

import tech.ydb.core.Result;
import tech.ydb.core.StatusCode;
import tech.ydb.table.Session;
import tech.ydb.table.SessionRetryContext;
import tech.ydb.table.description.TableOptionDescription;
import tech.ydb.table.impl.SimpleTableClient;
import tech.ydb.table.rpc.grpc.GrpcTableRpc;
import tech.ydb.table.settings.DescribeTableOptionsSettings;
import tech.ydb.test.junit4.GrpcTransportRule;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Test connected to get described table options.
 *
 * @author Evgeny Kuvardin
 */
public class TableOptionTest {

    @ClassRule
    public final static GrpcTransportRule ydbTransport = new GrpcTransportRule();

    private final SimpleTableClient tableClient = SimpleTableClient.newClient(
            GrpcTableRpc.useTransport(ydbTransport)
    ).build();

    private final SessionRetryContext ctx = SessionRetryContext.create(tableClient).build();

    /**
     * Test checks that call to describe table options works with the default settings
     */
    @Test
    public void tableOptionsTestShouldGetGlobalOptionsWithDefaultSettings() {
        Result<TableOptionDescription> describeResult = ctx.supplyResult(Session::describeTableOptions).join();

        Assert.assertTrue("Describe table options" + describeResult.getStatus(), describeResult.isSuccess());

        TableOptionDescription tableOptionDescription = describeResult.getValue();

        checkTableOptions(tableOptionDescription);
    }

    /**
     * Test checks that call to describe table options works with all set options
     */
    @Test
    public void tableOptionsTestShouldGetGlobalOptions() {
        Result<TableOptionDescription> describeResult = ctx.supplyResult(session -> {
            DescribeTableOptionsSettings settings = new DescribeTableOptionsSettings();
            settings.setTraceId("traceId");
            settings.setReportCostInfo(true);
            settings.setTimeout(10, TimeUnit.SECONDS);
            settings.setCancelAfter(Duration.ofSeconds(8));
            settings.setOperationTimeout(Duration.ofSeconds(8));
            return session.describeTableOptions(settings);
        }).join();
        Assert.assertTrue("Describe table options" + describeResult.getStatus(), describeResult.isSuccess());

        TableOptionDescription tableOptionDescription = describeResult.getValue();
        checkTableOptions(tableOptionDescription);
    }

    /**
     * Test checks that call to describe table options fails when we set client timeout as low as possible
     * and request can't be completed in this time.
     * We should catch StatusCode == CLIENT_DEADLINE_EXPIRED
     */
    @Test
    public void tableOptionsTestShouldFailedInOrderToTimeout() {
        Result<TableOptionDescription> describeResult = ctx.supplyResult(session -> {
            DescribeTableOptionsSettings settings = new DescribeTableOptionsSettings();
            settings.setTraceId("traceId");
            settings.setReportCostInfo(true);
            settings.setTimeout(1, TimeUnit.NANOSECONDS);
            return session.describeTableOptions(settings);
        }).join();
        Assert.assertFalse("Describe table options" + describeResult.getStatus(), describeResult.isSuccess());
        Assert.assertEquals(StatusCode.CLIENT_DEADLINE_EXPIRED, describeResult.getStatus().getCode());
    }

    private void checkTableOptions(TableOptionDescription tableOptionDescription) {
        checkStoragePresets(tableOptionDescription);

        checkPolicyPresets(tableOptionDescription);

        checkCompactionPolicyPresets(tableOptionDescription);

        checkExecutionPolicyPresets(tableOptionDescription);

        checkPartitioningPolicyPresets(tableOptionDescription);

        checkReplicationPolicyPresets(tableOptionDescription);

        checkTableProfileDescriptions(tableOptionDescription);
    }


    private void checkStoragePresets(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getStoragePolicyPresets());
        List<TableOptionDescription.StoragePolicyDescription> storagePolicyDescription = tableOptionDescription.getStoragePolicyPresets();
        Assert.assertEquals(2, storagePolicyDescription.size());


        for (TableOptionDescription.StoragePolicyDescription v1 : storagePolicyDescription) {
            if (v1.getName().equals("default")) {
                checkDefaultStoragePolicy(v1);
            } else {
                checkLogLz4StoragePolicy(v1);
            }
        }
    }

    private void checkTableProfileDescriptions(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getTableProfileDescriptions());
        List<TableOptionDescription.TableProfileDescription> tableProfileDescriptions = tableOptionDescription.getTableProfileDescriptions();

        Assert.assertFalse(tableProfileDescriptions.isEmpty());

        Optional<TableOptionDescription.TableProfileDescription> t = tableProfileDescriptions.stream().filter(p -> p.getName().equals("default")).findFirst();
        Assert.assertTrue(t.isPresent());
        TableOptionDescription.TableProfileDescription v = t.get();
        Assert.assertEquals("default", v.getName());
        Assert.assertNotNull(v.getLabels());
        Assert.assertEquals("default", v.getDefaultCachingPolicy());
        Assert.assertEquals("default", v.getDefaultCompactionPolicy());
        Assert.assertEquals("default", v.getDefaultExecutionPolicy());
        Assert.assertEquals("default", v.getDefaultPartitioningPolicy());
        Assert.assertEquals("default", v.getDefaultReplicationPolicy());
        Assert.assertEquals("default", v.getDefaultStoragePolicy());

        Assert.assertNotNull(v.getAllowedCachingPolicy());
        Assert.assertTrue(v.getAllowedCachingPolicy().stream().anyMatch(p -> p.equals("default")));

        Assert.assertNotNull(v.getAllowedCompactionPolicy());
        Assert.assertTrue(v.getAllowedCompactionPolicy().stream().anyMatch(p -> p.equals("default")));

        Assert.assertNotNull(v.getAllowedExecutionPolicy());
        Assert.assertTrue(v.getAllowedExecutionPolicy().stream().anyMatch(p -> p.equals("default")));

        Assert.assertNotNull(v.getAllowedPartitioningPolicy());
        Assert.assertTrue(v.getAllowedPartitioningPolicy().stream().anyMatch(p -> p.equals("default")));

        Assert.assertNotNull(v.getAllowedReplicationPolicy());
        Assert.assertTrue(v.getAllowedReplicationPolicy().stream().anyMatch(p -> p.equals("default")));

        Assert.assertNotNull(v.getAllowedStoragePolicy());
        Assert.assertTrue(v.getAllowedStoragePolicy().stream().anyMatch(p -> p.equals("default")));
    }

    private void checkReplicationPolicyPresets(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getReplicationPolicyPresets());
        List<TableOptionDescription.ReplicationPolicyDescription> replicationPolicyPresets = tableOptionDescription.getReplicationPolicyPresets();

        Assert.assertEquals(1, replicationPolicyPresets.size());
        Assert.assertEquals("default", replicationPolicyPresets.get(0).getName());
        Assert.assertNotNull(replicationPolicyPresets.get(0).getLabels());
        Assert.assertFalse(replicationPolicyPresets.get(0).getLabels().isEmpty());
    }

    private void checkPartitioningPolicyPresets(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getPartitioningPolicyPresets());
        List<TableOptionDescription.PartitioningPolicyDescription> partitioningPolicyPresets = tableOptionDescription.getPartitioningPolicyPresets();

        Assert.assertEquals(1, partitioningPolicyPresets.size());
        Assert.assertEquals("default", partitioningPolicyPresets.get(0).getName());
        Assert.assertNotNull(partitioningPolicyPresets.get(0).getLabels());
        Assert.assertFalse(partitioningPolicyPresets.get(0).getLabels().isEmpty());
    }

    private void checkExecutionPolicyPresets(TableOptionDescription tableOptionDescription) {

        Assert.assertNotNull(tableOptionDescription.getExecutionPolicyPresets());
        List<TableOptionDescription.ExecutionPolicyDescription> executionPolicyPresets = tableOptionDescription.getExecutionPolicyPresets();

        Assert.assertEquals(1, executionPolicyPresets.size());
        Assert.assertEquals("default", executionPolicyPresets.get(0).getName());
        Assert.assertNotNull(executionPolicyPresets.get(0).getLabels());
        Assert.assertFalse(executionPolicyPresets.get(0).getLabels().isEmpty());
    }

    private void checkCompactionPolicyPresets(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getCompactionPolicyPresets());
        List<TableOptionDescription.CompactionPolicyDescription> compactionPolicyPresets = tableOptionDescription.getCompactionPolicyPresets();

        Assert.assertEquals(1, compactionPolicyPresets.size());
        Assert.assertEquals("default", compactionPolicyPresets.get(0).getName());
        Assert.assertNotNull(compactionPolicyPresets.get(0).getLabels());
        Assert.assertTrue(compactionPolicyPresets.get(0).getLabels().isEmpty());
    }

    private void checkPolicyPresets(TableOptionDescription tableOptionDescription) {
        Assert.assertNotNull(tableOptionDescription.getCachingPolicyPresets());
        List<TableOptionDescription.CachingPolicyDescription> cachingPolicyPresets = tableOptionDescription.getCachingPolicyPresets();

        Assert.assertEquals(1, cachingPolicyPresets.size());
        Assert.assertEquals("default", cachingPolicyPresets.get(0).getName());
        Assert.assertNotNull(cachingPolicyPresets.get(0).getLabels());
        Assert.assertTrue(cachingPolicyPresets.get(0).getLabels().isEmpty());
    }

    private void checkLogLz4StoragePolicy(TableOptionDescription.StoragePolicyDescription storagePolicyDescription) {
        Map<String, String> map2 = storagePolicyDescription.getLabels();
        Assert.assertEquals("log_lz4", storagePolicyDescription.getName());
        Assert.assertEquals("hdd", map2.get("data"));
        Assert.assertEquals("false", map2.get("in_memory"));
        Assert.assertEquals("hdd", map2.get("log"));
        Assert.assertEquals("hdd", map2.get("syslog"));
        Assert.assertEquals("lz4", map2.get("codec"));
    }

    private void checkDefaultStoragePolicy(TableOptionDescription.StoragePolicyDescription storagePolicyDescription) {
        Assert.assertEquals("default", storagePolicyDescription.getName());
        Map<String, String> map = storagePolicyDescription.getLabels();
        Assert.assertEquals("hdd", map.get("data"));
        Assert.assertEquals("false", map.get("in_memory"));
        Assert.assertEquals("hdd", map.get("log"));
        Assert.assertEquals("hdd", map.get("syslog"));
        Assert.assertEquals("none", map.get("codec"));
    }

}
