package tech.ydb.core.impl.priority;

import tech.ydb.core.utils.Timer;
import tech.ydb.discovery.DiscoveryProtos;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author Kurdyukov Kirill
 */
public class DetectLocalDCPriorityEndpointEvaluator implements PriorityEndpointEvaluator {

    private static final int TCP_PING_TIMEOUT = 5000;

    @Override
    public long evaluatePriority(
            String selfLocation,
            DiscoveryProtos.EndpointInfo endpointInfo
    ) {
        try (final Socket socket = new Socket()) {
            final long startConnection = Timer.nanoTime();

            socket.connect(
                    new InetSocketAddress(endpointInfo.getAddress(), endpointInfo.getPort()),
                    TCP_PING_TIMEOUT
            );

            final long stopConnection = Timer.nanoTime();

            return stopConnection - startConnection;
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}
