package tech.ydb.core.impl.pool;

import java.util.Objects;

/**
 * @author Nikolay Perfilov
 */
public class EndpointRecord {
    private final String host;
    private final String hostAndPort;
    private final String locationDC;
    private final String sslNameOverride;
    private final int port;
    private final int nodeId;

    public EndpointRecord(String host, int port, int nodeId, String locationDC, String sslNameOverride) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.hostAndPort = host + ":" + port;
        this.nodeId = nodeId;
        this.locationDC = locationDC;
        if (sslNameOverride != null && !sslNameOverride.isEmpty()) {
            this.sslNameOverride = sslNameOverride;
        } else {
            this.sslNameOverride = null;
        }
    }

    public EndpointRecord(String host, int port) {
        this(host, port, 0, null, null);
    }

    public String getHost() {
        return host;
    }

    public String getSslNameOverride() {
        return sslNameOverride;
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

    public String getLocation() {
        return locationDC;
    }

    @Override
    public String toString() {
        return "Endpoint{host=" + host + ", port=" + port + ", node=" + nodeId +
            ", location=" + locationDC + ", sslNameOverride=" + sslNameOverride + "}";
    }
}
