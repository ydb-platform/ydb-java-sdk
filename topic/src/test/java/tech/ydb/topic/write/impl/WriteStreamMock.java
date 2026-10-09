package tech.ydb.topic.write.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import tech.ydb.core.Status;
import tech.ydb.core.grpc.GrpcReadStream;
import tech.ydb.core.grpc.GrpcReadWriteStream;
import tech.ydb.proto.StatusCodesProtos;
import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromClient;
import tech.ydb.proto.topic.YdbTopic.StreamWriteMessage.FromServer;

public class WriteStreamMock implements GrpcReadWriteStream<FromServer, FromClient> {
    private final CompletableFuture<Status> future = new CompletableFuture<>();
    final List<FromClient> messages = new ArrayList<>();
    GrpcReadStream.Observer<FromServer> observer = null;
    boolean isClosed = false;
    boolean isCanceled = false;

    void sendInitResponse(long lastSeqNo) {
        observer.onNext(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setInitResponse(YdbTopic.StreamWriteMessage.InitResponse.newBuilder()
                        .setLastSeqNo(lastSeqNo)
                        .setSessionId("test-session")
                        .build())
                .build());
    }

    void sendAckResponse(long seqNo, long offset) {
        observer.onNext(FromServer.newBuilder()
                .setStatus(StatusCodesProtos.StatusIds.StatusCode.SUCCESS)
                .setWriteResponse(YdbTopic.StreamWriteMessage.WriteResponse.newBuilder()
                        .addAcks(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.newBuilder()
                                .setSeqNo(seqNo)
                                .setWritten(YdbTopic.StreamWriteMessage.WriteResponse.WriteAck.Written.newBuilder()
                                        .setOffset(offset)
                                        .build())
                                .build())
                        .build())
                .build()
        );
    }

    void close(Status status) {
        future.complete(status);
    }

    @Override
    public String authToken() {
        return "token";
    }

    @Override
    public void sendNext(FromClient message) {
        messages.add(message);
    }

    @Override
    public void close() {
        this.isClosed = true;
    }

    @Override
    public CompletableFuture<Status> start(GrpcReadStream.Observer<FromServer> observer) {
        this.observer = observer;
        return future;
    }

    @Override
    public void cancel() {
        this.isCanceled = true;
    }
}
