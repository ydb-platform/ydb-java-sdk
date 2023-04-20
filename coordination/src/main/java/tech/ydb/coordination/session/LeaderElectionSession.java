package tech.ydb.coordination.session;

import java.nio.charset.StandardCharsets;

import com.google.protobuf.ByteString;
import tech.ydb.coordination.CoordinationClient;
import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.settings.SessionSettings;
import tech.ydb.core.grpc.GrpcReadStream;

/**
 * @author Kirill Kurdyukov
 */
public class LeaderElectionSession {

    private static final int START_SESSION_ID = 0;
    private static final int SESSION_KEEP_ALIVE_TIMEOUT_MS = 5_000;
    private static final int LIMIT_TOKENS_SEMAPHORE = 1;
    private static final int COUNT_TOKENS = 1;

    private final CoordinationSession coordinationSession;
    private final SessionSettings session;
    private final String coordinationNodePath;
    private final String semaphoreName;

    public LeaderElectionSession(
            CoordinationClient coordinationClient,
            SessionSettings settings,
            String coordinationNodePath
    ) {
        this.coordinationSession = coordinationClient.createSession();
        this.session = settings;
        this.coordinationNodePath = coordinationNodePath;
        this.semaphoreName = "leader-election-" + session.getSessionNum();
    }

    public void startLeaderElection(
            String publishEndpoint,
            GrpcReadStream.Observer<String> observerOnEndpointLeader
    ) {
        coordinationSession.start(
                value -> {
                    if (value.hasDescribeSemaphoreChanged()) {
                        String endpointLeader = value.getDescribeSemaphoreResult()
                                .getSemaphoreDescription()
                                .getOwnersList()
                                .get(0)
                                .getData()
                                .toString(StandardCharsets.UTF_8);

                        observerOnEndpointLeader.onNext(endpointLeader);
                    }
                }
        );

        coordinationSession.sendStartSession(
                SessionRequest.SessionStart.newBuilder()
                        .setSessionId(START_SESSION_ID)
                        .setPath(coordinationNodePath)
                        .setDescription(session.getDescription())
                        .setTimeoutMillis(SESSION_KEEP_ALIVE_TIMEOUT_MS)
                        .build()
        );

        coordinationSession.sendCreateSemaphore(
                SessionRequest.CreateSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setLimit(LIMIT_TOKENS_SEMAPHORE)
                        .build()
        );

        coordinationSession.sendAcquireSemaphore(
                SessionRequest.AcquireSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setCount(COUNT_TOKENS)
                        .setData(ByteString.copyFrom(
                                publishEndpoint.getBytes(StandardCharsets.UTF_8)
                        ))
                        .build()
        );

        coordinationSession.sendDescribeSemaphore(
                SessionRequest.DescribeSemaphore.newBuilder()
                        .setName(semaphoreName)
                        .setWatchOwners(true)
                        .setIncludeOwners(true)
                        .build()
        );
    }
}
