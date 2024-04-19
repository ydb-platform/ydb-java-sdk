package tech.ydb.core.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.ydb.core.Issue;
import tech.ydb.core.Result;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;
import tech.ydb.core.UnexpectedResultException;
import tech.ydb.core.grpc.GrpcRequestSettings;
import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.core.impl.pool.EndpointRecord;
import tech.ydb.core.operation.OperationBinder;
import tech.ydb.core.utils.Async;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public abstract class YdbDiscovery {
    private static final Status EMPTY_DISCOVERY = Status.of(StatusCode.CLIENT_DISCOVERY_FAILED)
            .withIssues(Issue.of("Discovery return empty list of endpoints", Issue.Severity.ERROR));

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;

    private static final Logger logger = LoggerFactory.getLogger(YdbDiscovery.class);

    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final String discoveryDatabase;
    private final Duration discoveryTimeout;
    private final Object readyObj = new Object();

    private volatile boolean isStopped = false;
    private volatile Instant lastUpdateTime;
    private volatile Future<?> currentSchedule = null;
    private RuntimeException lastException = null;

    public YdbDiscovery(Clock clock, ScheduledExecutorService scheduler, String database, Duration timeout) {
        this.clock = clock;
        this.scheduler = scheduler;
        this.lastUpdateTime = clock.instant();
        this.discoveryDatabase = database;
        this.discoveryTimeout = timeout;
    }

    public void start() {
        logger.debug("start periodic discovery task");
        currentSchedule = scheduler.submit(() -> {
            logger.info("Waiting for init discovery...");
            runDiscovery();
            logger.info("Discovery is finished");
        });
    }

    public void stop() {
        logger.debug("stopping PeriodicDiscoveryTask");
        isStopped = true;
        if (currentSchedule != null) {
            currentSchedule.cancel(false);
            currentSchedule = null;
        }
    }

    public void waitReady() {
        synchronized (readyObj) {
            try {
                long waitMillis = 2 * discoveryTimeout.toMillis();
                readyObj.wait(waitMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                handleThrowable(ex);
            }
        }

        if (lastException != null) {
            throw lastException;
        }
    }

    private void scheduleNextTick() {
        if (!isStopped) {
            logger.trace("schedule next discovery in {} seconds", DISCOVERY_PERIOD_MIN_SECONDS);
            currentSchedule = scheduler.schedule(this::tick, DISCOVERY_PERIOD_MIN_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void tick() {
        if (isStopped) {
            return;
        }

        if (forceDiscovery()) {
            logger.debug("launching discovery by endpoint pessimization");
            runDiscovery();
        } else {
            if (Instant.now().isAfter(lastUpdateTime.plusSeconds(DISCOVERY_PERIOD_NORMAL_SECONDS))) {
                logger.debug("launching discovery in normal mode");
                runDiscovery();
            } else {
                logger.trace("no need to run discovery yet");
                scheduleNextTick();
            }
        }
    }

    protected abstract GrpcTransport createDiscoveryTransport();
    protected abstract boolean forceDiscovery();
    protected abstract void handleEndpoints(List<EndpointRecord> endpoints, String selfLocation);

    private void runDiscovery() {
        if (isStopped) {
            return;
        }

        lastUpdateTime = clock.instant();

        final GrpcTransport transport = createDiscoveryTransport();
        logger.debug("execute list endpoints on {} with timeout {}", transport, discoveryTimeout);
        DiscoveryProtos.ListEndpointsRequest request = DiscoveryProtos.ListEndpointsRequest.newBuilder()
                .setDatabase(discoveryDatabase)
                .build();

        GrpcRequestSettings grpcSettings = GrpcRequestSettings.newBuilder()
                .withDeadline(discoveryTimeout)
                .build();

        transport.unaryCall(DiscoveryServiceGrpc.getListEndpointsMethod(), grpcSettings, request)
                .whenComplete((res, ex) -> transport.close()) // close transport for any result
                .thenApply(OperationBinder.bindSync(
                        DiscoveryProtos.ListEndpointsResponse::getOperation,
                        DiscoveryProtos.ListEndpointsResult.class
                ))
                .whenComplete(this::handleDiscoveryResult);
    }

    private void handleThrowable(Throwable th) {
        synchronized (readyObj) {
            if (th instanceof RuntimeException) {
                lastException = (RuntimeException) th;
            } else {
                lastException = new RuntimeException("Discovery failed", th);
            }
            readyObj.notifyAll();
        }
    }

    private void handleDiscoveryResult(Result<DiscoveryProtos.ListEndpointsResult> response, Throwable th) {
        if (th != null) {
            Throwable cause = Async.unwrapCompletionException(th);
            logger.warn("couldn't perform discovery with exception", cause);
            handleThrowable(cause);
            scheduleNextTick();
            return;
        }

        try {
            DiscoveryProtos.ListEndpointsResult result = response.getValue();
            if (result.getEndpointsList().isEmpty()) {
                logger.error("discovery return empty list of endpoints");
                handleThrowable(new UnexpectedResultException("Discovery failed", EMPTY_DISCOVERY));
                scheduleNextTick();
                return;
            }

            List<EndpointRecord> records = result.getEndpointsList().stream()
                    .map(e -> new EndpointRecord(e.getAddress(), e.getPort(), e.getNodeId(), e.getLocation()))
                    .collect(Collectors.toList());

            logger.debug("successfully received ListEndpoints result with {} endpoints", records.size());
            synchronized (readyObj) {
                lastException = null;
                handleEndpoints(records, result.getSelfLocation());
                readyObj.notifyAll();
            }

            scheduleNextTick();
        } catch (UnexpectedResultException ex) {
            logger.error("discovery fail {}", response);
            handleThrowable(th);
            scheduleNextTick();
        }
    }
}
