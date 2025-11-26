package tech.ydb.query.result;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import tech.ydb.proto.YdbQueryStats;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class QueryStatsTest {

    private static YdbQueryStats.CompilationStats comp(boolean fromCache, long cpu, long duration) {
        return YdbQueryStats.CompilationStats.newBuilder()
                .setFromCache(fromCache)
                .setCpuTimeUs(cpu)
                .setDurationUs(duration)
                .build();
    }

    private static YdbQueryStats.OperationStats op(long rows, long bytes) {
        return YdbQueryStats.OperationStats.newBuilder()
                .setRows(rows)
                .setBytes(bytes)
                .build();
    }

    private static YdbQueryStats.TableAccessStats table(String name, long parts, YdbQueryStats.OperationStats reads,
            YdbQueryStats.OperationStats updates, YdbQueryStats.OperationStats deletes) {
        return YdbQueryStats.TableAccessStats.newBuilder()
                .setName(name)
                .setPartitionsCount(parts)
                .setReads(reads)
                .setUpdates(updates)
                .setDeletes(deletes)
                .build();
    }

    private static YdbQueryStats.QueryPhaseStats phase(boolean literal, long shards, long cpu, long duration,
            YdbQueryStats.TableAccessStats... tables) {
        return YdbQueryStats.QueryPhaseStats.newBuilder()
                .setLiteralPhase(literal)
                .setAffectedShards(shards)
                .setCpuTimeUs(cpu)
                .setDurationUs(duration)
                .addAllTableAccess(Arrays.asList(tables))
                .build();
    }

    private static YdbQueryStats.QueryStats stats(String ast, String plan, long totalCpu, long duration,
            long processCpu, YdbQueryStats.CompilationStats cs, YdbQueryStats.QueryPhaseStats... qps) {
        return YdbQueryStats.QueryStats.newBuilder().setQueryAst(ast).setQueryPlan(plan).setTotalCpuTimeUs(totalCpu)
                .setTotalDurationUs(duration).setProcessCpuTimeUs(processCpu).setCompilation(cs)
                .addAllQueryPhases(Arrays.asList(qps))
                .build();
    }

    @Test
    public void queryStatsGettersTest() {
        YdbQueryStats.QueryPhaseStats ph = phase(true, 1, 2, 3, table("t1", 10, op(1, 2), op(3, 4), op(5, 6)));
        QueryStats qs = new QueryStats(stats("a", "b", 1, 2, 3, comp(true, 10, 20), ph));

        Assert.assertEquals("a", qs.getQueryAst());
        Assert.assertEquals("b", qs.getQueryPlan());
        Assert.assertEquals(1l, qs.getTotalCpuTimeUs());
        Assert.assertEquals(2l, qs.getTotalDurationUs());
        Assert.assertEquals(3l, qs.getProcessCpuTimeUs());

        Assert.assertTrue(qs.getCompilationStats().isFromCache());
        Assert.assertEquals(10l, qs.getCompilationStats().getCpuTimeUs());
        Assert.assertEquals(20l, qs.getCompilationStats().getDurationUs());

        Assert.assertEquals(1, qs.getPhases().size());
        Assert.assertTrue(qs.getPhases().get(0).isLiteralPhase());
        Assert.assertEquals(1l, qs.getPhases().get(0).getAffectedShards());
        Assert.assertEquals(2l, qs.getPhases().get(0).getCpuTimeUs());
        Assert.assertEquals(3l, qs.getPhases().get(0).getDurationUs());

        Assert.assertEquals(1, qs.getPhases().get(0).getTableAccesses().size());
        Assert.assertEquals("t1", qs.getPhases().get(0).getTableAccesses().get(0).getTableName());
        Assert.assertEquals(10l, qs.getPhases().get(0).getTableAccesses().get(0).getPartitionsCount());
        Assert.assertEquals(1l, qs.getPhases().get(0).getTableAccesses().get(0).getReads().getRows());
        Assert.assertEquals(2l, qs.getPhases().get(0).getTableAccesses().get(0).getReads().getBytes());
        Assert.assertEquals(3l, qs.getPhases().get(0).getTableAccesses().get(0).getUpdates().getRows());
        Assert.assertEquals(4l, qs.getPhases().get(0).getTableAccesses().get(0).getUpdates().getBytes());
        Assert.assertEquals(5l, qs.getPhases().get(0).getTableAccesses().get(0).getDeletes().getRows());
        Assert.assertEquals(6l, qs.getPhases().get(0).getTableAccesses().get(0).getDeletes().getBytes());
    }

    @Test
    @Deprecated
    public void queryStatsDeprectedGettersTest() {
        YdbQueryStats.QueryPhaseStats ph = phase(true, 1, 2, 3, table("t1", 10, op(1, 2), op(3, 4), op(5, 6)));
        QueryStats qs = new QueryStats(stats("a", "b", 1, 2, 3, comp(true, 10, 20), ph));
        Assert.assertEquals(qs.getCompilationStats(), qs.getComplilationStats());
    }

    @Test
    public void queryStatsEqualsTest() {
        YdbQueryStats.TableAccessStats t1 = table("t1", 5, op(1, 1), op(0, 0), op(0, 0));
        YdbQueryStats.CompilationStats c1 = comp(true, 10, 20);
        YdbQueryStats.CompilationStats c2 = comp(false, 10, 20);
        YdbQueryStats.QueryPhaseStats ph1 = phase(true, 1, 2, 3, t1);
        YdbQueryStats.QueryPhaseStats ph2 = phase(false, 1, 2, 3, t1);

        QueryStats s1 = new QueryStats(stats("a", "b", 1, 2, 3, c1, ph1));

        Assert.assertEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 2, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 3, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("b", "b", 1, 2, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "c", 1, 2, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 2, 2, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 3, 3, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 2, 4, c1, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 2, 3, c2, ph1)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 2, 3, c1, ph2)).hashCode());
        Assert.assertNotEquals(s1.hashCode(), new QueryStats(stats("a", "b", 1, 2, 3, c1, ph1, ph2)));

        Assert.assertEquals(s1, s1);
        Assert.assertEquals(s1, new QueryStats(stats("a", "b", 1, 2, 3, c1, ph1)));

        Assert.assertNotEquals(s1, null);
        Assert.assertNotEquals(s1, t1);

        Assert.assertNotEquals(s1, new QueryStats(stats("b", "b", 1, 2, 3, c1, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "c", 1, 2, 3, c1, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 2, 2, 3, c1, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 1, 3, 3, c1, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 1, 2, 4, c1, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 1, 2, 3, c2, ph1)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 1, 2, 3, c1, ph2)));
        Assert.assertNotEquals(s1, new QueryStats(stats("a", "b", 1, 2, 3, c1, ph1, ph2)));
    }

    @Test
    public void compilationEqualsTest() {
        QueryStats.Compilation c1 = new QueryStats.Compilation(comp(true, 10, 20));

        Assert.assertEquals(c1.hashCode(), new QueryStats.Compilation(comp(true, 10, 20)).hashCode());
        Assert.assertNotEquals(c1.hashCode(), new QueryStats.Compilation(comp(false, 10, 20)).hashCode());
        Assert.assertNotEquals(c1.hashCode(), new QueryStats.Compilation(comp(true, 11, 20)).hashCode());
        Assert.assertNotEquals(c1.hashCode(), new QueryStats.Compilation(comp(true, 10, 22)).hashCode());

        Assert.assertEquals(c1, c1);
        Assert.assertEquals(c1, new QueryStats.Compilation(comp(true, 10, 20)));

        Assert.assertNotEquals(c1, null);
        Assert.assertNotEquals(c1, comp(true, 10, 20));

        Assert.assertNotEquals(c1, new QueryStats.Compilation(comp(false, 10, 20)));
        Assert.assertNotEquals(c1, new QueryStats.Compilation(comp(true, 11, 20)));
        Assert.assertNotEquals(c1, new QueryStats.Compilation(comp(true, 10, 21)));
    }

    @Test
    public void operationEqualsTest() {
        QueryStats.Operation o1 = new QueryStats.Operation(op(10, 20));

        Assert.assertEquals(o1.hashCode(), new QueryStats.Operation(op(10, 20)).hashCode());
        Assert.assertNotEquals(o1.hashCode(), new QueryStats.Operation(op(11, 20)).hashCode());
        Assert.assertNotEquals(o1.hashCode(), new QueryStats.Operation(op(10, 22)).hashCode());

        Assert.assertEquals(o1, o1);
        Assert.assertEquals(o1, new QueryStats.Operation(op(10, 20)));

        Assert.assertNotEquals(o1, null);
        Assert.assertNotEquals(o1, op(10, 20));

        Assert.assertNotEquals(o1, new QueryStats.Operation(op(11, 20)));
        Assert.assertNotEquals(o1, new QueryStats.Operation(op(10, 21)));

        Assert.assertEquals("OperationStats{rows=10, bytes=20}", o1.toString());
    }

    @Test
    public void tableAccessEqualsTest() {
        YdbQueryStats.OperationStats o1 = op(1, 1);
        YdbQueryStats.OperationStats o2 = op(1, 2);
        YdbQueryStats.OperationStats o3 = op(1, 3);
        YdbQueryStats.OperationStats o4 = op(1, 4);

        QueryStats.TableAccess t1 = new QueryStats.TableAccess(table("t1", 5, o1, o2, o3));

        Assert.assertEquals(t1.hashCode(), new QueryStats.TableAccess(table("t1", 5, o1, o2, o3)).hashCode());
        Assert.assertNotEquals(t1.hashCode(), new QueryStats.TableAccess(table("t2", 5, o1, o2, o3)).hashCode());
        Assert.assertNotEquals(t1.hashCode(), new QueryStats.TableAccess(table("t1", 6, o1, o2, o3)).hashCode());
        Assert.assertNotEquals(t1.hashCode(), new QueryStats.TableAccess(table("t1", 5, o4, o2, o3)).hashCode());
        Assert.assertNotEquals(t1.hashCode(), new QueryStats.TableAccess(table("t1", 5, o1, o4, o3)).hashCode());
        Assert.assertNotEquals(t1.hashCode(), new QueryStats.TableAccess(table("t1", 5, o1, o2, o4)).hashCode());

        Assert.assertEquals(t1, t1);
        Assert.assertEquals(t1, new QueryStats.TableAccess(table("t1", 5, o1, o2, o3)));

        Assert.assertNotEquals(t1, null);
        Assert.assertNotEquals(t1, table("t1", 5, o1, o2, o3));

        Assert.assertNotEquals(t1, new QueryStats.TableAccess(table("t2", 5, o1, o2, o3)));
        Assert.assertNotEquals(t1, new QueryStats.TableAccess(table("t1", 6, o1, o2, o3)));
        Assert.assertNotEquals(t1, new QueryStats.TableAccess(table("t1", 5, o4, o2, o3)));
        Assert.assertNotEquals(t1, new QueryStats.TableAccess(table("t1", 5, o1, o4, o3)));
        Assert.assertNotEquals(t1, new QueryStats.TableAccess(table("t1", 5, o1, o2, o4)));
    }

    @Test
    public void queryPhaseEqualsTest() {
        YdbQueryStats.TableAccessStats t1 = table("t1", 5, op(1, 1), op(1, 2), op(1, 3));
        YdbQueryStats.TableAccessStats t2 = table("t2", 5, op(1, 1), op(1, 2), op(1, 3));

        QueryStats.QueryPhase p1 = new QueryStats.QueryPhase(phase(true, 1, 2, 3, t1));

        Assert.assertEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 1, 2, 3, t1)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(false, 1, 2, 3, t1)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 2, 2, 3, t1)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 1, 3, 3, t1)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 1, 2, 4, t1)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 1, 2, 3, t2)).hashCode());
        Assert.assertNotEquals(p1.hashCode(), new QueryStats.QueryPhase(phase(true, 1, 2, 3, t1, t2)).hashCode());

        Assert.assertEquals(p1, p1);
        Assert.assertEquals(p1, new QueryStats.QueryPhase(phase(true, 1, 2, 3, t1)));

        Assert.assertNotEquals(p1, null);
        Assert.assertNotEquals(p1, phase(true, 1, 2, 3, t1));

        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(false, 1, 2, 3, t1)));
        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(true, 2, 2, 3, t1)));
        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(true, 1, 3, 3, t1)));
        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(true, 1, 2, 4, t1)));
        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(true, 1, 2, 3, t2)));
        Assert.assertNotEquals(p1, new QueryStats.QueryPhase(phase(true, 1, 2, 3, t1, t2)));
    }
}
