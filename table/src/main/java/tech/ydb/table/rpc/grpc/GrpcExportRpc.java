package tech.ydb.table.rpc.grpc;

import tech.ydb.core.grpc.GrpcTransport;
import tech.ydb.table.rpc.ExportRpc;

/**
 * @author Kirill Kurdyukov
 */
public class GrpcExportRpc implements ExportRpc {

    private final GrpcTransport transport;


    public GrpcExportRpc(GrpcTransport transport) {
        this.transport = transport;
    }
}
