package tech.ydb.core.grpc;

/**
 * @author Nikolay Perfilov
 */
public class EndpointInfo {
    private final int nodeId;
    private final String endpoint;

    public EndpointInfo(int nodeId, String endpoint) {
        this.nodeId = nodeId;
        this.endpoint = endpoint;
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
