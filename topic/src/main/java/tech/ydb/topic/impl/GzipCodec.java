package tech.ydb.topic.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import tech.ydb.topic.description.Codec;

/**
 * Compression codec which implements the GZIP algorithm
 */
public class GzipCodec implements Codec {

    private static final GzipCodec INSTANCE = new GzipCodec();

    private GzipCodec() {
    }

    @Override
    public String toString() {
        return "GzipCodec[java.util.zip]";
    }

    /**
     * Get single instance
     * @return single instance of RawCodec
     */
    public static GzipCodec getInstance() {
        return INSTANCE;
    }

    @Override
    public int getId() {
        return Codec.GZIP;
    }

    @Override
    public long getMaxEncodedSize(int inputSizeBytes) {
        long size = inputSizeBytes;
        // zlib's compressBound formula plus the GZIP header and trailer
        return size + (size >>> 12) + (size >>> 14) + (size >>> 25) + 31;
    }

    @Override
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return new GZIPInputStream(byteArrayInputStream);
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
        return new GZIPOutputStream(byteArrayOutputStream);
    }
}
