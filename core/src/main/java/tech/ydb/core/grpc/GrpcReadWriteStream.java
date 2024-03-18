package tech.ydb.core.grpc;


/**
 *
 * @author Aleksandr Gorshenin
 * @param <R> type of message received
 * @param <W> type of message to be sent to the server
 */
public interface GrpcReadWriteStream<R, W> extends GrpcReadStream<R> {
    String authToken();

    /**
     * Send a request message to the server. May be called zero or more times depending on how many
     * messages the server is willing to accept for the operation.
     *
     * @param message message to be sent to the server.
     */
    void sendNext(W message);

    /**
     * Close the call for next message sending. Incoming response messages are unaffected.  This
     * should be called when no more messages will be sent from the client.
     */
    void close();
}
