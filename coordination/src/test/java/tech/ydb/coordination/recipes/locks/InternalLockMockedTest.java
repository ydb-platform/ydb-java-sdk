package tech.ydb.coordination.recipes.locks;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Test;
import tech.ydb.coordination.CoordinationSession;
import tech.ydb.coordination.CoordinationSessionBaseMockedTest;
import tech.ydb.core.StatusCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InternalLockMockedTest extends CoordinationSessionBaseMockedTest {

    /*
    Части функционала:
    tryAcquire + isAcquired (deadline, data, exclusive)
    release + isAcquired (уже освобожден, еще не захвачен?)

    listeners
    listeners + reconnection when session is interrupted

    session start
    close - (освобождает ресурсы)

    Внешние эффекты:
        1) состояние сессии (изначальное, в процессе)
            1.1) Дожидается открытия соединения
            1.2) Разрыв соединения (LOST, CLOSED) - возвращает сразу ошибку
        2) ответы на запросы
            2.1) При ошибке ретраит то что можно
            2.2) Кидает ошибку, если не ретраится
        3) прерывание потока
        4) контракт: вызвать 2 раза, 0 раз

    acquire:
        Критерии корректности:
            1) lock.isAcquired - нету нарушения внутреннего состояния (при успехе и не успехе)
            2) был успешный вызов session.isAcquired

        Тестовые кейсы:
            1) Все ОК - successAcquireTest
            2) Сессия сразу разорвана - failedAcquireOnLostSessionTest
            3) Сессия рвется в процессе захвата - failedAcquireDuringSessionLostTest
            4) Сессия рвется после захвата - successAcquireThenReleasedOnLostSessionTest
            4) При ответе ретраит - acquireRetriedOnRetryableTest
            5) При ответе ошибки кидает ошибку - acquireFailsOnNonRetryableErrorTest
            6) На блокировке может быть прервано - acquireInterruptedTest
            7) Уже захвачен - acquireFailsAlreadyAcquiredTest
            8) Все ОК (с таймаутом) - successTryAcquireTest
            9) Таймаут вышел - tryAcquireFailsTimeoutExceededTest

     release:
        Критерии корректности:
            1) lock.isAcquired - нету нарушения внутреннего состояния (при успехе и не успехе)
            2) был успешный вызов releaseSemaphore

        Тестовые кейсы:
            1) Все ОК (сессия, захват, освобождение) - release_RespondedStatusSuccess_ReleasedLock
            2) Семафор не был даже захвачен - release_NoCurrentLease_ReturnedFalse
            5) Вызван поверх LOST сессии - release_AlreadySessionLost_ThrowsLockReleaseFailedException
            3) Порвалась сессия в процессе освобождения - release_DuringSessionLost_ThrowsLockReleaseFailedException
            4) Корректно обрабатывается InterruptedException - release_Interrupted_StateConsistent

     */

    @Test
    public void acquire_RespondedStatusSuccess_AcquiresLock() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name");
        sessionMock.acquireEphemeralSemaphore()
                .then(successAcquire(lease));

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());
        verify(getCoordinationSession())
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );

        lock.close();
    }

    @Test
    public void acquire_WithCustomData_PropagatesToSession() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect().then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name");
        sessionMock.acquireEphemeralSemaphore()
                .then(successAcquire(lease));

        byte[] testData = "test_payload".getBytes(StandardCharsets.UTF_8);
        LockInternals.LeaseData leaseData = lock.tryAcquire(null, true, testData);

        Assert.assertNotNull(leaseData);
        Assert.assertTrue(leaseData.isExclusive());
        verify(getCoordinationSession()).acquireEphemeralSemaphore(
                eq("lock_name"),
                eq(true),   // exclusive
                eq(testData),
                any()    // deadline
        );
    }


    @Test
    public void acquire_LostSession_ThrowsLockAcquireFailedException() {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.lost();

        Assert.assertThrows(
                LockAcquireFailedException.class,
                () -> lock.tryAcquire(null, false, null)
        );
        Assert.assertFalse(lock.isAcquired());
    }

    @Test
    public void acquire_SessionLostDuringBlock_ThrowsLockAcquireFailedException() {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire())
                .then(lostAcquire())
                .then(successAcquire(lease("lock_name"))); // never reached

        Assert.assertThrows(
                LockAcquireFailedException.class,
                () -> lock.tryAcquire(null, false, null)
        );
        Assert.assertFalse(lock.isAcquired());
        verify(getCoordinationSession(), times(2))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void acquire_RespondedSuccessStatusThenLostSession_ReleasedLock() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        lock.getSessionListenable().addListener(
                getSessionStateAssert()
                        .next(CoordinationSession.State.CONNECTING)
                        .next(CoordinationSession.State.CONNECTED)
                        .next(CoordinationSession.State.LOST)
        );

        sessionMock.connecting();
        verify(getCoordinationSession())
                .connect();
        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(successAcquire(lease("lock_name")));

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());
        verify(getCoordinationSession())
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );

        sessionMock.lost();
        Assert.assertFalse(lock.isAcquired());
        getSessionStateAssert().finished();
    }

    @Test
    public void acquire_RespondedRetryableStatusThenSuccessStatus_AcquiredLock() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire())
                .then(statusAcquire(StatusCode.SESSION_BUSY))
                .then(statusAcquire(StatusCode.UNAVAILABLE))
                .then(statusAcquire(StatusCode.TRANSPORT_UNAVAILABLE))
                .then(successAcquire(lease("lock_name")));

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());
        verify(getCoordinationSession(), times(5))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
        ;
    }

    @Test
    public void acquire_RespondedNonRetryableStatus_ThrowsLockAcquireFailedException() {
        StatusCode badStatus = StatusCode.BAD_REQUEST;

        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(statusAcquire(badStatus))
                .then(successAcquire(lease("lock_name"))); // never reached

        Assert.assertThrows(
                LockAcquireFailedException.class,
                () -> lock.tryAcquire(null, false, null)
        );

        Assert.assertFalse(lock.isAcquired());
        verify(getCoordinationSession(), times(1))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void acquire_BlockingInterrupted_ThrowsInterruptedException() throws InterruptedException {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire(Duration.ofSeconds(120)))
                .then(successAcquire(lease("lock_name"))); // never reached

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            latch.countDown();
            Assert.assertThrows(
                    InterruptedException.class,
                    () -> lock.tryAcquire(null, false, null)
            );
        });

        Thread.sleep(20);
        latch.await();
        future.cancel(true);

        Assert.assertFalse(lock.isAcquired());
        verify(getCoordinationSession(), times(1))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void acquire_CallAcquireTwice_ThrowsLockAlreadyAcquiredException() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        verify(getCoordinationSession())
                .connect();

        sessionMock.connected();

        sessionMock.acquireEphemeralSemaphore()
                .then(successAcquire(lease("lock_name")))
                .then(statusAcquire(StatusCode.BAD_REQUEST)); // never reached

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());

        verify(getCoordinationSession(), times(1))
                .acquireEphemeralSemaphore(any(), anyBoolean(), any(), any());

        Assert.assertThrows(
                LockAlreadyAcquiredException.class,
                () -> lock.tryAcquire(null, false, null)
        );
        Assert.assertTrue(lock.isAcquired());
        verify(getCoordinationSession(), times(1))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void acquireWithTimeout_RespondedSuccessStatus_AcquiredLock() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name");
        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire(Duration.ofMillis(100)))
                .then(successAcquire(lease));

        Instant deadline = Instant.now().plus(Duration.ofMillis(1000));
        LockInternals.LeaseData leaseData = lock.tryAcquire(deadline, false, null);

        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());
        verify(getCoordinationSession(), times(2))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void acquireWithTimeout_ResponseTimeout_ReturnsFalse() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name");
        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire(Duration.ofMillis(100)))
                .then(successAcquire(lease)); // never reaches

        Instant deadline = Instant.now().plus(Duration.ofMillis(10));
        LockInternals.LeaseData leaseData = lock.tryAcquire(deadline, false, null);

        Assert.assertNull(leaseData);
        Assert.assertFalse(lock.isAcquired());
        verify(getCoordinationSession(), times(1))
                .acquireEphemeralSemaphore(
                        eq("lock_name"),
                        eq(false),
                        isNull(),
                        any()
                );
    }

    @Test
    public void release_RespondedStatusSuccess_ReleasedLock() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name");
        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire(Duration.ofMillis(100)))
                .then(successAcquire(lease));

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());

        Assert.assertTrue(lock.release());
        Assert.assertFalse(lock.isAcquired());
        lease.assertReleased();
    }

    @Test
    public void release_NoCurrentLease_ReturnedFalse() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        Assert.assertFalse(lock.release());
        Assert.assertFalse(lock.isAcquired());
    }

    @Test
    public void release_AlreadySessionLost_ThrowsLockReleaseFailedException() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();

        sessionMock.lost();

        Assert.assertThrows(LockReleaseFailedException.class, lock::release);
        Assert.assertFalse(lock.isAcquired());
    }

    @Test
    public void release_DuringSessionLost_ThrowsLockReleaseFailedException() throws Exception {
        SessionMock sessionMock = getSessionMock();
        sessionMock.connect()
                .then(successConnect());

        LockInternals lock = new LockInternals(
                getClient(),
                "/node/path",
                "lock_name"
        );
        lock.start();
        sessionMock.connected();

        LeaseMock lease = lease("lock_name")
                .failed(new IllegalStateException());
        sessionMock.acquireEphemeralSemaphore()
                .then(timeoutAcquire(Duration.ofMillis(100)))
                .then(successAcquire(lease));

        LockInternals.LeaseData leaseData = lock.tryAcquire(null, false, null);
        Assert.assertNotNull(leaseData);
        Assert.assertFalse(leaseData.isExclusive());
        Assert.assertTrue(lock.isAcquired());

        Assert.assertThrows(LockReleaseFailedException.class, lock::release);
        lease.assertReleased();
    }

}
