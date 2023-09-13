package tech.ydb.topic.read.impl;

import tech.ydb.topic.read.DeferredCommitter;
import tech.ydb.topic.read.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class DeferredCommitterImpl implements DeferredCommitter {
    private final BiFunction<Long, OffsetsRange, CompletableFuture<Void>> commitFunction;
    private final Map<Long, SortedSet<Message>> messages = new HashMap<>();

    DeferredCommitterImpl(BiFunction<Long, OffsetsRange, CompletableFuture<Void>> commitFunction) {
        this.commitFunction = commitFunction;
    }

    @Override
    public synchronized void add(Message message) {
        messages.computeIfAbsent(
                message.getPartitionSession().getId(),
                ignore -> new TreeSet<>(Comparator.comparingLong(Message::getOffset))
        ).add(message);
    }

    @Override
    public synchronized CompletableFuture<Void> commit() {
        return CompletableFuture.allOf(
                messages.entrySet().stream()
                        .flatMap(entry -> commitOnePartitionSession(
                                entry.getKey(),
                                new ArrayList<>(entry.getValue())
                        ).stream())
                        .toArray(CompletableFuture<?>[]::new)
        );
    }

    private List<CompletableFuture<Void>> commitOnePartitionSession(
            long partitionSessionId,
            List<Message> messagesToCommit
    ) {
        List<CompletableFuture<Void>> commitFutures = new ArrayList<>();
        int startIntervalIndex = 0;
        while (startIntervalIndex < messagesToCommit.size()) {
            int endIntervalIndex = getIntervalEnd(messagesToCommit, startIntervalIndex);
            commitFutures.add(
                    commitFunction.apply(partitionSessionId, new OffsetsRange(
                            messagesToCommit.get(startIntervalIndex).getCommitOffsetFrom(),
                            messagesToCommit.get(endIntervalIndex).getOffset() + 1
                    ))
            );
            startIntervalIndex = endIntervalIndex + 1;
        }
        messages.remove(partitionSessionId);
        return commitFutures;
    }

    private static int getIntervalEnd(List<Message> messagesToCommit, int startIntervalIndex) {
        int endIntervalIndex = startIntervalIndex;
        while (endIntervalIndex < messagesToCommit.size() - 1) {
            if (messagesToCommit.get(endIntervalIndex + 1).getCommitOffsetFrom() ==
                    messagesToCommit.get(endIntervalIndex).getCommitOffsetFrom() + 1) {
                endIntervalIndex++;
            } else {
                break;
            }
        }
        return endIntervalIndex;
    }
}
