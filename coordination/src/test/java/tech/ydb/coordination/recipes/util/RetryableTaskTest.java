package tech.ydb.coordination.recipes.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import tech.ydb.common.retry.RetryPolicy;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RetryableTaskTest {
    @Mock
    private Supplier<CompletableFuture<Status>> taskSupplier;

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private RetryPolicy retryPolicy;

    private RetryableTask retryableTask;
    private final String taskName = "testTask";

    @Before
    public void setUp() {
        retryableTask = new RetryableTask(taskName, taskSupplier, executor, retryPolicy);
    }

    @Test
    public void testExecute_SuccessOnFirstAttempt() {
        Status successStatus = Status.SUCCESS;
        CompletableFuture<Status> future = CompletableFuture.completedFuture(successStatus);

        when(taskSupplier.get()).thenReturn(future);

        CompletableFuture<Status> result = retryableTask.execute();

        assertTrue(result.isDone());
        assertEquals(successStatus, result.join());
    }

    @Test
    public void testExecute_FailureWithRetries() {
        Status failureStatus = Status.of(StatusCode.CLIENT_INTERNAL_ERROR);
        RuntimeException exception = new RuntimeException("Operation failed");

        // First attempt fails
        CompletableFuture<Status> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(exception);

        when(taskSupplier.get())
                .thenReturn(failedFuture)
                .thenReturn(CompletableFuture.completedFuture(failureStatus));

        when(retryPolicy.nextRetryMs(anyInt(), anyLong()))
                .thenReturn(100L) // First retry after 100ms
                .thenReturn(-1L); // No more retries

        CompletableFuture<Status> result = retryableTask.execute();

        // Verify retry was scheduled
        verify(executor).schedule(any(Runnable.class), eq(100L), eq(TimeUnit.MILLISECONDS));

        // Simulate retry execution
        retryableTask.attemptTask(result);

        assertTrue(result.isDone());
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    public void testExecute_SuccessAfterRetry() {
        Status successStatus = Status.SUCCESS;
        RuntimeException exception = new RuntimeException("Temporary failure");

        // First attempt fails
        CompletableFuture<Status> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(exception);

        when(taskSupplier.get())
                .thenReturn(failedFuture)
                .thenReturn(CompletableFuture.completedFuture(successStatus));

        when(retryPolicy.nextRetryMs(anyInt(), anyLong()))
                .thenReturn(0L); // Immediate retry

        CompletableFuture<Status> result = retryableTask.execute();

        // Verify immediate retry was scheduled
        verify(executor).execute(any(Runnable.class));

        // Simulate retry execution
        retryableTask.attemptTask(result);

        assertTrue(result.isDone());
        assertEquals(successStatus, result.join());
    }

    @Test
    public void testExecute_NoMoreRetries() {
        RuntimeException exception = new RuntimeException("Permanent failure");

        CompletableFuture<Status> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(exception);

        when(taskSupplier.get()).thenReturn(failedFuture);
        when(retryPolicy.nextRetryMs(anyInt(), anyLong())).thenReturn(-1L); // No more retries

        CompletableFuture<Status> result = retryableTask.execute();

        assertTrue(result.isDone());
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    public void testExecute_TaskSupplierThrowsException() {
        RuntimeException exception = new RuntimeException("Supplier failure");

        when(taskSupplier.get()).thenThrow(exception);
        when(retryPolicy.nextRetryMs(anyInt(), anyLong())).thenReturn(-1L); // No more retries

        CompletableFuture<Status> result = retryableTask.execute();

        assertTrue(result.isDone());
        assertTrue(result.isCompletedExceptionally());
    }
}
