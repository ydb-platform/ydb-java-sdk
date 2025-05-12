package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzopInputStream;
import org.anarres.lzo.LzopOutputStream;

import tech.ydb.topic.description.Codec;

/**
 * Compression codec which implements the LZO algorithm
 */
public class LzopCodec implements Codec {

    private static final LzopCodec INSTANCE = new LzopCodec();

    private LzopCodec() {
    }

    /**
     * Get single instance
     * @return single instance of RawCodec
     */
    public static LzopCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public int getId() {
        return Codec.LZOP;
    }

    @Override
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return new LzopInputStream(byteArrayInputStream);
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream)  throws IOException {
        LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
        return new LzopOutputStream(byteArrayOutputStream, lzoCompressor);
    }
}
