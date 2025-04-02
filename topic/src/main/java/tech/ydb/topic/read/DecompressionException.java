package tech.ydb.topic.read;

import tech.ydb.topic.description.Codec;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Nikolay Perfilov
 */
public class DecompressionException extends UncheckedIOException {
    private static final long serialVersionUID = 2720187645859527813L;

    private final byte[] rawData;
    private final Codec codec;
    public DecompressionException(String message, IOException cause, byte[] rawData, Codec codec) {
        super(message, cause);
        this.rawData = rawData;
        this.codec = codec;
    }

    /**
     * @return Raw message byte data that failed be decompressed
     */
    public byte[] getRawData() {
        return rawData;
    }

    /**
     * @return Codec of message byte data
     */
    public Codec getCodec() {
        return codec;
    }
}
