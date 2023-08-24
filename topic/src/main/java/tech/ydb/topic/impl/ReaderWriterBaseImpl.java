package tech.ydb.topic.impl;

import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import tech.ydb.core.grpc.GrpcStreamRetrier;

/**
 * @author Nikolay Perfilov
 */
public abstract class ReaderWriterBaseImpl<SessionType extends Session> extends GrpcStreamRetrier {

    protected final String id;
    protected String currentSessionId = "";
    protected SessionType session;

    protected ReaderWriterBaseImpl(ScheduledExecutorService scheduler) {
        super(scheduler);
        this.id = UUID.randomUUID().toString();
    }
    protected abstract void onSessionStop();
    protected abstract SessionType createNewSession();

    @Override
    protected void onStreamReconnect() {
        getLogger().info("[{}] Creating new {} stream session", id, getStreamName());
        session = createNewSession();
    }

    @Override
    protected void onStreamFinished() {
        getLogger().info("[{}] Stopping {} stream session {}", id, getStreamName(), currentSessionId);
        // This session is not working anymore
        session.stop();
        onSessionStop();
    }

    @Override
    protected void onShutdown(String reason) {
        getLogger().info("[{}] {} is shut down. Shutting down current stream session {}", id, getStreamName(),
                currentSessionId);
        session.shutdown();
    }

}
