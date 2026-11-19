package tech.ydb.core.impl.pool;

import java.util.Objects;

/**
 * @author Nikolay Perfilov
 */
public class EndpointRecord {
    private final String host;
    private final String hostAndPort;
    private final String locationDC;
    private final String pile;
    private final String authority;
    private final int port;
    private final int nodeId;

    public EndpointRecord(String host, int port, int nodeId) {
        this(host, port, nodeId, null, null, null);
    }

    public EndpointRecord(String host, int port, int nodeId, String locationDC) {
        this(host, port, nodeId, locationDC, null, null);
    }

    public EndpointRecord(String host, int port, int nodeId, String locationDC, String pile, String authority) {
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.hostAndPort = host + ":" + port;
        this.nodeId = nodeId;
        this.pile = pile != null && !pile.isEmpty() ? pile : null;
        this.locationDC = locationDC;
        this.authority = authority != null && !authority.isEmpty() ? authority : null;
    }

    public EndpointRecord(String host, int port) {
        this(host, port, 0, null, null, null);
    }

    public String getHost() {
        return host;
    }

    public String getAuthority() {
        return authority;
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

    public String getPile() {
        return pile;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Endpoint{");
        sb.append("host=").append(host);
        sb.append(", port=").append(port);
        sb.append(", node=").append(nodeId);
        if (pile != null) {
            sb.append(", pile=").append(pile);
        }
        if (locationDC != null) {
            sb.append(", location=").append(locationDC);
        }
        if (authority != null) {
            sb.append(", overrideAuthority=").append(authority);
        }
        return sb.append("}").toString();
    }
}
