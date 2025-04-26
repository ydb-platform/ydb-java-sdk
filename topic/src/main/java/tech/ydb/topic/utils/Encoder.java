package tech.ydb.topic.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nonnull;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzopInputStream;
import org.anarres.lzo.LzopOutputStream;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;
import tech.ydb.topic.description.CustomTopicCodec;

/**
 * Class accumulated logic for encode and decode messages
 *
 * @author Nikolay Perfilov
 */
public class Encoder {

    private Encoder() {
    }

    /**
     * Encode messages
     *
     * @param codec codec identifier
     * @param input byte array of data to be encoded
     * @param codecRegistry contains custom codecs
     * @return encoded data
     * @throws IOException throws when error has happened
     */
    public static byte[] encode(int codec,
                                @Nonnull byte[] input,
                                @Nonnull CodecRegistry codecRegistry) throws IOException {
        if (codec == Codec.RAW) {
            return input;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (OutputStream os = makeOutputStream(codec, byteArrayOutputStream, codecRegistry)) {
            os.write(input);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Decode messages
     *
     * @param codec codec identifier
     * @param input byte array of data to be decoded
     * @param codecRegistry contains custom codecs
     * @return decoded data
     * @throws IOException throws when error has happened
     */
    public static byte[] decode(int codec,
                                @Nonnull byte[] input,
                                @Nonnull CodecRegistry codecRegistry) throws IOException {
        if (codec == Codec.RAW) {
            return input;
        }

        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
                InputStream is = makeInputStream(codec, byteArrayInputStream, codecRegistry)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static OutputStream makeOutputStream(int codec,
                                                 ByteArrayOutputStream byteArrayOutputStream,
                                                 CodecRegistry codecRegistry) throws IOException {
        CustomTopicCodec customTopicCodec;
        if (codec > 10000 && (customTopicCodec = codecRegistry.getCustomCodec(codec)) != null) {
            return customTopicCodec.encode(byteArrayOutputStream);
        }

        switch (codec) {
            case Codec.GZIP:
                return new GZIPOutputStream(byteArrayOutputStream);
            case Codec.LZOP:
                LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
                return new LzopOutputStream(byteArrayOutputStream, lzoCompressor);
            case Codec.ZSTD:
                return new ZstdOutputStreamNoFinalizer(byteArrayOutputStream);
            case Codec.CUSTOM:
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }

    private static InputStream makeInputStream(int codec,
                                               ByteArrayInputStream byteArrayInputStream,
                                               CodecRegistry codecRegistry) throws IOException {
        CustomTopicCodec customTopicCodec;
        if (codec > 10000 && (customTopicCodec = codecRegistry.getCustomCodec(codec)) != null) {
            return customTopicCodec.decode(byteArrayInputStream);
        }

        switch (codec) {
            case Codec.GZIP:
                return new GZIPInputStream(byteArrayInputStream);
            case Codec.LZOP:
                return new LzopInputStream(byteArrayInputStream);
            case Codec.ZSTD:
                return new ZstdInputStreamNoFinalizer(byteArrayInputStream);
            case Codec.CUSTOM:
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }
}
