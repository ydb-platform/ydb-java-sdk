package tech.ydb.topic.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.anarres.lzo.LzopInputStream;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.TopicCodec;

/**
 * @author Nikolay Perfilov
 */
public class Encoder {

    private Encoder() {
    }

    @Deprecated
    public static byte[] encode(int codec, byte[] input) throws IOException {
        return encode(codec, null, input);
    }

    public static byte[] encode(int codec, TopicCodec topic, byte[] input) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (OutputStream os = makeOutputStream(codec, topic, byteArrayOutputStream)) {
            os.write(input);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Deprecated
    public static byte[] decode(int codec, byte[] input) throws IOException {
        return decode(codec, null, input);
    }

    public static byte[] decode(int codec, TopicCodec topic, byte[] input) throws IOException {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
                InputStream is = makeInputStream(codec, topic, byteArrayInputStream)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static OutputStream makeOutputStream(int codec, TopicCodec topic,
                                                 ByteArrayOutputStream byteArrayOutputStream) throws IOException {
       if (codec > 10000 && topic != null) {
           return topic.encode(byteArrayOutputStream);
       }

        switch (codec) {
            case Codec.GZIP:
                return new GZIPOutputStream(byteArrayOutputStream);
            case Codec.ZSTD:
                return new ZstdOutputStreamNoFinalizer(byteArrayOutputStream);
            case Codec.LZOP:
                LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
                return new LzoOutputStream(byteArrayOutputStream, lzoCompressor);
            case Codec.CUSTOM:
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }

    private static InputStream makeInputStream(int codec, TopicCodec topic,
                                               ByteArrayInputStream byteArrayInputStream) throws IOException {
        if (codec > 10000 && topic != null) {
            return topic.decode(byteArrayInputStream);
        }

        switch (codec) {
            case Codec.GZIP:
                return new GZIPInputStream(byteArrayInputStream);
            case Codec.ZSTD:
                return new ZstdInputStreamNoFinalizer(byteArrayInputStream);
            case Codec.LZOP:
                return new LzopInputStream(byteArrayInputStream);
            case Codec.CUSTOM:
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }
}
