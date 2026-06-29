package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;

import tech.ydb.topic.description.Codec;

/**
 * Compression codec which implements the ZSTD algorithm
 */
public class ZstdBackwardCodec implements Codec {

    private static final ZstdBackwardCodec INSTANCE = new ZstdBackwardCodec();

    private ZstdBackwardCodec() {
    }

    /**
     * Get single instance
     * @return single instance of RawCodec
     */
    public static ZstdBackwardCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "ZstdBackwardCodec[com.github.luben.zstd]";
    }

    @Override
    public int getId() {
        return Codec.ZSTD;
    }

    @Override
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return new ZstdInputStream(byteArrayInputStream);
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
        return new ZstdOutputStream(byteArrayOutputStream);
    }
}
