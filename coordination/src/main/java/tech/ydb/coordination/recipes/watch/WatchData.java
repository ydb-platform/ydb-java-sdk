package tech.ydb.coordination.recipes.watch;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WatchData {
    final long count;
    final byte[] data;
    final List<Participant> waiters;
    final List<Participant> owners;
    final List<Participant> participants;

    WatchData(long count, byte[] data, List<Participant> waiters, List<Participant> owners) {
        this.count = count;
        this.data = data;
        this.waiters = waiters;
        this.owners = owners;
        this.participants = Stream.concat(owners.stream(), waiters.stream()).collect(Collectors.toList());
    }

    public long getCount() {
        return count;
    }

    public byte[] getData() {
        return data;
    }

    public List<Participant> getWaiters() {
        return waiters;
    }

    public List<Participant> getOwners() {
        return owners;
    }

    public List<Participant> getParticipants() {
        return participants;
    }
}
