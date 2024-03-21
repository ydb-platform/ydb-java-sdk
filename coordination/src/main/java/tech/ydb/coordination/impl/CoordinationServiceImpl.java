package tech.ydb.coordination.impl;

import javax.annotation.WillNotClose;

import tech.ydb.coordination.CoordinationClient;
import tech.ydb.core.grpc.GrpcTransport;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class CoordinationServiceImpl {
    private CoordinationServiceImpl() { }

    public static CoordinationClient newClient(@WillNotClose GrpcTransport transport) {
        Rpc rpc = new RpcImpl(transport);
        return new ClientImpl(rpc);
    }
}
