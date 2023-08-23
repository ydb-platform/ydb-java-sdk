package tech.ydb.coordination;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

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
    private static AtomicLong numberOfStartSession = new AtomicLong(0);
    private static AtomicLong numberOfPingPong = new AtomicLong(0);
    private static AtomicLong numberOfAcquireSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfReleaseSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfDescribeSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfCreateSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfUpdateSemaphore = new AtomicLong(0);
    private static AtomicLong numberOfDeleteSemaphore = new AtomicLong(0);

    private static Queue<Runnable> stagesQueue = new ArrayDeque<>();
    private final AtomicLong sessionId = new AtomicLong();
    private Observer observer;

    public static Queue<Runnable> getStagesQueue() {
        return stagesQueue;
    }

    public static void runNStagesInQueue(int n) {
        final int size = stagesQueue.size();
        for (int i = 0; i < Math.min(n, size); i++) {
            stagesQueue.remove().run();
        }
    }

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
        stagesQueue.add(() -> {
            numberOfStartSession.incrementAndGet();
            observer.onSessionStarted();
        });
    }

    @Override
    public void sendPingPong(PingPong pingPong) {
        stagesQueue.add(() -> {
            numberOfPingPong.incrementAndGet();
            observer.onPong(pingPong.getOpaque());
        });
    }

    @Override
    public void sendAcquireSemaphore(AcquireSemaphore acquireSemaphore) {
        stagesQueue.add(() -> {
            numberOfAcquireSemaphore.incrementAndGet();
            observer.onAcquireSemaphoreResult(true, Status.SUCCESS);
        });
    }

    @Override
    public void sendReleaseSemaphore(ReleaseSemaphore releaseSemaphore) {
        stagesQueue.add(() -> {
            numberOfReleaseSemaphore.incrementAndGet();
            observer.onReleaseSemaphoreResult(true, Status.SUCCESS);
        });
    }

    @Override
    public void sendDescribeSemaphore(DescribeSemaphore describeSemaphore) {
        stagesQueue.add(() -> {
            numberOfDescribeSemaphore.incrementAndGet();
            observer.onDescribeSemaphoreResult(SemaphoreDescription.newBuilder().build(), Status.SUCCESS);
        });
    }

    @Override
    public void sendCreateSemaphore(CreateSemaphore createSemaphore) {
        stagesQueue.add(() -> {
            numberOfCreateSemaphore.incrementAndGet();
            observer.onCreateSemaphoreResult(Status.SUCCESS);
        });
    }

    @Override
    public void sendUpdateSemaphore(UpdateSemaphore updateSemaphore) {
        stagesQueue.add(() -> {
            numberOfUpdateSemaphore.incrementAndGet();
            observer.onUpdateSemaphoreResult(updateSemaphore.getReqId(), Status.SUCCESS);
        });
    }

    @Override
    public void sendDeleteSemaphore(DeleteSemaphore deleteSemaphore) {
        stagesQueue.add(() -> {
            numberOfDeleteSemaphore.incrementAndGet();
            observer.onDeleteSemaphoreResult(Status.SUCCESS);
        });
    }

    @Override
    public void stop() {
    }
}
