package tech.ydb.query.result.arrow;

import io.grpc.ExperimentalApi;
import org.apache.arrow.compression.CommonsCompressionFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public abstract class ApacheArrowCompressedPartsHandler extends ApacheArrowPartsHandler {
    public ApacheArrowCompressedPartsHandler(BufferAllocator allocator) {
        super(allocator);
    }

    @Override
    protected VectorLoader createLoader(VectorSchemaRoot vsr) {
        return new VectorLoader(vsr, CommonsCompressionFactory.INSTANCE);
    }
}
