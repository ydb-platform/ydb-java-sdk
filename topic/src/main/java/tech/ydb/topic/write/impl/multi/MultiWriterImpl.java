package tech.ydb.topic.write.impl.multi;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.FutureTools;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.MetadataItem;
import tech.ydb.topic.description.PartitionInfo;
import tech.ydb.topic.settings.MultiWriterSettings;
import tech.ydb.topic.settings.MultiWriterSettings.RoutingMode;
import tech.ydb.topic.settings.SendSettings;
import tech.ydb.topic.settings.TopicRetryConfig;
import tech.ydb.topic.settings.WriterSettings;
import tech.ydb.topic.write.AsyncMultiWriter;
import tech.ydb.topic.write.AsyncWriter;
import tech.ydb.topic.write.InitResult;
import tech.ydb.topic.write.Message;
import tech.ydb.topic.write.MultiMessage;
import tech.ydb.topic.write.MultiWriteResult;
import tech.ydb.topic.write.MultiWriteResult.ConfirmationSource;
import tech.ydb.topic.write.QueueOverflowException;
import tech.ydb.topic.write.WriteAck;

/**
 * Serial logical coordinator over public AsyncWriter instances. No lower transport or queue state is accessed.
 * Like the Go sweep, the shared budget retains an acknowledged suffix behind an unfinished earlier message.
 */
public class MultiWriterImpl implements AsyncMultiWriter {
    private static final Logger logger = LoggerFactory.getLogger(MultiWriterImpl.class);
    private static final long REFRESH_NANOS = TimeUnit.MILLISECONDS.toNanos(100);

    private final MultiWriterSettings settings;
    private final Codec codec;
    private final Function<WriterSettings, AsyncWriter> factory;
    private final Supplier<CompletableFuture<List<PartitionInfo>>> describer;
    private final MultiWriterRuntime runtime;
    // Only admission order and the shared budget are guarded here. No lower calls or user callbacks hold this lock.
    private final Object admission = new Object();
    private final CompletableFuture<InitResult> initialized = new CompletableFuture<>();
    private final CompletableFuture<Void> closed = new CompletableFuture<>();
    private final Map<Long, Pending> pending = new ConcurrentSkipListMap<>();
    private final Map<Long, WriterSlot> sendSlotsByPartition = new HashMap<>();
    private final Set<WriterSlot> occupiedWriterSlots = new HashSet<>();
    private final Map<Long, Long> histories = new HashMap<>();
    private final Map<Long, Recovery> recoveries = new HashMap<>();
    private final List<Barrier> barriers = new ArrayList<>();
    private final Deque<ProbeBatch> probeBatches = new ArrayDeque<>();
    private CompletableFuture<Void> preparation;
    private final AtomicBoolean tickQueued = new AtomicBoolean();

    private boolean initRequested;
    private boolean admissionOpen;
    private boolean shutdownRequested;
    private long lastAdmissionId;
    private long budgetBytes;
    private int budgetCount;
    private int barrierCount;
    private volatile boolean disposed;
    private volatile Topology topology;
    private volatile Throwable failure;
    private long lastAssignedSeqNo;
    private long settledThroughAdmissionId;
    private long initStarted;
    private long closeStarted;
    private long nextBootstrap;
    private boolean bootstrapping;
    private boolean bootstrapped;
    private boolean closing;
    private int initializingCount;
    private ScheduledFuture<?> ticker;

    public MultiWriterImpl(MultiWriterSettings settings, Codec codec,
            Function<WriterSettings, AsyncWriter> factory,
            Supplier<CompletableFuture<List<PartitionInfo>>> describer) {
        this(settings, codec, factory, describer, new MultiWriterRuntime(settings));
    }

    MultiWriterImpl(MultiWriterSettings settings, Codec codec,
            Function<WriterSettings, AsyncWriter> factory,
            Supplier<CompletableFuture<List<PartitionInfo>>> describer, MultiWriterRuntime runtime) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.factory = Objects.requireNonNull(factory, "factory");
        this.describer = Objects.requireNonNull(describer, "describer");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        if (codec.getId() != settings.getCodec()) {
            throw new IllegalArgumentException("Codec does not match multiwriter settings");
        }
    }

    @Override
    public CompletableFuture<InitResult> init() {
        synchronized (admission) {
            if (!initRequested && !shutdownRequested && failure == null) {
                initRequested = true;
                enqueueOnCoordinator(() -> {
                    initStarted = now();
                    startTicker();
                    bootstrap();
                });
            } else if (!initRequested) {
                return exceptional(new IllegalStateException("Multiwriter is closed", failure));
            }
        }
        return view(initialized);
    }

    @Override
    public CompletableFuture<MultiWriteResult> send(MultiMessage input) throws QueueOverflowException {
        Objects.requireNonNull(input, "message");
        byte[] data = input.getData();
        List<MetadataItem> metadata = input.getMetadataItems();
        long encoded = codec.getMaxEncodedSize(data.length);
        if (encoded < data.length || encoded > settings.getWriterBufferBytes()
                || data.length > settings.getMaxMessageBytes()) {
            throw new IllegalArgumentException("Message or conservative codec bound exceeds the configured limit");
        }
        long bytes = Math.addExact(data.length, encoded);
        for (MetadataItem item : metadata) {
            bytes = Math.addExact(bytes, 32L + item.getKey().getBytes(StandardCharsets.UTF_8).length
                    + item.getValue().length);
        }
        String key = input.getKey().isEmpty() ? settings.getProducerPrefix() : input.getKey();
        bytes = Math.addExact(bytes, 256L + 2L * key.length());
        CompletableFuture<MultiWriteResult> result = new CompletableFuture<>();
        synchronized (admission) {
            checkAdmission();
            long partition = topology.route(key, input.getPartitionId());
            if (budgetCount >= settings.getBufferMessages() || bytes > settings.getBufferBytes() - budgetBytes) {
                throw new QueueOverflowException("Multiwriter shared message/byte budget is full");
            }
            if (lastAdmissionId == Long.MAX_VALUE) {
                throw new IllegalStateException("Admission number exhausted");
            }
            Pending message = new Pending(lastAdmissionId + 1, bytes, encoded, data, metadata, key, input,
                    partition, result);
            // Submission and all barriers use the same lock, defining their admission linearization.
            pending.put(message.admissionId, message);
            try {
                enqueueOnCoordinator(() -> admit(message));
            } catch (RejectedExecutionException error) {
                pending.remove(message.admissionId);
                throw error;
            }
            lastAdmissionId++;
            budgetBytes += bytes;
            budgetCount++;
        }
        return result;
    }

    protected void checkAdmission() {
        if (!admissionOpen || shutdownRequested || failure != null) {
            throw new IllegalStateException("Multiwriter requires successful init and open admission", failure);
        }
    }

    @Override
    public CompletableFuture<Void> flush() {
        synchronized (admission) {
            if (failure != null) {
                return exceptional(failure);
            }
            if (!admissionOpen && !shutdownRequested && preparation == null) {
                return exceptional(new IllegalStateException("Multiwriter is not initialized"));
            }
            if (disposed) {
                return view(closed);
            }
            if (barrierCount >= settings.getBufferMessages()) {
                return exceptional(new IllegalStateException("Too many outstanding flush barriers"));
            }
            CompletableFuture<Void> result = new CompletableFuture<>();
            long admissionId = lastAdmissionId;
            enqueueOnCoordinator(() -> addBarrier(admissionId, result));
            barrierCount++;
            return result;
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        synchronized (admission) {
            if (!shutdownRequested) {
                shutdownRequested = true;
                admissionOpen = false;
                closeStarted = now();
                if (!disposed) {
                    enqueueOnCoordinator(() -> {
                        startTicker();
                        if (!bootstrapped) {
                            fail(new IllegalStateException("Multiwriter closed before initialization completed"));
                        }
                        checkDrain();
                    });
                }
            }
        }
        return view(closed);
    }

    private void enqueueOnCoordinator(Runnable task) {
        if (disposed) {
            throw new RejectedExecutionException("Multiwriter coordinator is closed");
        }
        runtime.coordinator.execute(() -> {
            // Already accepted commands must run even when an earlier event completed shutdown.
            // In particular, their flush promises must not disappear behind the disposal flag.
            try {
                task.run();
            } catch (Throwable error) {
                fail(error);
            }
        });
    }

    private boolean tryEnqueueOnCoordinator(Runnable task) {
        try {
            enqueueOnCoordinator(task);
            return true;
        } catch (RejectedExecutionException ignored) {
            return false; // Only owned shutdown can reject internal events; late resources clean up at their source.
        }
    }

    private long now() {
        return runtime.nanoTime.getAsLong();
    }

    private void startTicker() {
        if (ticker != null) {
            return;
        }
        try {
            ticker = runtime.scheduler.scheduleAtFixedRate(() -> {
                if (tickQueued.compareAndSet(false, true)) {
                    if (!tryEnqueueOnCoordinator(() -> {
                        tickQueued.set(false);
                        tick();
                    })) {
                        tickQueued.set(false);
                    }
                }
            }, 10, 10, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException error) {
            fail(error);
        }
    }

    private void bootstrap() {
        if (failure != null || closing || shutdownRequested || bootstrapping) {
            return;
        }
        bootstrapping = true;
        describe(settings.getRoutingMode())
                .thenCompose(this::initializeFromProducerHistory)
                .exceptionally(error -> {
                    bootstrapError(FutureTools.unwrapCompletionException(error));
                    return null;
                });
    }

    // Both operations complete on the coordinator. The second describe starts after the probes have closed.
    private CompletableFuture<Void> initializeFromProducerHistory(Topology initial) {
        return readPartitionHistories(initial.getPartitions().values()).thenCompose(seqNoByPartition -> {
            CompletableFuture<Topology> recheck = describe(initial.getRoutingMode());
            return recheck.thenAccept(current -> finishBootstrap(initial, current, seqNoByPartition));
        });
    }

    private void finishBootstrap(Topology initial, Topology current, Map<Long, Long> seqNoByPartition) {
        bootstrapping = false;
        if (!initial.sameLayout(current)) {
            nextBootstrap = now() + REFRESH_NANOS;
            return;
        }
        histories.putAll(seqNoByPartition);
        long maximum = 0;
        for (long value : seqNoByPartition.values()) {
            maximum = Math.max(maximum, value);
        }
        if (maximum == Long.MAX_VALUE) {
            fail(new IllegalStateException("Producer sequence number exhausted"));
            return;
        }
        lastAssignedSeqNo = maximum;
        topology = current;
        bootstrapped = true;
        synchronized (admission) {
            admissionOpen = !shutdownRequested && failure == null;
        }
        complete(initialized, new InitResult(lastAssignedSeqNo), null);
    }

    private void bootstrapError(Throwable error) {
        bootstrapping = false;
        if (permanent(error)) {
            fail(error);
        } else {
            nextBootstrap = now() + REFRESH_NANOS;
        }
    }

    private CompletableFuture<Topology> describe(RoutingMode mode) {
        CompletableFuture<Topology> result = new CompletableFuture<>();
        dispatchTopicCall(() -> {
            try {
                describer.get().whenComplete((partitions, error) ->
                        tryEnqueueOnCoordinator(() -> completeDescribe(result, mode, partitions, error)));
            } catch (Throwable error) {
                tryEnqueueOnCoordinator(() -> completeDescribe(result, mode, null, error));
            }
        });
        return result;
    }

    private void completeDescribe(CompletableFuture<Topology> result, RoutingMode mode,
            List<PartitionInfo> partitions, Throwable error) {
        if (failure != null || closing) {
            return;
        }
        if (error != null) {
            result.completeExceptionally(error);
            return;
        }
        try {
            result.complete(new Topology(partitions, mode));
        } catch (IllegalArgumentException invalid) {
            result.completeExceptionally(invalid);
        }
    }

    private CompletableFuture<Map<Long, Long>> readPartitionHistories(Collection<PartitionInfo> partitions) {
        ProbeBatch batch = new ProbeBatch(partitions);
        probeBatches.addLast(batch);
        if (probeBatches.size() == 1) {
            processProbeQueue();
        }
        return batch.result;
    }

    // Coordinator only. Bootstrap and split recovery share the same writer and initialization limits.
    private void processProbeQueue() {
        while (!probeBatches.isEmpty() && failure == null && !closing) {
            ProbeBatch batch = probeBatches.getFirst();
            if (!startPendingHistoryProbes(batch) || batch.inFlightProbes > 0) {
                return;
            }
            probeBatches.removeFirst();
            if (batch.error == null) {
                batch.result.complete(batch.seqNoByPartition);
            } else {
                batch.result.completeExceptionally(batch.error);
            }
        }
    }

    private boolean startPendingHistoryProbes(ProbeBatch batch) {
        while (batch.error == null && !batch.remaining.isEmpty()) {
            if (occupiedWriterSlots.size() >= settings.getMaxWriters()) {
                evictOneIdle();
                return false;
            }
            if (initializingCount >= settings.getMaxConcurrentInits()) {
                return false;
            }
            PartitionInfo partition = batch.remaining.getFirst();
            WriterSlot existing = sendSlotsByPartition.get(partition.getPartitionId());
            if (existing != null && !existing.slotReleased) {
                // Wait for the previous session of this producer to close before reading its history.
                requestWriterShutdown(existing);
                return false;
            }
            batch.remaining.removeFirst();
            WriterSlot slot = new WriterSlot(partition, batch);
            occupiedWriterSlots.add(slot);
            batch.inFlightProbes++;
            startWriter(slot);
        }
        return true;
    }

    private void admit(Pending message) {
        message.registered = true;
        if (message.done) {
            return;
        }
        if (failure != null || closing) {
            settle(message, null, failure != null ? failure : new IllegalStateException("Writer closed"));
            return;
        }
        message.partition = topology.route(message.key, message.explicitPartition);
        Long explicit = message.explicitSequence;
        if (lastAssignedSeqNo == Long.MAX_VALUE || explicit != null && explicit <= lastAssignedSeqNo) {
            fail(new IllegalArgumentException("Sequence numbers must strictly increase and fit a positive long"));
            return;
        }
        message.sequence = explicit == null ? lastAssignedSeqNo + 1 : explicit;
        lastAssignedSeqNo = message.sequence;
        byte[] bound = topology.partitionKey(message.key);
        if (bound != null) {
            message.metadata.add(new MetadataItem("__partition_key", bound));
        }
        dispatchReadyWrites();
    }

    private void dispatchReadyWrites() {
        if (failure != null || closing || topology == null) {
            return;
        }
        Set<Long> blocked = new HashSet<>();
        for (Pending message : pending.values()) {
            if (!message.registered || message.done || message.attempt != null) {
                continue;
            }
            // A refresh for another branch can discover this split while the message waits for a pool slot.
            // It has never been submitted to this producer, so route it before creating an inactive writer.
            // Older attempted messages remain protected by the recovery lineage lock below.
            if (!topology.get(message.partition).isActive()) {
                message.partition = topology.route(message.key, message.explicitPartition);
            }
            if (blocked.contains(message.partition) || isBlockedBySplitRecovery(message.partition)) {
                continue;
            }
            if (!trySubmit(message)) {
                blocked.add(message.partition);
            }
        }
    }

    private boolean trySubmit(Pending message) {
        WriterSlot slot = sendSlotsByPartition.get(message.partition);
        if (slot == null) {
            if (occupiedWriterSlots.size() >= settings.getMaxWriters()) {
                evictOneIdle();
                return false;
            }
            if (initializingCount >= settings.getMaxConcurrentInits()) {
                return false;
            }
            slot = new WriterSlot(message.partition);
            sendSlotsByPartition.put(message.partition, slot);
            occupiedWriterSlots.add(slot);
            startWriter(slot);
        }
        if (!slot.ready || slot.retiring || slot.sendCallInFlight) {
            return false;
        }
        if (slot.count >= settings.getWriterBufferMessages()
                || message.encodedBytes > settings.getWriterBufferBytes() - slot.bytes) {
            return false;
        }
        submit(slot, message);
        return true;
    }

    private boolean isBlockedBySplitRecovery(long partition) {
        for (long ancestor : recoveries.keySet()) {
            if (topology.isDescendant(partition, ancestor)) {
                return true;
            }
        }
        return false;
    }

    private void submit(WriterSlot slot, Pending message) {
        Attempt attempt = new Attempt(slot, message);
        message.attempt = attempt;
        slot.sendCallInFlight = true;
        slot.count++;
        slot.bytes += message.encodedBytes;
        slot.lastActivity = now();
        Message lower = Message.newBuilder().setData(message.data).setSeqNo(message.sequence)
                .setMetadataItems(message.metadata).setCreateTimestamp(message.timestamp).build();
        dispatchTopicCall(() -> {
            try {
                if (slot.retiring) {
                    tryEnqueueOnCoordinator(() -> handleSendResult(attempt, null,
                            new IllegalStateException("Partition stopped before submit")));
                } else {
                    SendSettings sendSettings = beforeSubmit();
                    slot.writer.send(lower, sendSettings).whenComplete((ack, error) ->
                            tryEnqueueOnCoordinator(() -> handleSendResult(attempt, ack, error)));
                }
            } catch (Throwable error) {
                tryEnqueueOnCoordinator(() -> handleSendResult(attempt, null, error));
            } finally {
                if (!tryEnqueueOnCoordinator(() -> {
                    slot.sendCallInFlight = false;
                    if (slot.retiring) {
                        requestWriterShutdown(slot);
                    }
                    dispatchReadyWrites();
                })) {
                    // The close deadline can expire while send holds a lower transport lock.
                    // Once it returns, the topic call task must finish the deferred close even without a coordinator.
                    lateClose(slot.writer);
                }
            }
        });
    }

    private void lateClose(AsyncWriter writer) {
        try {
            writer.shutdown().whenComplete((ignored, error) -> {
                if (error != null) {
                    logger.warn("Late multiwriter resource cleanup failed after the user deadline", error);
                }
            });
        } catch (Throwable error) {
            logger.warn("Cannot request late multiwriter resource cleanup after the user deadline", error);
        }
    }

    protected SendSettings beforeSubmit() {
        return null;
    }

    protected void validateAcknowledgement(WriteAck ack) {
        if (ack.getState() == WriteAck.State.WRITTEN_IN_TX) {
            throw new IllegalStateException("Transactional ACK for an ordinary message");
        }
    }

    private void handleSendResult(Attempt attempt, WriteAck ack, Throwable error) {
        WriterSlot slot = attempt.slot;
        Pending message = attempt.message;
        if (attempt.completed) {
            return;
        }
        attempt.completed = true;
        slot.count--;
        slot.bytes -= message.encodedBytes;
        slot.lastActivity = now();
        if (message.done || message.attempt != attempt) {
            return;
        }
        if (error == null && ack != null) {
            if (ack.getSeqNo() < message.sequence) {
                fail(new IllegalStateException("ACK does not cover its logical message"));
                return;
            }
            validateAcknowledgement(ack);
            histories.merge(slot.partition, message.sequence, Math::max);
            Long offset = ack.getSeqNo() == message.sequence && ack.getDetails() != null
                    && ack.getState() == WriteAck.State.WRITTEN ? ack.getDetails().getOffset() : null;
            settle(message, new MultiWriteResult(message.sequence, ack.getState(), ConfirmationSource.ACK,
                    slot.partition, offset), null);
            dispatchReadyWrites();
            return;
        }
        if (failure != null || closing) {
            settle(message, null, failure != null ? failure : new IllegalStateException("Writer closing"));
        } else if (error instanceof QueueOverflowException) {
            fail(new IllegalStateException(
                    "Unexpected lower admission failure; SeqNo cannot be retried in place", error));
        } else {
            recover(slot, error != null ? error : new IllegalStateException("Missing ACK"));
        }
    }

    private void startWriter(WriterSlot slot) {
        initializingCount++;
        slot.opening = true;
        slot.started = now();
        WriterSettings.Builder builder = WriterSettings.newBuilder().setTopicPath(settings.getTopicPath())
                .setProducerId(settings.getProducerPrefix() + '-' + slot.partition)
                .setCodec(settings.getCodec()).setMaxSendBufferMemorySize(settings.getWriterBufferBytes())
                .setMaxSendBufferMessagesCount(settings.getWriterBufferMessages())
                .setDirectWrite(settings.isDirectWrite())
                .setRetryConfig(status -> retry(slot, status))
                .setErrorsHandler((status, error) -> {
                    if (status.getCode() == StatusCode.OVERLOADED
                            || TopicRetryConfig.STANDARD.getStatusRetryPolicy(status) == null) {
                        tryEnqueueOnCoordinator(() -> handleWriterFailure(slot,
                                new UnexpectedResultException("Partition writer stopped", status)));
                    }
                });
        if (slot.pinned) {
            builder.setPartitionId(slot.partition);
        }
        dispatchTopicCall(() -> {
            AsyncWriter writer = null;
            try {
                writer = factory.apply(builder.build());
                AsyncWriter created = writer;
                // Publish the writer before init so the coordinator can close it even if init hangs.
                if (!tryEnqueueOnCoordinator(() -> {
                    slot.writer = created;
                    if (slot.retiring) {
                        requestWriterShutdown(slot);
                    }
                })) {
                    lateClose(created);
                    return;
                }
                writer.init().whenComplete((result, error) -> {
                    if (!tryEnqueueOnCoordinator(() -> applyInitResult(slot, result, error))) {
                        lateClose(created);
                    }
                });
            } catch (Throwable error) {
                AsyncWriter created = writer;
                if (!tryEnqueueOnCoordinator(() -> applyInitResult(slot, null, error)) && created != null) {
                    lateClose(created);
                }
            }
        });
    }

    private RetryPolicy retry(WriterSlot slot, Status status) {
        RetryPolicy policy = status.getCode() == StatusCode.OVERLOADED
                ? null : TopicRetryConfig.STANDARD.getStatusRetryPolicy(status);
        if (policy == null && !slot.retiring) {
            tryEnqueueOnCoordinator(() -> handleWriterFailure(slot,
                    new UnexpectedResultException("Partition writer stopped", status)));
        }
        return policy;
    }

    private void applyInitResult(WriterSlot slot, InitResult result, Throwable error) {
        if (!slot.opening) {
            return;
        }
        slot.opening = false;
        initializingCount--;
        if (error != null || result == null || result.getSeqNo() < 0) {
            Throwable problem = error != null ? error : new IllegalStateException("Invalid producer history");
            if (slot.probeBatch != null) {
                slot.probeError = problem;
                requestWriterShutdown(slot);
            } else {
                handleWriterFailure(slot, problem);
            }
            return;
        }
        slot.history = result.getSeqNo();
        if (slot.probeBatch != null || slot.retiring || closing) {
            requestWriterShutdown(slot);
            return;
        }
        long expected = histories.getOrDefault(slot.partition, 0L);
        if (slot.history > expected) {
            fail(new IllegalStateException("Producer history advanced outside this writer; prefix owner conflict"));
            return;
        }
        slot.ready = true;
        slot.lastActivity = now();
        dispatchReadyWrites();
    }

    private void handleWriterFailure(WriterSlot slot, Throwable error) {
        if (slot.probeBatch != null) {
            if (slot.probeError == null) {
                slot.probeError = error;
                requestWriterShutdown(slot);
            }
            return;
        }
        if (failure != null || closing || sendSlotsByPartition.get(slot.partition) != slot) {
            return;
        }
        if (permanent(error)) {
            fail(error);
        } else {
            recover(slot, error);
        }
    }

    protected void checkRecoveryAllowed() {
        // Ordinary messages may use history only after Describe confirms an inactive parent.
    }

    protected boolean mayConfirmFromHistory() {
        return true;
    }

    /** Seals admission at the same linearization point as send and flush. */
    protected final CompletableFuture<Void> prepareCommitBarrier() {
        synchronized (admission) {
            if (preparation != null) {
                return view(preparation);
            }
            if (failure != null) {
                return exceptional(failure);
            }
            checkAdmission();
            preparation = new CompletableFuture<>();
            admissionOpen = false;
            long admissionId = lastAdmissionId;
            barrierCount++;
            enqueueOnCoordinator(() -> addBarrier(admissionId, preparation));
            return view(preparation);
        }
    }

    protected final boolean isPrepared() {
        synchronized (admission) {
            return preparation != null && preparation.isDone() && !preparation.isCompletedExceptionally();
        }
    }

    protected final void sessionEvent(Runnable action) {
        tryEnqueueOnCoordinator(action);
    }

    private void recover(WriterSlot slot, Throwable error) {
        if (recoveries.containsKey(slot.partition) || failure != null || closing) {
            return;
        }
        checkRecoveryAllowed();
        long started = now();
        for (Pending message : pending.values()) {
            if (!message.done && message.partition == slot.partition) {
                if (message.recoveryStarted == null) {
                    message.recoveryStarted = started;
                }
                started = Math.min(started, message.recoveryStarted);
            }
        }
        Recovery recovery = new Recovery(slot, started, error);
        recoveries.put(slot.partition, recovery);
        requestWriterShutdown(slot);
        refreshTopologyForRecovery(recovery);
    }

    private void refreshTopologyForRecovery(Recovery recovery) {
        if (recovery.refreshing || recovery.probing || failure != null || closing) {
            return;
        }
        recovery.refreshing = true;
        describe(topology.getRoutingMode()).whenComplete((snapshot, error) -> {
            recovery.refreshing = false;
            if (error != null) {
                recoveryError(recovery, error);
                return;
            }
            try {
                snapshot.validateRefresh(topology);
                probeRecoveryHistory(recovery, snapshot);
            } catch (IllegalArgumentException invalid) {
                recoveryError(recovery, invalid);
            } catch (Throwable unexpected) {
                fail(unexpected);
            }
        });
    }

    private void probeRecoveryHistory(Recovery recovery, Topology snapshot) {
        PartitionInfo old = snapshot.get(recovery.slot.partition);
        if (old.isActive() || !recovery.slot.slotReleased) {
            recovery.nextRefresh = now() + REFRESH_NANOS;
            return;
        }
        if (snapshot.getRoutingMode() != RoutingMode.BOUND) {
            fail(new IllegalStateException("Split recovery requires bound routing", recovery.cause));
            return;
        }
        topology = snapshot;
        for (WriterSlot known : new ArrayList<>(sendSlotsByPartition.values())) {
            if (!snapshot.get(known.partition).isActive() && !recoveries.containsKey(known.partition)) {
                if (needed(known.partition)) {
                    recover(known, new IllegalStateException("Describe confirmed another inactive parent"));
                } else {
                    requestWriterShutdown(known);
                }
            }
        }
        recovery.probing = true;
        List<PartitionInfo> ancestors = new ArrayList<>();
        for (long id : snapshot.ancestors(old.getPartitionId())) {
            ancestors.add(snapshot.get(id));
        }
        readPartitionHistories(ancestors).whenComplete((values, error) -> {
            if (error != null) {
                recovery.probing = false;
                recoveryError(recovery, error);
                return;
            }
            try {
                finishRecovery(recovery, values);
            } catch (Throwable unexpected) {
                fail(unexpected);
            }
        });
    }

    private void recoveryError(Recovery recovery, Throwable error) {
        recovery.nextRefresh = now() + REFRESH_NANOS;
        if (permanent(error)) {
            fail(error);
        }
    }

    private void finishRecovery(Recovery recovery, Map<Long, Long> values) {
        long cut = 0;
        for (long value : values.values()) {
            cut = Math.max(cut, value);
        }
        if (cut > lastAssignedSeqNo) {
            fail(new IllegalStateException("Recovery history exceeds local sequence; prefix owner conflict"));
            return;
        }
        histories.putAll(values);
        // Pending is traversed in admission order: every resend is queued before newer records of its lineage.
        for (Pending message : new ArrayList<>(pending.values())) {
            if (message.done || message.partition != recovery.slot.partition) {
                continue;
            }
            if (mayConfirmFromHistory() && message.sequence <= cut && message.attempt != null) {
                settle(message, new MultiWriteResult(message.sequence, WriteAck.State.WRITTEN,
                        ConfirmationSource.PRODUCER_HISTORY, null, null), null);
            } else {
                message.partition = topology.route(message.key, message.explicitPartition);
                message.attempt = null;
            }
        }
        sendSlotsByPartition.remove(recovery.slot.partition, recovery.slot);
        recoveries.remove(recovery.slot.partition);
        dispatchReadyWrites();
        checkDrain();
    }

    private void requestWriterShutdown(WriterSlot slot) {
        slot.retiring = true;
        if (slot.writer == null && !slot.opening) {
            releaseWriterSlot(slot, slot.probeError);
            return;
        }
        if (slot.slotReleased || slot.shutdownScheduled || slot.sendCallInFlight || slot.writer == null) {
            return;
        }
        slot.shutdownScheduled = true;
        slot.closeStarted = now();
        dispatchTopicCall(() -> {
            try {
                slot.writer.shutdown().whenComplete((ignored, error) ->
                        tryEnqueueOnCoordinator(() -> releaseWriterSlot(slot, error)));
            } catch (Throwable error) {
                tryEnqueueOnCoordinator(() -> releaseWriterSlot(slot, error));
            }
        });
    }

    private void releaseWriterSlot(WriterSlot slot, Throwable error) {
        if (slot.slotReleased) {
            return;
        }
        slot.slotReleased = true;
        occupiedWriterSlots.remove(slot);
        if (slot.opening) {
            slot.opening = false;
            initializingCount--;
        }
        if (slot.probeBatch != null) {
            slot.probeBatch.recordProbeResult(slot, error);
            processProbeQueue();
        } else if (recoveries.containsKey(slot.partition)) {
            recoveries.get(slot.partition).nextRefresh = now();
        } else {
            sendSlotsByPartition.remove(slot.partition, slot);
        }
        if (error != null && failure == null) {
            fail(new IllegalStateException("Lower writer shutdown failed", error));
        }
        processProbeQueue();
        dispatchReadyWrites();
        checkDrain();
    }

    private void evictOneIdle() {
        for (WriterSlot slot : sendSlotsByPartition.values()) {
            if (slot.ready && !slot.retiring && !slot.sendCallInFlight && slot.count == 0 && !needed(slot.partition)) {
                requestWriterShutdown(slot);
                return;
            }
        }
    }

    private boolean needed(long partition) {
        for (Pending message : pending.values()) {
            if (!message.done && message.partition == partition) {
                return true;
            }
        }
        return false;
    }

    private void settle(Pending message, MultiWriteResult value, Throwable error) {
        if (message.done) {
            return;
        }
        message.done = true;
        message.data = null;
        message.metadata = null;
        completeSendFuture(message, value, error);
        Iterator<Pending> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            Pending head = iterator.next();
            if (!head.done) {
                break;
            }
            settledThroughAdmissionId = head.admissionId;
            iterator.remove();
            synchronized (admission) {
                head.removedFromPending = true;
                releaseBufferBudgetIfReady(head);
            }
        }
        checkBarriers();
        checkDrain();
    }

    private void addBarrier(long admissionId, CompletableFuture<Void> result) {
        barriers.add(new Barrier(admissionId, result));
        checkBarriers();
    }

    private void checkBarriers() {
        Iterator<Barrier> iterator = barriers.iterator();
        while (iterator.hasNext()) {
            Barrier barrier = iterator.next();
            if (failure != null || settledThroughAdmissionId >= barrier.admissionId) {
                iterator.remove();
                Throwable problem = failure;
                completeWithFinally(barrier.result, null, problem, false, () -> {
                    synchronized (admission) {
                        barrierCount--;
                    }
                });
            }
        }
    }

    protected final void fail(Throwable error) {
        if (failure != null || disposed) {
            return;
        }
        failure = error;
        synchronized (admission) {
            admissionOpen = false;
            if (!shutdownRequested) {
                closeStarted = now();
            }
            shutdownRequested = true;
        }
        if (!initialized.isDone()) {
            complete(initialized, null, error);
        }
        for (Pending message : new ArrayList<>(pending.values())) {
            settle(message, null, error);
        }
        checkBarriers();
        beginClosing();
    }

    private void checkDrain() {
        if (!shutdownRequested) {
            return;
        }
        if (!closing && pending.isEmpty()) {
            beginClosing();
        }
        if (closing && occupiedWriterSlots.isEmpty()) {
            finishClose(failure);
        }
    }

    private void beginClosing() {
        closing = true;
        for (WriterSlot slot : new ArrayList<>(occupiedWriterSlots)) {
            requestWriterShutdown(slot);
        }
        if (occupiedWriterSlots.isEmpty()) {
            finishClose(failure);
        }
    }

    private void finishClose(Throwable error) {
        if (disposed) {
            return;
        }
        checkBarriers();
        complete(closed, null, error);
        disposed = true;
        if (ticker != null) {
            ticker.cancel(false);
        }
        runtime.shutdown();
    }

    private void tick() {
        long current = now();
        if (shutdownRequested && current - closeStarted >= settings.getCloseTimeout().toNanos()) {
            TimeoutException timeout = new TimeoutException("Multiwriter close deadline; lower resources may remain");
            if (failure == null) {
                fail(timeout);
            }
            finishClose(timeout);
            return;
        }
        if (failure != null) {
            return;
        }
        if (!bootstrapped) {
            if (current - initStarted >= settings.getInitTimeout().toNanos()) {
                fail(new TimeoutException("Multiwriter initialization deadline"));
            } else if (!bootstrapping && current >= nextBootstrap) {
                bootstrap();
            }
            return;
        }
        for (WriterSlot slot : new ArrayList<>(occupiedWriterSlots)) {
            if (slot.shutdownScheduled && current - slot.closeStarted >= settings.getCloseTimeout().toNanos()) {
                fail(new TimeoutException("Lower writer close deadline; resources may remain"));
                return;
            }
            if (slot.opening && current - slot.started >= settings.getInitTimeout().toNanos()) {
                fail(new TimeoutException("Partition writer initialization deadline"));
                return;
            }
            if (slot.probeBatch == null && slot.ready && !slot.retiring && slot.count == 0 && !slot.sendCallInFlight
                    && !needed(slot.partition)
                    && current - slot.lastActivity >= settings.getIdleTimeout().toNanos()) {
                requestWriterShutdown(slot);
            }
        }
        for (Recovery recovery : new ArrayList<>(recoveries.values())) {
            if (current - recovery.started >= settings.getRecoveryTimeout().toNanos()) {
                fail(new IllegalStateException(
                        "Split recovery deadline; write outcome may be unknown", recovery.cause));
                return;
            }
            if (current >= recovery.nextRefresh) {
                refreshTopologyForRecovery(recovery);
            }
        }
        long delivery = settings.getDeliveryTimeout().toNanos();
        if (delivery > 0) {
            for (Pending message : pending.values()) {
                if (!message.done && current - message.accepted >= delivery) {
                    fail(new TimeoutException("Delivery deadline; server write outcome is unknown"));
                    return;
                }
            }
        }
        processProbeQueue();
        dispatchReadyWrites();
    }

    private void dispatchTopicCall(Runnable task) {
        try {
            runtime.topicCallExecutor.execute(task);
        } catch (RejectedExecutionException error) {
            fail(error);
        }
    }

    private void completeSendFuture(Pending message, MultiWriteResult value, Throwable error) {
        completeWithFinally(message.result, value, error, true, () -> {
            synchronized (admission) {
                message.completionReturned = true;
                releaseBufferBudgetIfReady(message);
            }
        });
    }

    // The admission lock guards these two conditions. Blocking user callbacks exert bounded backpressure.
    private void releaseBufferBudgetIfReady(Pending message) {
        if (message.removedFromPending && message.completionReturned && !message.released) {
            message.released = true;
            budgetCount--;
            budgetBytes -= message.bytes;
        }
    }

    private <T> void complete(CompletableFuture<T> future, T value, Throwable error) {
        completeWithFinally(future, value, error, false, () -> { });
    }

    private <T> void completeWithFinally(CompletableFuture<T> future, T value, Throwable error,
            boolean message, Runnable after) {
        Runnable completion = () -> {
            try {
                if (error == null) {
                    future.complete(value);
                } else {
                    future.completeExceptionally(error);
                }
            } finally {
                after.run();
            }
        };
        try {
            (message ? runtime.completion : runtime.controlCompletion).execute(completion);
        } catch (RejectedExecutionException rejected) {
            // No state lock is held here. Still resolve the user promise if an injected executor rejects work.
            completion.run();
        }
    }

    private static boolean permanent(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnexpectedResultException) {
                Status status = ((UnexpectedResultException) cause).getStatus();
                return status.getCode() != StatusCode.OVERLOADED
                        && TopicRetryConfig.STANDARD.getStatusRetryPolicy(status) == null;
            }
        }
        return false;
    }

    private static <T> CompletableFuture<T> exceptional(Throwable error) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(error);
        return result;
    }

    private static <T> CompletableFuture<T> view(CompletableFuture<T> source) {
        return source.thenApply(Function.identity());
    }

    private final class Pending {
        final long admissionId;
        final long bytes;
        final long encodedBytes;
        final long accepted = now();
        final String key;
        final Instant timestamp;
        final Long explicitPartition;
        final Long explicitSequence;
        final CompletableFuture<MultiWriteResult> result;
        byte[] data;
        List<MetadataItem> metadata;
        long sequence;
        long partition;
        Long recoveryStarted;
        Attempt attempt;
        boolean done;
        boolean registered;
        boolean removedFromPending;
        boolean completionReturned;
        boolean released;

        @SuppressWarnings("ParameterNumber")
        Pending(long admissionId, long bytes, long encodedBytes, byte[] data, List<MetadataItem> metadata,
                String key, MultiMessage input, long partition, CompletableFuture<MultiWriteResult> result) {
            this.admissionId = admissionId;
            this.bytes = bytes;
            this.encodedBytes = encodedBytes;
            this.data = data;
            this.metadata = new ArrayList<>(metadata);
            this.key = key;
            this.timestamp = input.getCreateTimestamp();
            this.explicitPartition = input.getPartitionId();
            this.explicitSequence = input.getSeqNo();
            this.partition = partition;
            this.result = result;
        }
    }

    private static final class WriterSlot {
        final long partition;
        final ProbeBatch probeBatch;
        final boolean pinned;
        volatile boolean retiring;
        AsyncWriter writer;
        boolean ready;
        boolean opening;
        boolean sendCallInFlight;
        boolean shutdownScheduled;
        boolean slotReleased;
        int count;
        long bytes;
        long history;
        long started;
        long lastActivity;
        long closeStarted;
        Throwable probeError;

        WriterSlot(long partition) {
            this.partition = partition;
            this.pinned = true;
            this.probeBatch = null;
        }

        WriterSlot(PartitionInfo partition, ProbeBatch probeBatch) {
            this.partition = partition.getPartitionId();
            this.pinned = partition.isActive();
            this.probeBatch = probeBatch;
        }
    }

    private static final class Attempt {
        final WriterSlot slot;
        final Pending message;
        boolean completed;

        Attempt(WriterSlot slot, Pending message) {
            this.slot = slot;
            this.message = message;
        }
    }

    private static final class Barrier {
        final long admissionId;
        final CompletableFuture<Void> result;

        Barrier(long admissionId, CompletableFuture<Void> result) {
            this.admissionId = admissionId;
            this.result = result;
        }
    }

    private static final class Recovery {
        final WriterSlot slot;
        final long started;
        final Throwable cause;
        long nextRefresh;
        boolean refreshing;
        boolean probing;

        Recovery(WriterSlot slot, long started, Throwable cause) {
            this.slot = slot;
            this.started = started;
            this.cause = cause;
        }
    }

    private static final class ProbeBatch {
        final Deque<PartitionInfo> remaining;
        final Map<Long, Long> seqNoByPartition = new HashMap<>();
        final CompletableFuture<Map<Long, Long>> result = new CompletableFuture<>();
        int inFlightProbes;
        Throwable error;

        ProbeBatch(Collection<PartitionInfo> partitions) {
            remaining = new ArrayDeque<>(partitions);
        }

        void recordProbeResult(WriterSlot slot, Throwable closeError) {
            inFlightProbes--;
            if (error == null) {
                error = slot.probeError != null ? slot.probeError : closeError;
            }
            seqNoByPartition.put(slot.partition, slot.history);
        }
    }
}
