package tech.ydb.core.operation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class OperationTray {
    private static final Logger logger = LoggerFactory.getLogger(OperationTray.class);

    private OperationTray() { }

    public static <T> CompletableFuture<T> fetchOperation(Operation<T> operation, int rateSeconds) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (operation.isReady()) {
            logger.debug("{} is already done", operation);
            future.complete(operation.getValue());
            return future;
        }

        if (operation instanceof AsyncOperation) {
            long started = System.currentTimeMillis();
            fetch(null, future, (AsyncOperation<T>) operation, started, rateSeconds);
            return future;
        }

        logger.error("unknown type of {}", operation);
        throw new IllegalArgumentException("Unknown type of operation");
    }

    private static <T> boolean complete(Throwable th, CompletableFuture<T> f, AsyncOperation<T> o, long elapsed) {
        if (th != null) {
            logger.error("cannot fetch the operation {}, {} ms elapsed", o, elapsed, th);
            f.completeExceptionally(th);
            return false;
        }

        if (!o.isReady()) {
            return false;
        }

        logger.info("{} is done, {} ms elapsed", o, elapsed);
        f.complete(o.getValue());
        return true;
    }

    private static <T> void fetch(Throwable th, CompletableFuture<T> f, AsyncOperation<T> o, long started, int rs) {
        long elapsed = System.currentTimeMillis() - started;

        if (complete(th, f, o, elapsed)) {
            return;
        }

        logger.info("fetch the operation {} update in {} seconds, {} ms elapsed", o, rs, elapsed);
        o.fetch().whenComplete((res, th2) -> {
            long elapsed2 = System.currentTimeMillis() - started;
            if (complete(th2, f, o, elapsed2)) {
                return;
            }

            if (res != null) {
                logger.info("got operation {} status {}, schedule next update in {} seconds", o, res, rs);
            }
            o.getScheduler().schedule(() -> fetch(th, f, o, started, rs), rs, TimeUnit.SECONDS);
        });
    }
}
