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
import tech.ydb.topic.description.CustomCodecDecoder;
import tech.ydb.topic.description.CustomCodecEncoder;

/**
 * @author Nikolay Perfilov
 */
public class Encoder {

    private Encoder() { }

    public static byte[] encode(Codec codec, byte[] input) throws IOException {
        if (codec == Codec.RAW) {
            return input;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (OutputStream os = makeOutputStream(codec, byteArrayOutputStream)) {
            os.write(input);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decode(Codec codec, byte[] input) throws IOException {
        if (codec == Codec.RAW) {
            return input;
        }

        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
            InputStream is = makeInputStream(codec, byteArrayInputStream)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    private static OutputStream makeOutputStream(Codec codec,
                                                 ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        switch (codec) {
            case GZIP:
                return new GZIPOutputStream(byteArrayOutputStream);
            case ZSTD:
                return new ZstdOutputStreamNoFinalizer(byteArrayOutputStream);
            case LZOP:
                LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
                return new LzoOutputStream(byteArrayOutputStream, lzoCompressor);
            case CUSTOM:
                return CustomCodecDecoder.getInstance().getStream(byteArrayOutputStream);
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }

    private static InputStream makeInputStream(Codec codec,
                                                 ByteArrayInputStream byteArrayInputStream) throws IOException {
        switch (codec) {
            case GZIP:
                return new GZIPInputStream(byteArrayInputStream);
            case ZSTD:
                return new ZstdInputStreamNoFinalizer(byteArrayInputStream);
            case LZOP:
                return new LzopInputStream(byteArrayInputStream);
            case CUSTOM:
                return CustomCodecEncoder.getInstance().getStream(byteArrayInputStream);
            default:
                throw new RuntimeException("Unsupported codec: " + codec);
        }
    }

}
