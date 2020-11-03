package tech.ydb.core.rpc;

public interface OutStreamObserver<V> {

    void onNext(V value);

    void onError(Throwable t);

    void onCompleted();

}
