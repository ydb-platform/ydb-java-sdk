package tech.ydb.coordination.session;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 * @author Kirill Kurdyukov
 */
public class ServiceDiscoverySubscribeSession {

    private final CoordinationSession coordinationSession;
    private final String coordinationNameNode;
    private final SessionSettings settings;
    private final String semaphoreName;


    public ServiceDiscoverySubscribeSession(
            CoordinationSession coordinationSession,
            SessionSettings settings,
            String coordinationNameNode
    ) {
        this.coordinationSession = coordinationSession;
        this.coordinationNameNode = coordinationNameNode;
        this.semaphoreName = ServiceDiscoveryPublishSession.SERVICE_DISCOVERY_SEMAPHORE_PREFIX + settings.getSessionNum();
        this.settings = settings;
    }

    public void describe(GrpcReadStream.Observer<List<String>> onChangedData) {
        coordinationSession.start(
                value -> {
                    if (value.hasDescribeSemaphoreChanged()) {
                        describeSemaphore();
                    }

                    if (value.hasDescribeSemaphoreResult()) {
                        onChangedData.onNext(
                                value.getDescribeSemaphoreResult()
                                        .getSemaphoreDescription()
                                        .getOwnersList()
                                        .stream()
                                        .map(semaphoreSession -> semaphoreSession
                                                .getData()
                                                .toString(StandardCharsets.UTF_8))
                                        .collect(Collectors.toList()));
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setPath(coordinationNameNode)
                        .setSessionId(SessionSettings.START_SESSION_ID)
                        .setDescription(settings.getDescription())
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setLimit(Long.MAX_VALUE)
                        .build()
        );

        describeSemaphore();
    }

    private void describeSemaphore() {
        coordinationSession.sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setIncludeOwners(true)
                        .setWatchOwners(true)
                        .build()
        );
    }
}
