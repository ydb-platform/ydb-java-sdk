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
            return new GrpcChannel(endpoint, channelFactory);
        });
    }

    private CompletableFuture<Boolean> shutdownChannels(Collection<GrpcChannel> channelsToShutdown) {
        if (channelsToShutdown.isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        logger.debug("shutdown {} channels", channelsToShutdown.size());
        return CompletableFuture.supplyAsync(() -> {
            int closed = 0;
            for (GrpcChannel channel : channelsToShutdown) {
                if (Thread.currentThread().isInterrupted()) {
                    return false;
                }
                if (channel.shutdown()) {
                    closed++;
                }
            }
            return closed == channelsToShutdown.size();
        }, executor);
    }

    public CompletableFuture<Boolean> removeChannels(Collection<EndpointRecord> endpointsToRemove) {
        if (endpointsToRemove == null || endpointsToRemove.isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        logger.debug("removing {} endpoints from pool: {}", endpointsToRemove.size(), endpointsToRemove);
        List<GrpcChannel> channelsToShutdown = endpointsToRemove.stream()
                .map(EndpointRecord::getHostAndPort)
                .map(channels::remove)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return shutdownChannels(channelsToShutdown);
    }

    public CompletableFuture<Boolean> shutdown() {
        logger.debug("initiating grpc pool shutdown with {} channels...", channels.size());
        return shutdownChannels(channels.values()).whenComplete((res, th) -> {
            if (res != null && res) {
                logger.debug("grpc pool was shutdown successfully");
            } else {
                logger.warn("grpc pool was not shutdown properly");
            }
        });
    }

    @VisibleForTesting
    Map<String, GrpcChannel> getChannels() {
        return channels;
    }
}
