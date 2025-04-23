package tech.ydb.coordination.recipes.watch;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.description.SemaphoreDescription;
import tech.ydb.coordination.recipes.util.Listenable;
import tech.ydb.coordination.recipes.util.ListenableAdder;
import tech.ydb.coordination.recipes.util.ListenableProvider;
import tech.ydb.coordination.recipes.util.ListenerWrapper;
import tech.ydb.coordination.settings.DescribeSemaphoreMode;
import tech.ydb.coordination.settings.WatchSemaphoreMode;
import tech.ydb.core.Result;
import tech.ydb.core.Status;

import java.io.Closeable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SemaphoreWatchListener implements ListenableAdder<WatchData>, ListenableProvider<WatchData>, Closeable {
    private static final Logger logger = LoggerFactory.getLogger(SemaphoreWatchListener.class);

    private final CoordinationSession session;
    private final String semaphoreName;
    private final ListenerWrapper<WatchData> listenableWrapper;

    private AtomicReference<State> state;
    private Future<Void> watchTask;
    private volatile WatchData watchData;
    private Set<Consumer<WatchData>> listeners;

    public SemaphoreWatchListener(CoordinationSession session, String semaphoreName) {
        this.session = session;
        this.semaphoreName = semaphoreName;
        this.state = new AtomicReference<>(State.CREATED);
        this.watchTask = null;
        this.watchData = null;
        this.listeners = new HashSet<>();
        this.listenableWrapper = new ListenerWrapper<>(this);
    }

    public enum State {
        CREATED,
        STARTED,
        CLOSED
    }

    public List<Participant> getOwners() {
        // TODO: block until initialized or throw exception or return default value or return Optional.empty()
        Preconditions.checkState(watchData == null, "Is not yet fetched state");

        return Collections.unmodifiableList(watchData.owners); // TODO: copy Participant.data[]?
    }

    public List<Participant> getWaiters() {
        Preconditions.checkState(watchData == null, "Is not yet fetched state");

        return Collections.unmodifiableList(watchData.waiters); // TODO: copy Participant.data[]?
    }

    public List<Participant> getParticipants() {
        Preconditions.checkState(watchData == null, "Is not yet fetched state");

        return Collections.unmodifiableList(watchData.participants); // TODO: copy Participant.data[]?
    }

    public long getCount() {
        Preconditions.checkState(watchData == null, "Is not yet fetched state");

        return watchData.count;
    }

    public byte[] getData() {
        Preconditions.checkState(watchData == null, "Is not yet fetched state");

        return watchData.data.clone();
    }

    public boolean start() {
        Preconditions.checkState(state.compareAndSet(State.CREATED, State.STARTED), "Already started or closed");

        return enqueueWatch();
    }

    private synchronized boolean enqueueWatch() {
        if (watchIsQueued() && state.get() == State.STARTED) {
            return false;
        }

        watchTask = watchSemaphore().thenCompose(status -> {
            if (!status.isSuccess()) {
                // TODO: stop watching on error?
                logger.error("Wailed to watch semaphore: {} with status: {}", semaphoreName, status);
            }

            finish();
            return null;
        });
        return true;
    }

    private boolean watchIsQueued() {
        return watchTask != null;
    }

    private synchronized void finish() {
        watchTask = null;
        enqueueWatch();
    }

    private CompletableFuture<Status> watchSemaphore() {
        return session.watchSemaphore(
                semaphoreName,
                DescribeSemaphoreMode.WITH_OWNERS_AND_WAITERS,
                WatchSemaphoreMode.WATCH_DATA_AND_OWNERS
        ).thenCompose(result -> {
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                return CompletableFuture.completedFuture(status);
            }
            tech.ydb.coordination.description.SemaphoreWatcher watcher = result.getValue();
            saveWatchState(watcher.getDescription());
            return watcher.getChangedFuture().thenApply(Result::getStatus);
        });
    }

    private void saveWatchState(SemaphoreDescription description) {
        List<Participant> waitersList = description.getWaitersList().stream().map(it -> new Participant(
                it.getId(),
                it.getData(),
                it.getCount(),
                false
        )).collect(Collectors.toList());
        List<Participant> ownersList = description.getOwnersList().stream().map(it -> new Participant(
                it.getId(),
                it.getData(),
                it.getCount(),
                true
        )).collect(Collectors.toList());

        watchData = new WatchData(
                description.getCount(),
                description.getData(),
                waitersList,
                ownersList
        );
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(listener -> listener.accept(watchData));
    }

    private synchronized void stopWatch() {
        Future<Void> task = watchTask;
        if (task != null) {
            task.cancel(true);
        }
        watchTask = null;
    }

    public State getState() {
        return state.get();
    }

    @Override
    public Listenable<WatchData> getListenable() {
        return listenableWrapper;
    }

    @Override
    public void addListener(Consumer<WatchData> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<WatchData> listener) {
        listeners.remove(listener);
    }

    @Override
    public void close() {
        Preconditions.checkState(state.compareAndSet(State.STARTED, State.CLOSED), "Is not yet started");

        stopWatch();
    }
}
