package tech.ydb.query.result.arrow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;

import com.google.protobuf.ByteString;
import io.grpc.ExperimentalApi;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ReadChannel;
import org.apache.arrow.vector.ipc.message.ArrowRecordBatch;
import org.apache.arrow.vector.ipc.message.MessageSerializer;
import org.apache.arrow.vector.types.pojo.Schema;

import tech.ydb.proto.ValueProtos;
import tech.ydb.query.QueryStream;
import tech.ydb.query.result.QueryResultPart;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public abstract class ApacheArrowPartsHandler implements QueryStream.PartsHandler {
    private final RootAllocator allocator;

    public ApacheArrowPartsHandler(RootAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public void onNextRawPart(long index, ValueProtos.ResultSet rs) {
        if (!rs.hasArrowFormatMeta()) {
            // use the standard behaviour
            onNextPart(new QueryResultPart(index, rs));
            return;
        }

        try {
            Schema schema = readApacheArrowSchema(rs.getArrowFormatMeta().getSchema());
            try (VectorSchemaRoot vsr = VectorSchemaRoot.create(schema, allocator)) {
                loadApacheArrowVector(vsr, rs.getData());
                onNextPart(new ApacheArrowQueryResultPart(index, vsr, rs.getColumnsList(), rs.getTruncated()));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read ApacheArrow vector", ex);
        }
    }

    protected VectorLoader createLoader(VectorSchemaRoot vsr) {
        return new VectorLoader(vsr);
    }

    private void loadApacheArrowVector(VectorSchemaRoot vsr, ByteString bytes) throws IOException {
        try (InputStream is = bytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
                try (ArrowRecordBatch batch = MessageSerializer.deserializeRecordBatch(channel, allocator)) {
                    VectorLoader loader = createLoader(vsr);
                    loader.load(batch);
                }
            }
        }
    }

    private static Schema readApacheArrowSchema(ByteString bytes) throws IOException {
        try (InputStream is = bytes.newInput()) {
            try (ReadChannel channel = new ReadChannel(Channels.newChannel(is))) {
                return MessageSerializer.deserializeSchema(channel);
            }
        }
    }
}
