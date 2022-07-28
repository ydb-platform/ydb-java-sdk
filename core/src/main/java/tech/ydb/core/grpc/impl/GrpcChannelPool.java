package tech.ydb.core.grpc.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tech.ydb.core.grpc.ChannelSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.CompletableFuture.allOf;

/**
 * @author Nikolay Perfilov
 */
public class GrpcChannelPool {
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannelPool.class);

    // How long should we wait for already running requests to complete before shutting down channels
    public static final long WAIT_FOR_REQUESTS_MS = 1000;
    // How long should we wait for executor termination after shutdownTimeoutMs and WAIT_FOR_REQUESTS_MS
    public static final long WAIT_FOR_EXECUTOR_SHUTDOWN_MS = 500;

    private final Map<String, GrpcChannel> channels = new ConcurrentHashMap<>();
    private final ChannelSettings channelSettings;

    public GrpcChannelPool(ChannelSettings channelSettings, List<EndpointRecord> initialEndpoints) {
        this.channelSettings = channelSettings;
        for (EndpointRecord endpoint : initialEndpoints) {
            channels.put(endpoint.getHostAndPort(), new GrpcChannel(endpoint, channelSettings));
        }
    }

    public GrpcChannel getChannel(EndpointRecord endpoint) {
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8161372 to prevent unnecessary locks in Java 8
        // Was fixed in Java 9+
        GrpcChannel result = channels.get(endpoint.getHostAndPort());

        return result != null ? result : channels.computeIfAbsent(endpoint.getHostAndPort(), (key) -> {
            logger.debug("channel " + endpoint.getHostAndPort() + " was not found in pool, creating one...");
            return new GrpcChannel(endpoint, channelSettings);
        });
    }

    private CompletableFuture<Boolean> shutdownChannels(Stream<GrpcChannel> channelsToShutdown, int channelCount,
                                                        long shutdownTimeoutMs) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(channelCount);
        try {
            List<CompletableFuture<Boolean>> futures = channelsToShutdown
                    .map(channel -> {
                        CompletableFuture<Boolean> promise = new CompletableFuture<>();
                        if (channel != null) {
                            executor.schedule(
                                    () -> { promise.complete(channel.shutdown(shutdownTimeoutMs)); },
                                    WAIT_FOR_REQUESTS_MS,  // Waiting for already running requests to complete
                                    TimeUnit.MILLISECONDS
                            );
                        } else {
                            promise.complete(false);
                        }
                        return promise;
                    })
                    .collect(Collectors.toList());
            CompletableFuture<Boolean> promise = new CompletableFuture<>();
            allOf(futures.toArray(new CompletableFuture[channelCount]))
                    .thenRun(() -> {
                        boolean shutdownResult = futures
                                .stream()
                                .allMatch(future -> {
                                    try {
                                        return future.get();
                                    } catch (Exception e) {
                                        return false;
                                    }
                                });
                        promise.complete(shutdownResult);
                    });
            return promise;
        } finally {
            executor.shutdown();
            try {
                // WAIT_FOR_REQUESTS_MS to wait for already running requests to complete
                // + shutdownTimeoutMs to wait for channels to shutdown
                // + WAIT_FOR_EXECUTOR_SHUTDOWN_MS to wait for scheduled executor to shutdown
                boolean closed = executor.awaitTermination(
                        WAIT_FOR_REQUESTS_MS + shutdownTimeoutMs + WAIT_FOR_EXECUTOR_SHUTDOWN_MS,
                        TimeUnit.SECONDS);
                if (!closed) {
                    logger.warn("scheduled executor termination timeout exceeded");
                }
            } catch (InterruptedException e) {
                logger.warn("scheduled executor termination interrupted", e);
            }
        }
    }

    public CompletableFuture<Boolean> removeChannels(List<String> endpointsToRemove, long shutdownTimeoutMs) {
        if (endpointsToRemove == null || endpointsToRemove.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        logger.debug("removing {} channels from pool: {}", endpointsToRemove.size(), endpointsToRemove);
        Stream<GrpcChannel> removedChannels = endpointsToRemove.stream().map(channels::remove);
        return shutdownChannels(removedChannels, endpointsToRemove.size(), shutdownTimeoutMs);
    }

    public void shutdown(long shutdownTimeoutMs) {
        logger.debug("initiating grpc pool shutdown with {} channels...", channels.size());
        Collection<GrpcChannel> channelsToShutdown = channels.values();
        boolean shutDownResult =
                shutdownChannels(channelsToShutdown.stream(), channelsToShutdown.size(), shutdownTimeoutMs)
                        .join();
        if (shutDownResult) {
            logger.debug("grpc pool was shut down successfully");
        } else {
            logger.warn("grpc pool was not shut down properly");
        }
    }
}
