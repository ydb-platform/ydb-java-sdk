package tech.ydb.coordination.session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.Status;
import tech.ydb.core.StatusCode;

/**
 * @author Kirill Kurdyukov
 */
public class ConfigurationPublishSession {


    public static final String CONFIGURATION_SEMAPHORE_PREFIX = "configuration-";

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPublishSession.class);

    private final CoordinationSession coordinationSession;
    private final ConcurrentHashMap<Long, CompletableFuture<Status>> reqIdToStatus = new ConcurrentHashMap<>();


    public ConfigurationPublishSession(
            CoordinationSession coordinationSession,
            SessionSettings settings,
            String coordinationNodePath
    ) {
        this.coordinationSession = coordinationSession;

        coordinationSession.start(
                value -> {
                    if (value.hasSessionStarted()) {
                        logger.info("Starting publish session!");
                    }

                    if (value.hasUpdateSemaphoreResult()) {
                        SessionResponse.UpdateSemaphoreResult result = value.getUpdateSemaphoreResult();

                        CompletableFuture<Status> statusCompletableFuture = reqIdToStatus.remove(result.getReqId());
                        if (statusCompletableFuture != null) {
                            statusCompletableFuture.complete(
                                    Status.of(StatusCode.fromProto(result.getStatus()))
                            );
                        }
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setSessionId(SessionSettings.START_SESSION_ID)
                        .setPath(coordinationNodePath)
                        .setDescription(settings.getDescription())
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(CONFIGURATION_SEMAPHORE_PREFIX + settings.getSessionNum())
                        .build()
        );
    }

    public CompletableFuture<Status> publishData(byte[] bytes) {
        Long reqId = ThreadLocalRandom.current().nextLong();
        CompletableFuture<Status> resultFuture = new CompletableFuture<>();

        reqIdToStatus.put(reqId, resultFuture);

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .build()
        );

        return resultFuture;
    }
}
