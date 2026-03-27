package tech.ydb.topic.read;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.topic.description.OffsetsRange;

/**
 *
 * @author Aleksandr Gorshenin
 */
public interface MessageCommitter {

    CompletableFuture<Void> commit(OffsetsRange range);

    void commitRanges(List<OffsetsRange> ranges);
}
