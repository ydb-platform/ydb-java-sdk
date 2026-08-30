package tech.ydb.topic.read.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;
import org.junit.Test;

import tech.ydb.proto.topic.YdbTopic;
import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.read.PartitionSession;
import tech.ydb.topic.read.events.DataReceivedEvent;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadPartitionSessionTest {
    /**
     * An empty batch is a server side anomaly, but it must not affect the batches that follow it in the
     * same ReadResponse.
     */
    @Test(timeout = 30_000)
    public void batchesAfterAnEmptyBatchAreStillRead() {
        ReadSession session = mock(ReadSession.class);
        when(session.getMaxBatchSize()).thenReturn(0);
        when(session.getMessageDecoder()).thenReturn(new MessageDecoder(1024, Runnable::run, new CodecRegistry()));

        PartitionSession partition = new PartitionSession(1, 1, "/topic");
        List<Long> delivered = Collections.synchronizedList(new ArrayList<>());

        ReadPartitionSession reader = new ReadPartitionSession("test", session, partition, directExecutor(), 0) {
            @Override
            public void handleDataReceivedEvent(DataReceivedEvent event) {
                event.getMessages().forEach(message -> delivered.add(message.getOffset()));
            }
        };

        reader.addBatches(Arrays.asList(emptyProtoBatch(), rawProtoBatch(1), rawProtoBatch(2)));
        assertEquals(Arrays.asList(1L, 2L), delivered);
    }

    private static YdbTopic.StreamReadMessage.ReadResponse.Batch emptyProtoBatch() {
        return YdbTopic.StreamReadMessage.ReadResponse.Batch.newBuilder()
                .setCodec(Codec.RAW)
                .setProducerId("producer")
                .build();
    }

    private static YdbTopic.StreamReadMessage.ReadResponse.Batch rawProtoBatch(long offset) {
        return YdbTopic.StreamReadMessage.ReadResponse.Batch.newBuilder()
                .setCodec(Codec.RAW)
                .setProducerId("producer")
                .addMessageData(
                        YdbTopic.StreamReadMessage.ReadResponse.MessageData.newBuilder()
                                .setOffset(offset)
                                .setSeqNo(offset)
                                .setData(ByteString.copyFromUtf8("data"))
                                .build()
                )
                .build();
    }
}
