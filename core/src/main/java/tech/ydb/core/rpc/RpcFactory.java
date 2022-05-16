package tech.ydb.core.rpc;

import javax.annotation.Nullable;
import tech.ydb.core.grpc.GrpcTransport;


/**
 * @author Sergey Polovko
 */
public interface RpcFactory<R extends Rpc> {

    @Nullable
    R create(GrpcTransport transport);

}
