package tech.ydb.topic.read.impl;

import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DeferredCommitterImpl implements DeferredCommitter {
    private final Map<Long, Function<OffsetsRange, CompletableFuture<Void>>> commitFunctions;
    private final Set<Message> messages = new HashSet<>();

    DeferredCommitterImpl(Map<Long, Function<OffsetsRange, CompletableFuture<Void>>> commitFunctions) {
        this.commitFunctions = commitFunctions;
    }

    @Override
    public synchronized void add(Message message) {
        messages.add(message);
    }

    @Override
    public synchronized CompletableFuture<Void> commit(long partitionId) {
        Map<Long, List<Message>> messagesToCommit = messages.stream()
                .filter(message -> message.getPartitionSession().getPartitionId() == partitionId)
                .sorted(Comparator.comparingLong(Message::getOffset))
                .collect(Collectors.groupingBy(message -> message.getPartitionSession().getId()));

        return CompletableFuture.allOf(
                messagesToCommit.entrySet().stream()
                        .flatMap(entry -> commitOnePartitionSession(entry.getKey(), entry.getValue()).stream())
                        .toArray(CompletableFuture<?>[]::new)
        );
    }

    private List<CompletableFuture<Void>> commitOnePartitionSession(long partitionSessionId, List<Message> messagesToCommit) {
        List<CompletableFuture<Void>> commitFutures = new ArrayList<>();
        int startIntervalIndex = 0;
        while (startIntervalIndex < messagesToCommit.size()) {
            int endIntervalIndex = getIntervalEnd(messagesToCommit, startIntervalIndex);
            commitFutures.add(
                    commitFunctions.get(partitionSessionId).apply(new OffsetsRange(
                            messagesToCommit.get(startIntervalIndex).getCommitOffsetFrom(),
                            messagesToCommit.get(endIntervalIndex).getOffset() + 1
                    ))
            );
            startIntervalIndex = endIntervalIndex + 1;
        }
        messagesToCommit.forEach(messages::remove);
        return commitFutures;
    }

    private static int getIntervalEnd(List<Message> messagesToCommit, int startIntervalIndex) {
        int endIntervalIndex = startIntervalIndex;
        while (endIntervalIndex < messagesToCommit.size() - 1) {
            if (messagesToCommit.get(endIntervalIndex + 1).getCommitOffsetFrom() == messagesToCommit.get(endIntervalIndex).getCommitOffsetFrom() + 1) {
                endIntervalIndex++;
            } else {
                break;
            }
        }
        return endIntervalIndex;
    }
}
