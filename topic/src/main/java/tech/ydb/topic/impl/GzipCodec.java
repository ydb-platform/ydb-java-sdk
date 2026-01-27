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
    public InputStream decode(InputStream byteArrayInputStream) throws IOException {
        return new GZIPInputStream(byteArrayInputStream);
    }

    @Override
    public OutputStream encode(OutputStream byteArrayOutputStream) throws IOException {
        return new GZIPOutputStream(byteArrayOutputStream);
    }
}
