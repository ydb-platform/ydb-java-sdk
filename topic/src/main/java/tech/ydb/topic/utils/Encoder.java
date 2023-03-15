package tech.ydb.topic.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.anarres.lzo.LzopInputStream;

import tech.ydb.topic.description.Codec;

/**
 * @author Nikolay Perfilov
 */
public class Encoder {

    private Encoder() { }

    public static byte[] encode(Codec codec, byte[] input) {
        if (codec == Codec.RAW) {
            return input;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream os;
        try {
            switch (codec) {
                case GZIP:
                    os = new GZIPOutputStream(byteArrayOutputStream);
                    break;
                case ZSTD:
                    os = new ZstdOutputStream(byteArrayOutputStream);
                    break;
                case LZOP:
                    LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
                    os = new LzoOutputStream(byteArrayOutputStream, lzoCompressor);
                    break;
                case CUSTOM:
                default:
                    throw new RuntimeException("Unsupported codec: " + codec);
            }
            os.write(input);
            os.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decode(Codec codec, byte[] input) {
        if (codec == Codec.RAW) {
            return input;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(input);
            InputStream is;
            switch (codec) {
                case GZIP:
                    is = new GZIPInputStream(byteArrayInputStream);
                    break;
                case ZSTD:
                    is = new ZstdInputStream(byteArrayInputStream);
                    break;
                case LZOP:
                    is = new LzopInputStream(byteArrayInputStream);
                    break;
                case CUSTOM:
                default:
                    throw new RuntimeException("Unsupported codec: " + codec);
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            is.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return byteArrayOutputStream.toByteArray();
    }

}
