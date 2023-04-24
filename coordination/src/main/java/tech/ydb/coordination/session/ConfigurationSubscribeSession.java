package tech.ydb.coordination.session;

import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationSubscribeSession {

    private final CoordinationSession coordinationSession;
    private final SessionSettings settings;
    private final String coordinationNodeName;
    private final String semaphoreName;

    public ConfigurationSubscribeSession(
        CoordinationSession coordinationSession,
        SessionSettings settings,
        String coordinationNodeName
    ) {
        this.coordinationSession = coordinationSession;
        this.settings = settings;
        this.coordinationNodeName = coordinationNodeName;
        this.semaphoreName = ConfigurationPublishSession.CONFIGURATION_SEMAPHORE_PREFIX + settings.getSessionNum();
    }

    public void describe(GrpcReadStream.Observer<byte []> onChangedData) {
        coordinationSession.start(
                value -> {
                    switch (value.getResponseCase()) {
                        case DESCRIBE_SEMAPHORE_CHANGED:
                            describeSemaphore();
                            break;
                        case DESCRIBE_SEMAPHORE_RESULT: {
                            onChangedData.onNext(
                                    value.getDescribeSemaphoreResult()
                                            .getSemaphoreDescription()
                                            .getData()
                                            .toByteArray()
                            );
                        }
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setPath(coordinationNodeName)
                        .setSessionId(SessionSettings.START_SESSION_ID)
                        .setDescription(settings.getDescription())
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .build()
        );

        describeSemaphore();
    }

    private void describeSemaphore() {
        coordinationSession.sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setWatchData(true)
                        .build()
        );
    }
}
