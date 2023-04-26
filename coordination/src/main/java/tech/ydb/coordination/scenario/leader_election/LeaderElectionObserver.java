package tech.ydb.coordination.scenario.leader_election;

/**
 * @author Kirill Kurdyukov
 */
public interface LeaderElectionObserver {

    void onNext(String ticket);
}
