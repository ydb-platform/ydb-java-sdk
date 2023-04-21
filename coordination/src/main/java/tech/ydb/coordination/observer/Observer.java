package tech.ydb.coordination.observer;

import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.SessionResponse;

/**
 * @author Kirill Kurdyukov
 */
public interface Observer {

    void onNext(SessionResponse sessionResponse);

    default void onFailure(StatusCodesProtos.StatusIds.StatusCode statusCode) {
    }
}
