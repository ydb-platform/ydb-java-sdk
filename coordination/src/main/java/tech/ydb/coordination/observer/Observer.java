package tech.ydb.coordination.observer;

import tech.ydb.StatusCodesProtos;
import tech.ydb.coordination.SessionResponse;

public interface Observer {

    void onNext(SessionResponse sessionResponse);

    default void onFailure(Long sessionId, StatusCodesProtos.StatusIds.StatusCode statusCode) {
    }
}
