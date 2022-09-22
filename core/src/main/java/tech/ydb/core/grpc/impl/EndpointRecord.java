package tech.ydb.core.grpc.impl;

import java.util.Objects;

/**
 * @author Nikolay Perfilov
 */
public class EndpointRecord {
    private final String host;
    private final int port;
    private final String hostAndPort;
    private final int nodeId;

    public EndpointRecord(String host, int port, int nodeId) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.hostAndPort = host + ":" + port;
        this.nodeId = nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getHostAndPort() {
        return hostAndPort;
    }

    public int getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "Endpoint{host=" + host + ", port=" + port + ", node=" + nodeId + "}";
    }
}
