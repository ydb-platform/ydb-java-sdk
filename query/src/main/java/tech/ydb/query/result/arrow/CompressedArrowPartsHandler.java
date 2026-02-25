package tech.ydb.query.result.arrow;

import io.grpc.ExperimentalApi;
import org.apache.arrow.compression.CommonsCompressionFactory;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorLoader;
import org.apache.arrow.vector.VectorSchemaRoot;

/**
 *
 * @author Aleksandr Gorshenin
 */
@ExperimentalApi("ApacheArrow support is experimental and API may change without notice")
public abstract class CompressedArrowPartsHandler extends ArrowPartsHandler {
    public CompressedArrowPartsHandler(RootAllocator allocator) {
        super(allocator);
    }

    @Override
    protected VectorLoader createLoader(VectorSchemaRoot vsr) {
        return new VectorLoader(vsr, CommonsCompressionFactory.INSTANCE);
    }
}
