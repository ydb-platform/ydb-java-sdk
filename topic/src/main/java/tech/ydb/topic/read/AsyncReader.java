package tech.ydb.topic.read;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.grpc.ExperimentalApi;

import tech.ydb.common.transaction.BaseTransaction;
import tech.ydb.core.Status;
import tech.ydb.topic.settings.UpdateOffsetsInTransactionSettings;

/**
 * @author Nikolay Perfilov
 */
@ExperimentalApi("Topic service interfaces are experimental and may change without notice")
public interface AsyncReader {

    /**
     * Initialize reading in the background. Non-blocking
     */
    CompletableFuture<Void> init();

    /**
     * Stops internal threads and makes cleanup in background. Non-blocking
     */
    CompletableFuture<Void> shutdown();

    /**
     * Add offsets to transaction. Offsets could be from several topics.
     * These offsets are "committed" only after said transaction is successfully committed.
     * It is a separate request sent outside the reading stream.
     *
     * @param transaction a {@link BaseTransaction} that offsets should be added to
     * @param offsets Offsets that should be added to transaction.
     *                Map key: topic Path
     *                Map value: List of Partition ranges for every partition in this topic to add
     * @param settings Operation settings.
     * @return {@link CompletableFuture} to operation status
     */
    CompletableFuture<Status> updateOffsetsInTransaction(BaseTransaction transaction,
                                                         Map<String, List<PartitionOffsets>> offsets,
                                                         UpdateOffsetsInTransactionSettings settings);

    /**
     * Add offsets of a single partition session to transaction.Offsets could be from several topics.
     * These offsets are "committed" only after said transaction is successfully committed.
     * It is a separate request sent outside the reading stream.
     *
     * @param transaction a {@link BaseTransaction} that offsets should be added to
     * @param offsets Offsets that should be added to transaction.
     * @param settings Operation settings.
     * @return {@link CompletableFuture} to operation status
     */
    default CompletableFuture<Status> updateOffsetsInTransaction(BaseTransaction transaction, PartitionOffsets offsets,
                                                                 UpdateOffsetsInTransactionSettings settings) {
        return updateOffsetsInTransaction(transaction,
                Collections.singletonMap(offsets.getPartitionSession().getPath(), Collections.singletonList(offsets)),
                settings);
    }
}
