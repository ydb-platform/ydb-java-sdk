package tech.ydb.core.impl.pool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Nikolay Perfilov
 */
public class GrpcChannelPool {
    private static final Logger logger = LoggerFactory.getLogger(GrpcChannelPool.class);

    private final Map<String, GrpcChannel> channels = new ConcurrentHashMap<>();
    private final ManagedChannelFactory channelFactory;
    private final ScheduledExecutorService executor;

    public GrpcChannelPool(ManagedChannelFactory channelFactory, ScheduledExecutorService executor) {
        this.channelFactory = channelFactory;
        this.executor = executor;
    }

    public GrpcChannel getChannel(EndpointRecord endpoint) {
        // Workaround for https://bugs.openjdk.java.net/browse/JDK-8161372 to prevent unnecessary locks in Java 8
        // Was fixed in Java 9+
        GrpcChannel result = channels.get(endpoint.getHostAndPort());

        return result != null ? result : channels.computeIfAbsent(endpoint.getHostAndPort(), (key) -> {
            logger.debug("channel " + endpoint.getHostAndPort() + " was not found in pool, creating one...");
            return new GrpcChannel(endpoint, channelFactory, true);
        });
    }

    private CompletableFuture<Boolean> shutdownChannels(Collection<GrpcChannel> channelsToShutdown) {
        if (channelsToShutdown.isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        logger.debug("shutdown {} channels", channelsToShutdown.size());
        List<CompletableFuture<Boolean>> removed = channelsToShutdown.stream()
                .map(channel -> CompletableFuture.supplyAsync(channel::shutdown, executor))
                .collect(Collectors.toList());

        return CompletableFuture
                .allOf(removed.toArray(new CompletableFuture<?>[0]))
                .thenApply((res) -> {
                    // all shutdown futures are completed here, we can just count failed
                    return removed.stream().allMatch(CompletableFuture::join);
                });
    }

    public boolean removeChannels(Collection<EndpointRecord> endpointsToRemove) {
        if (endpointsToRemove == null || endpointsToRemove.isEmpty()) {
            return true;
        }

        logger.debug("removing {} endpoints from pool: {}", endpointsToRemove.size(), endpointsToRemove);
        List<GrpcChannel> channelsToShutdown = endpointsToRemove.stream()
                .map(EndpointRecord::getHostAndPort)
                .map(channels::remove)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return shutdownChannels(channelsToShutdown).join();
    }

    public void shutdown() {
        logger.debug("initiating grpc pool shutdown with {} channels...", channels.size());
        boolean shutDownResult = shutdownChannels(channels.values()).join();

        if (shutDownResult) {
            logger.debug("grpc pool was shutdown successfully");
        } else {
            logger.warn("grpc pool was not shutdown properly");
        }
    }

    @VisibleForTesting
    Map<String, GrpcChannel> getChannels() {
        return channels;
    }
}
