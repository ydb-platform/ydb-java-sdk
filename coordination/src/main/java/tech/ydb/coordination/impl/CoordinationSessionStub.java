package tech.ydb.coordination.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import tech.ydb.coordination.CoordinationSession;
import tech.ydb.core.Status;
import tech.ydb.proto.coordination.SemaphoreDescription;
import tech.ydb.proto.coordination.SessionRequest.AcquireSemaphore;
import tech.ydb.proto.coordination.SessionRequest.CreateSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DeleteSemaphore;
import tech.ydb.proto.coordination.SessionRequest.DescribeSemaphore;
import tech.ydb.proto.coordination.SessionRequest.PingPong;
import tech.ydb.proto.coordination.SessionRequest.ReleaseSemaphore;
import tech.ydb.proto.coordination.SessionRequest.SessionStart;
import tech.ydb.proto.coordination.SessionRequest.UpdateSemaphore;

public class CoordinationSessionStub implements CoordinationSession {
    private static AtomicLong delay = new AtomicLong(0);
    private static AtomicLong numberOfStartSession = new AtomicLong(0);
    private static AtomicLong numberOfPingPong = new AtomicLong(0);
    private static AtomicLong numberOfAcquireSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfReleaseSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfDescribeSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfCreateSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfUpdateSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfDeleteSemaphore = new AtomicLong(0);
    private final AtomicLong sessionId = new AtomicLong();
    private Observer observer;

    public static AtomicLong getNumberOfStartSession() {
        return numberOfStartSession;
    }

    public static void setNumberOfStartSession(AtomicLong numberOfStartSession) {
        CoordinationSessionStub.numberOfStartSession = numberOfStartSession;
    }

    public static AtomicLong getNumberOfPingPong() {
        return numberOfPingPong;
    }

    public static void setNumberOfPingPong(AtomicLong numberOfPingPong) {
        CoordinationSessionStub.numberOfPingPong = numberOfPingPong;
    }

    public static AtomicLong getNumberOfAcquireSemaphore() {
        return numberOfAcquireSemaphore;
    }

    public static void setNumberOfAcquireSemaphore(AtomicLong numberOfAcquireSemaphore) {
        CoordinationSessionStub.numberOfAcquireSemaphore = numberOfAcquireSemaphore;
    }

    public static AtomicLong getNumberOfReleaseSemaphore() {
        return numberOfReleaseSemaphore;
    }

    public static void setNumberOfReleaseSemaphore(AtomicLong numberOfReleaseSemaphore) {
        CoordinationSessionStub.numberOfReleaseSemaphore = numberOfReleaseSemaphore;
    }

    public static AtomicLong getNumberOfDescribeSemaphore() {
        return numberOfDescribeSemaphore;
    }

    public static void setNumberOfDescribeSemaphore(AtomicLong numberOfDescribeSemaphore) {
        CoordinationSessionStub.numberOfDescribeSemaphore = numberOfDescribeSemaphore;
    }

    public static AtomicLong getNumberOfCreateSemaphore() {
        return numberOfCreateSemaphore;
    }

    public static void setNumberOfCreateSemaphore(AtomicLong numberOfCreateSemaphore) {
        CoordinationSessionStub.numberOfCreateSemaphore = numberOfCreateSemaphore;
    }

    public static AtomicLong getNumberOfUpdateSemaphore() {
        return numberOfUpdateSemaphore;
    }

    public static void setNumberOfUpdateSemaphore(AtomicLong numberOfUpdateSemaphore) {
        CoordinationSessionStub.numberOfUpdateSemaphore = numberOfUpdateSemaphore;
    }

    public static AtomicLong getNumberOfDeleteSemaphore() {
        return numberOfDeleteSemaphore;
    }

    public static void setNumberOfDeleteSemaphore(AtomicLong numberOfDeleteSemaphore) {
        CoordinationSessionStub.numberOfDeleteSemaphore = numberOfDeleteSemaphore;
    }

    public static AtomicLong getDelay() {
        return delay;
    }

    public static void setDelay(AtomicLong delay) {
        CoordinationSessionStub.delay = delay;
    }

    @Override
    public long getSessionId() {
        return sessionId.get();
    }

    @Override
    public CompletableFuture<Status> start(Observer observer) {
        this.observer = observer;
        return CompletableFuture.completedFuture(Status.SUCCESS);
    }

    @Override
    public void sendStartSession(SessionStart sessionStart) {
        runAfterDelay(() -> numberOfStartSession.incrementAndGet()).thenRun(() -> observer.onSessionStarted()).join();
    }

    @Override
    public void sendPingPong(PingPong pingPong) {
        runAfterDelay(() -> numberOfPingPong.incrementAndGet()).thenRun(() -> observer.onPong(pingPong.getOpaque()))
                .join();
    }

    @Override
    public void sendAcquireSemaphore(AcquireSemaphore acquireSemaphore) {
        runAfterDelay(() -> numberOfAcquireSemaphore.incrementAndGet()).thenRun(
                () -> observer.onAcquireSemaphoreResult(true, Status.SUCCESS)).join();
    }

    @Override
    public void sendReleaseSemaphore(ReleaseSemaphore releaseSemaphore) {
        runAfterDelay(() -> numberOfReleaseSemaphore.incrementAndGet()).thenRun(
                () -> observer.onReleaseSemaphoreResult(true, Status.SUCCESS)).join();
    }

    @Override
    public void sendDescribeSemaphore(DescribeSemaphore describeSemaphore) {
        runAfterDelay(() -> numberOfDescribeSemaphore.incrementAndGet()).thenRun(
                        () -> observer.onDescribeSemaphoreResult(SemaphoreDescription.newBuilder().build(),
                                Status.SUCCESS))
                .join();
    }

    @Override
    public void sendCreateSemaphore(CreateSemaphore createSemaphore) {
        runAfterDelay(() -> numberOfCreateSemaphore.incrementAndGet()).thenRun(
                () -> observer.onCreateSemaphoreResult(Status.SUCCESS)).join();
    }

    @Override
    public void sendUpdateSemaphore(UpdateSemaphore updateSemaphore) {
        runAfterDelay(() -> numberOfUpdateSemaphore.incrementAndGet()).thenRun(
                () -> observer.onUpdateSemaphoreResult(updateSemaphore.getReqId(), Status.SUCCESS)).join();
    }

    @Override
    public void sendDeleteSemaphore(DeleteSemaphore deleteSemaphore) {
        runAfterDelay(() -> numberOfDeleteSemaphore.incrementAndGet()).thenRun(
                () -> observer.onDeleteSemaphoreResult(Status.SUCCESS)).join();
    }

    @Override
    public void stop() {
    }

    private CompletableFuture<Void> runAfterDelay(Runnable runnable) {
        return CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(delay.get());
                    } catch (InterruptedException e) {
                        runAfterDelay(runnable);
                    }
                }
        ).thenRun(runnable);
    }
}
