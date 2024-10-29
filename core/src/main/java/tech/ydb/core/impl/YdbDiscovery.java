package tech.ydb.core.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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
import tech.ydb.core.utils.FutureTools;
import tech.ydb.proto.discovery.DiscoveryProtos;
import tech.ydb.proto.discovery.DiscoveryProtos.EndpointInfo;
import tech.ydb.proto.discovery.v1.DiscoveryServiceGrpc;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public class YdbDiscovery {
    private static final Status EMPTY_DISCOVERY = Status.of(StatusCode.CLIENT_DISCOVERY_FAILED)
            .withIssues(Issue.of("Discovery return empty list of endpoints", Issue.Severity.ERROR));

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;

    private static final Logger logger = LoggerFactory.getLogger(YdbDiscovery.class);

    public interface Handler {
        Instant instant();
        GrpcTransport createDiscoveryTransport();
        boolean needToForceDiscovery();
        CompletableFuture<Boolean> handleEndpoints(List<EndpointRecord> endpoints, String selfLocation);
    }

    private final Handler handler;
    private final ScheduledExecutorService scheduler;
    private final String discoveryDatabase;
    private final Duration discoveryTimeout;
    private final ReentrantLock readyLock = new ReentrantLock();
    private final Condition readyCondition = readyLock.newCondition();

    private volatile Instant lastUpdateTime;
    private volatile Future<?> currentSchedule = null;
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;
    private volatile Throwable lastException = null;

    public YdbDiscovery(Handler handler, ScheduledExecutorService scheduler, String database, Duration timeout) {
        this.handler = handler;
        this.scheduler = scheduler;
        this.lastUpdateTime = handler.instant();
        this.discoveryDatabase = database;
        this.discoveryTimeout = timeout;
    }

    public void start() {
        logger.debug("start periodic discovery task");
        currentSchedule = scheduler.submit(() -> {
            logger.info("Waiting for init discovery...");
            runDiscovery();
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

    public void waitReady(long millis) throws IllegalStateException {
        if (isStarted) {
            return;
        }

        readyLock.lock();

        try {
            if (isStarted) {
                return;
            }

            long timeout = millis > 0 ? millis : discoveryTimeout.toMillis();
            readyCondition.await(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            lastException = new IllegalStateException("Discovery waiting interrupted", ex);
        } finally {
            readyLock.unlock();
        }

        if (!isStarted) {
            if (lastException != null) {
                throw new IllegalStateException("Discovery failed", lastException);
            } else {
                throw new IllegalStateException("Discovery is not ready");
            }
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

        if (handler.needToForceDiscovery()) {
            logger.debug("launching discovery by endpoint pessimization");
            runDiscovery();
        } else {
            if (handler.instant().isAfter(lastUpdateTime.plusSeconds(DISCOVERY_PERIOD_NORMAL_SECONDS))) {
                logger.debug("launching discovery in normal mode");
                runDiscovery();
            } else {
                logger.trace("no need to run discovery yet");
                scheduleNextTick();
            }
        }
    }

    private void runDiscovery() {
        lastUpdateTime = handler.instant();
        try {
            final GrpcTransport transport = handler.createDiscoveryTransport();
            try {
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
            } catch (Throwable th) {
                transport.close();
                throw th;
            }
        } catch (Throwable th) {
            handleDiscoveryResult(null, th);
        }
    }

    private void handleThrowable(Throwable th) {
        readyLock.lock();

        try {
            lastException = th;
            scheduleNextTick();
            readyCondition.signalAll();
        } finally {
            readyLock.unlock();
        }
    }

    private void handleOk(String selfLocation, List<EndpointRecord> endpoints) {
        readyLock.lock();

        try {
            isStarted = true;
            lastException = null;
            handler.handleEndpoints(endpoints, selfLocation).whenComplete((res, th) -> scheduleNextTick());
            readyCondition.signalAll();
        } finally {
            readyLock.unlock();
        }
    }

    private static String createAddress(EndpointInfo e) {
        String addr;
        if (e.getIpV6Count() > 0 && e.getIpV6(0) != null && !e.getIpV6(0).isEmpty()) {
            addr = e.getIpV6(0);
        } else if (e.getIpV4Count() > 0 && e.getIpV4(0) != null && !e.getIpV4(0).isEmpty()) {
            addr = e.getIpV4(0);
        } else {
            addr = e.getAddress();
        }

        logger.debug("address {} will be used to connect to node {}", addr, e.getAddress());

        return addr;
    }

    private void handleDiscoveryResult(Result<DiscoveryProtos.ListEndpointsResult> response, Throwable th) {
        if (th != null) {
            Throwable cause = FutureTools.unwrapCompletionException(th);
            logger.warn("couldn't perform discovery with exception", cause);
            handleThrowable(cause);
            return;
        }

        try {
            DiscoveryProtos.ListEndpointsResult result = response.getValue();
            if (result.getEndpointsList().isEmpty()) {
                logger.error("discovery return empty list of endpoints");
                handleThrowable(new UnexpectedResultException("Discovery list is empty", EMPTY_DISCOVERY));
                return;
            }

            List<EndpointRecord> records = result.getEndpointsList().stream()
                    .map(e -> new EndpointRecord(createAddress(e), e.getPort(), e.getNodeId(), e.getLocation(),
                        e.getSslTargetNameOverride()))
                    .collect(Collectors.toList());

            logger.debug("successfully received ListEndpoints result with {} endpoints", records.size());
            handleOk(result.getSelfLocation(), records);
        } catch (UnexpectedResultException ex) {
            logger.warn("discovery fail {}", response);
            handleThrowable(ex);
        }
    }
}
