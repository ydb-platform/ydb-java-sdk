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
import tech.ydb.topic.description.CodecRegister;
import tech.ydb.topic.description.GzipCode;
import tech.ydb.topic.description.RawCodec;
import tech.ydb.topic.description.TopicCodec;
import tech.ydb.topic.description.ZctdCodec;

/**
 * @author Nikolay Perfilov
 */
public class Encoder {

    private Encoder() {
    }

    public static byte[] encode(TopicCodec codec, byte[] input) throws IOException {
        if (codec instanceof RawCodec) {
            return input;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (OutputStream os = codec.decode(byteArrayOutputStream)) {
            os.write(input);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decode(TopicCodec codec, byte[] input) throws IOException {
        if (codec instanceof RawCodec) {
            return input;
        }

        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
                InputStream is = codec.encode(byteArrayInputStream)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

}
