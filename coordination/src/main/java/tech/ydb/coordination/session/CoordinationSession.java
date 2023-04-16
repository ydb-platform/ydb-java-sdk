package tech.ydb.coordination.session;

import java.util.concurrent.CompletableFuture;

import tech.ydb.coordination.SessionRequest;
import tech.ydb.coordination.SessionResponse;
import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;

/**
 * @author Kirill Kurdyukov
 */
public class CoordinationSession {

    private final String path;
    private final GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream;

    public CoordinationSession(
            String path,
            GrpcReadWriteStream<SessionResponse, SessionRequest> coordinationStream
    ) {
        this.path = path;
        this.coordinationStream = coordinationStream;
    }

//    public CompletableFuture<Status> startSession() {
//        coordinationStream.start(
//                value -> {
//                }
//        );
//        coordinationStream.sendNext(
//                SessionRequest.newBuilder()
//                        .setSessionStart(
//                                SessionRequest.SessionStart.newBuilder()
//                                        .setSessionId(0)
//                                        .setTimeoutMillis(0)
//                                        .setPath(path)
//                                        .build()
//                        ).build()
//        );
//    }
}
