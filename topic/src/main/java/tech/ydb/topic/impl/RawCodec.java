package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import tech.ydb.topic.description.Codec;

/**
 * Default codec which don't do any encode and decode.
 *
 */
public class RawCodec implements Codec {
    private static final RawCodec INSTANCE = new RawCodec();

    private RawCodec() {
    }

    /**
     * Get single instance
     * @return single instance of RawCodec
     */
    public static RawCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public int getId() {
        return Codec.RAW;
    }

    @Override
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return byteArrayInputStream;
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
        return byteArrayOutputStream;
    }
}
