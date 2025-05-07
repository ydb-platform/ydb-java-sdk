package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;

import tech.ydb.topic.description.Codec;

/**
 * Compression codec which implements the ZSTD algorithm
 */
public class ZstdCodec implements Codec {

    private static final ZstdCodec INSTANCE = new ZstdCodec();

    private ZstdCodec() {
    }

    /**
     * Get single instance
     * @return single instance of RawCodec
     */
    public static ZstdCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public int getId() {
        return Codec.ZSTD;
    }

    @Override
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return new ZstdInputStreamNoFinalizer(byteArrayInputStream);
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
        return new ZstdOutputStreamNoFinalizer(byteArrayOutputStream);
    }
}
