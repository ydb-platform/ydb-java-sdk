package tech.ydb.core.impl.discovery;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.utils.Async;
import tech.ydb.discovery.DiscoveryProtos;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public class PeriodicDiscoveryTask implements Runnable {
    private static final Status EMPTY_DISCOVERY = Status.of(StatusCode.CLIENT_DISCOVERY_FAILED)
            .withIssues(Issue.of("Discovery return empty list of endpoints", Issue.Severity.ERROR));

    public interface DiscoveryHandler {
        boolean useMinDiscoveryPeriod();
        void handleDiscoveryResult(DiscoveryProtos.ListEndpointsResult result);
    }

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;

    private static final Logger logger = LoggerFactory.getLogger(PeriodicDiscoveryTask.class);

    private final ScheduledExecutorService scheduler;
    private final GrpcDiscoveryRpc discoveryRpc;
    private final DiscoveryHandler discoveryHandler;

    private final AtomicBoolean updateInProgress = new AtomicBoolean();
    private final State state = new State();
    private volatile ScheduledFuture<?> currentSchedule = null;

    public PeriodicDiscoveryTask(ScheduledExecutorService scheduler, GrpcDiscoveryRpc rpc, DiscoveryHandler handler) {
        this.scheduler = scheduler;
        this.discoveryRpc = rpc;
        this.discoveryHandler = handler;
    }

    public void stop() {
        logger.debug("stopping PeriodicDiscoveryTask");
        state.stopped = true;
        if (currentSchedule != null) {
            currentSchedule.cancel(false);
            currentSchedule = null;
        }
    }

    public void start() {
        logger.info("Waiting for init discovery...");
        runDiscovery();
        state.waitReady();
        logger.info("Discovery is finished");
    }

    @Override
    public void run() {
        if (state.stopped) {
            return;
        }

        if (discoveryHandler.useMinDiscoveryPeriod()) {
            runDiscovery();
        } else {
            if (Instant.now().isAfter(state.lastUpdateTime.plusSeconds(DISCOVERY_PERIOD_NORMAL_SECONDS))) {
                logger.debug("launching discovery in normal mode");
                runDiscovery();
            } else {
                logger.trace("no need to run discovery yet");
                scheduleNextDiscovery();
            }
        }
    }

    private void scheduleNextDiscovery() {
        currentSchedule = scheduler.schedule(this, DISCOVERY_PERIOD_MIN_SECONDS, TimeUnit.SECONDS);
    }

    private void handleDiscoveryResponse(Result<DiscoveryProtos.ListEndpointsResult> response) {
        if (!response.isSuccess()) {
            logger.error("discovery fail {}", response);
            state.handleProblem(new UnexpectedResultException("discovery fail", response.getStatus()));
            return;
        }

        DiscoveryProtos.ListEndpointsResult result = response.getValue();
        if (result.getEndpointsList().isEmpty()) {
            logger.error("discovery return empty list of endpoints");
            state.handleProblem(new UnexpectedResultException("discovery fail", EMPTY_DISCOVERY));
            return;
        }

        logger.debug("successfully received ListEndpoints result with {} endpoints",
                result.getEndpointsList().size());
        discoveryHandler.handleDiscoveryResult(result);

        state.handleOK();
    }

    private void runDiscovery() {
        if (!updateInProgress.compareAndSet(false, true)) {
            logger.debug("couldn't start update: already in progress");
            return;
        }

        logger.debug("updating endpoints, calling ListEndpoints...");
        discoveryRpc.listEndpoints().whenComplete((response, ex) -> {
            if (state.stopped) {
                updateInProgress.set(false);
                return;
            }

            if (ex != null) {
                Throwable cause = Async.unwrapCompletionException(ex);
                logger.warn("couldn't perform discovery with exception", cause);
                state.handleProblem(cause);
            }
            if (response != null) {
                handleDiscoveryResponse(response);
            }

            updateInProgress.set(false);
            scheduleNextDiscovery();
        });
    }

    private static class State {
        private volatile Instant lastUpdateTime = Instant.now();
        private volatile boolean isReady = false;
        private volatile boolean stopped = false;
        private volatile RuntimeException lastProblem = null;
        private final Object readyLock = new Object();

        public void handleOK() {
            this.lastUpdateTime = Instant.now();

            if (!isReady) {
                // Wake up all waiting locks
                synchronized (readyLock) {
                    isReady = true;
                    lastProblem = null;
                    readyLock.notifyAll();
                }
            }
        }

        public void handleProblem(Throwable ex) {
            if (isReady) {
                logger.error("discovery problem", ex);
                return;
            }

            // Wake up all waiting locks
            synchronized (readyLock) {
                if (isReady) {
                    logger.error("discovery problem", ex);
                    return;
                }

                isReady = false;
                if (ex instanceof RuntimeException) {
                    lastProblem = (RuntimeException) ex;
                } else {
                    lastProblem = new RuntimeException("Check ready problem", ex);
                }
                readyLock.notifyAll();
            }
        }

        public void waitReady() {
            if (isReady) {
                return;
            }

            synchronized (readyLock) {
                if (isReady) {
                    return;
                }

                if (lastProblem != null) {
                    throw lastProblem;
                }

                try {
                    readyLock.wait(TimeUnit.SECONDS.toMillis(DISCOVERY_PERIOD_MIN_SECONDS));

                    if (lastProblem != null) {
                        throw lastProblem;
                    }
                } catch (InterruptedException ex) {
                    logger.warn("ydb transport wait for ready interrupted", ex);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
