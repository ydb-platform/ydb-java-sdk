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
    private static final int BLOCK_SIZE = 256 * 1024;
    private static final int HEADER_SIZE = 38;
    private static final int BLOCK_OVERHEAD = 8;
    private static final int END_MARKER_SIZE = 4;

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
    public String toString() {
        return "LzopCodec[org.anarres.lzo]";
    }

    @Override
    public int getId() {
        return Codec.LZOP;
    }

    @Override
    public long getMaxEncodedSize(int inputSizeBytes) {
        long size = inputSizeBytes;
        // LzopOutputStream header and end marker, plus two length fields for each non-empty block
        long blockCount = (size + BLOCK_SIZE - 1) / BLOCK_SIZE;
        return size + HEADER_SIZE + blockCount * BLOCK_OVERHEAD + END_MARKER_SIZE;
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
