package tech.ydb.table.impl.pool;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import org.junit.Assert;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class FutureHelper {
    protected <T> T readyFuture(Supplier<CompletableFuture<T>> supplier) {
        return futureIsReady(supplier.get());
    }

    protected <T> CompletableFuture<T> pendingFuture(CompletableFuture<T> future) {
        return futureIsPending(future);
    }

    protected <T> CompletableFuture<T> pendingFuture(Supplier<CompletableFuture<T>> supplier) {
        return futureIsPending(supplier.get());
    }

    protected <T> CompletableFuture<T> exceptionallyFuture(Supplier<CompletableFuture<T>> supplier, String message) {
        return futureIsExceptionally(supplier.get(), message);
    }

    protected <T> T futureIsReady(CompletableFuture<T> future) {
        Assert.assertTrue("Future is done", future.isDone());
        Assert.assertFalse("Future is valid", future.isCompletedExceptionally());
        return future.join();
    }

    protected <T> CompletableFuture<T> futureIsPending(CompletableFuture<T> future) {
        Assert.assertFalse("Future is not done", future.isDone());
        return future;
    }

    protected <T> CompletableFuture<T> futureIsCanceled(CompletableFuture<T> future, String cancelationMessage) {
        Assert.assertTrue("Future is done", future.isDone());
        Assert.assertTrue("Future is canceled", future.isCancelled());
        Assert.assertTrue("Future is exceptionally", future.isCompletedExceptionally());
        futureHasException(future, cancelationMessage);
        return future;
    }

    protected <T> CompletableFuture<T> futureIsExceptionally(CompletableFuture<T> future, String exceptionMessage) {
        Assert.assertTrue("Pending future is done", future.isDone());
        Assert.assertFalse("Future is canceled", future.isCancelled());
        Assert.assertTrue("Pending future is rejected", future.isCompletedExceptionally());
        futureHasException(future, exceptionMessage);
        return future;
    }

    private void futureHasException(CompletableFuture<?> future, String message) {
        try {
            future.get();
            Assert.assertFalse("Future must be exceptionally", true);
        } catch (CompletionException | ExecutionException ex) {
            Throwable reason = ex.getCause();
            Assert.assertEquals("Completion exception message", message, reason.getMessage());
        } catch (CancellationException ex) {
            Assert.assertEquals("Exception message", message, ex.getMessage());
        } catch (InterruptedException ex) {
            Assert.assertNotNull("Test interrupted", ex);
        }
    }


}
