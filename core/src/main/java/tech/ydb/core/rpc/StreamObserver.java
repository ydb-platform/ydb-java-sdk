package tech.ydb.core.rpc;

import tech.ydb.core.Status;


/**
 * @author Sergey Polovko
 */
public interface StreamObserver<V> {

    void onNext(V value);

    void onError(Status status);

    void onCompleted();
}
