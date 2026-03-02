package tech.ydb.topic.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import tech.ydb.topic.description.Codec;
import tech.ydb.topic.description.CodecRegistry;

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

    private static OutputStream makeOutputStream(int codecId,
                                                 @Nonnull ByteArrayOutputStream byteArrayOutputStream,
                                                 @Nonnull CodecRegistry codecRegistry) throws IOException {
        Codec codec = getCodec(codecId, codecRegistry);

        return codec.encode(byteArrayOutputStream);
    }

    private static InputStream makeInputStream(int codecId,
                                               @Nonnull ByteArrayInputStream byteArrayInputStream,
                                               @Nonnull CodecRegistry codecRegistry) throws IOException {
        Codec codec = getCodec(codecId, codecRegistry);

        return codec.decode(byteArrayInputStream);
    }

    private static @Nonnull Codec getCodec(int codecId, @Nonnull CodecRegistry codecRegistry) {
        Codec codec = codecRegistry.getCodec(codecId);
        if (codec == null) {
            throw new RuntimeException("Unsupported codec: " + codecId);
        }
        return codec;
    }
}
