package tech.ydb.coordination.recipes.election;

public interface LeaderElectionListener {
    void takeLeadership() throws Exception;
}
