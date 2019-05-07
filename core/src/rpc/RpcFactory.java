package ru.yandex.ydb.core.rpc;

import javax.annotation.Nullable;


/**
 * @author Sergey Polovko
 */
public interface RpcFactory<R extends Rpc> {

    @Nullable
    R create(RpcTransport transport);

}
