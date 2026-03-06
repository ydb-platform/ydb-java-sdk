package tech.ydb.query.result.arrow;

import java.io.IOException;
import java.nio.channels.Channels;

import com.google.protobuf.ByteString;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.VectorUnloader;
import org.apache.arrow.vector.ipc.WriteChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import tech.ydb.proto.ValueProtos;
import tech.ydb.proto.formats.YdbFormats;
import tech.ydb.query.result.QueryResultPart;

/**
 *
 * @author Aleksandr Gorshenin
 */
public class ArrowPartsHandlerTest {
    private RootAllocator allocator;

    @Before
    public void init() {
        this.allocator = new RootAllocator();
    }

    @After
    public void clean() {
        this.allocator.close();
    }

    @Test
    public void wrongDataTest() throws IOException {
        ApacheArrowPartsHandler unexpected = new ApacheArrowPartsHandler(allocator) {
            @Override
            public void onNextPart(QueryResultPart part) {
                throw new AssertionError("Must not be called");
            }
        };

        ByteString testSchema;
        ByteString testData;

        IntVector col1 = new IntVector("col1", FieldType.nullable(new ArrowType.Int(32, true)), allocator);
        BigIntVector col2 = new BigIntVector("col2", FieldType.nullable(new ArrowType.Int(64, true)), allocator);

        try (VectorSchemaRoot vsr = VectorSchemaRoot.of(col1, col2)) {
            try (ByteString.Output out = ByteString.newOutput()) {
                try (WriteChannel channel = new WriteChannel(Channels.newChannel(out))) {
                    MessageSerializer.serialize(channel, vsr.getSchema());
                    testSchema = out.toByteString();
                }
            }

            try (ByteString.Output out = ByteString.newOutput()) {
                try (WriteChannel channel = new WriteChannel(Channels.newChannel(out))) {
                    VectorUnloader loader = new VectorUnloader(vsr);
                    try (ArrowRecordBatch batch = loader.getRecordBatch()) {
                        MessageSerializer.serialize(channel, batch);
                        testData = out.toByteString();
                    }
                }
            }
        }

        ValueProtos.ResultSet noSchema = ValueProtos.ResultSet.newBuilder()
                .setArrowFormatMeta(YdbFormats.ArrowFormatMeta.newBuilder().setSchema(ByteString.EMPTY))
                .setData(testData)
                .build();

        RuntimeException ex1 = Assert.assertThrows(RuntimeException.class, () -> unexpected.onNextRawPart(0, noSchema));
        Assert.assertEquals("Cannot read ApacheArrow vector", ex1.getMessage());

        ValueProtos.ResultSet noData = ValueProtos.ResultSet.newBuilder()
                .setArrowFormatMeta(YdbFormats.ArrowFormatMeta.newBuilder().setSchema(testSchema))
                .setData(ByteString.EMPTY)
                .build();

        RuntimeException ex2 = Assert.assertThrows(RuntimeException.class, () -> unexpected.onNextRawPart(0, noData));
        Assert.assertEquals("Cannot read ApacheArrow vector", ex2.getMessage());
    }
}
