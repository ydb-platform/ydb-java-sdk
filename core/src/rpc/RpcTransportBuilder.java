package ru.yandex.ydb.core.rpc;

import java.util.concurrent.ExecutorService;


/**
 * @author Sergey Polovko
 */
@SuppressWarnings("unchecked")
public abstract class RpcTransportBuilder<T extends RpcTransport, B extends RpcTransportBuilder<T, B>> {

    protected ExecutorService executorService;

    public B withExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return (B) this;
    }

    public abstract T build();
}
