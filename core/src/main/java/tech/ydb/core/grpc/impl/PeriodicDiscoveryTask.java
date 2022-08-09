package tech.ydb.core.grpc.impl;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import tech.ydb.core.Result;
import tech.ydb.core.utils.Async;
import tech.ydb.discovery.DiscoveryProtos;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Nikolay Perfilov
 * @author Aleksandr Gorshenin
 */
public class PeriodicDiscoveryTask implements TimerTask {
    public interface DiscoveryHandler {
        boolean useMinDiscoveryPeriod();
        void handleDiscoveryResult(DiscoveryProtos.ListEndpointsResult result);
    }

    // Interval between discovery requests when everything is ok
    private static final long DISCOVERY_PERIOD_NORMAL_SECONDS = 60;
    // Interval between discovery requests when pessimization threshold is exceeded
    private static final long DISCOVERY_PERIOD_MIN_SECONDS = 5;
    
    private static final Logger logger = LoggerFactory.getLogger(PeriodicDiscoveryTask.class);
    
    private final GrpcDiscoveryRpc discoveryRpc;
    private final DiscoveryHandler discoveryHandler;

    private final AtomicBoolean updateInProgress = new AtomicBoolean();
    private volatile Instant lastUpdateTime;
    private volatile boolean stopped = false;
    private Timeout currentSchedule = null;

    public PeriodicDiscoveryTask(GrpcDiscoveryRpc rpc, DiscoveryHandler handler) {
        this.discoveryRpc = rpc;
        this.discoveryHandler = handler;
        this.lastUpdateTime = Instant.now();
    }

    void stop() {
        logger.debug("stopping PeriodicDiscoveryTask");
        stopped = true;
        if (currentSchedule != null) {
            currentSchedule.cancel();
            currentSchedule = null;
        }
    }

    void start() {
        logger.debug("first run of PeriodicDiscoveryTask");
        runDiscovery();
    }

    @Override
    public void run(Timeout timeout) {
        if (timeout.isCancelled() || stopped) {
            return;
        }
        
        if (discoveryHandler.useMinDiscoveryPeriod()) {
            runDiscovery();
        } else {
            if (Instant.now().isAfter(lastUpdateTime.plusSeconds(DISCOVERY_PERIOD_NORMAL_SECONDS))) {
                logger.debug("launching discovery in normal mode");
                runDiscovery();
            } else {
                logger.trace("no need to run discovery yet");
                scheduleNextDiscovery();
            }
        }
    }

    private void scheduleNextDiscovery() {
        currentSchedule = Async.runAfter(this, DISCOVERY_PERIOD_MIN_SECONDS, TimeUnit.SECONDS);
    }
    
    private void handleDiscoveryResponse(Result<DiscoveryProtos.ListEndpointsResult> response) {
        if (!response.isSuccess()) {
            logger.error("discovery problem {}", response);
            return;
        }

        DiscoveryProtos.ListEndpointsResult result = response.expect("couldn't get response from ListEndpointsResult");
        if (result.getEndpointsList().isEmpty()) { 
            logger.error("discovery get empty list of endpoints");
            return;
        }

        logger.debug("successfully received ListEndpoints result with {} endpoints",
                result.getEndpointsList().size());
        discoveryHandler.handleDiscoveryResult(result);
        lastUpdateTime = Instant.now();
    }

    private void runDiscovery() {
        if (!updateInProgress.compareAndSet(false, true)) {
            logger.debug("couldn't start update: already in progress");
            return;
        }
        
        logger.debug("updating endpoints, calling ListEndpoints...");
        discoveryRpc.listEndpoints().whenComplete((response, ex) -> {
            if (stopped) {
                updateInProgress.set(false);
                return;
            }

            if (ex != null) {
                logger.warn("couldn't perform discovery with exception", ex);
            }
            if (response != null) {
                handleDiscoveryResponse(response);
            }

            updateInProgress.set(false);
            scheduleNextDiscovery();
        });
    }
}
