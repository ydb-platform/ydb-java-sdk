package tech.ydb.topic.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import com.github.luben.zstd.ZstdOutputStream;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;

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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os;
        try {
            switch (codec) {
                case GZIP:
                    os = new GZIPOutputStream(baos);
                    break;
                case ZSTD:
                    os = new ZstdOutputStream(baos);
                    break;
                case LZOP:
                    LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
                    os = new LzoOutputStream(baos, lzoCompressor);
                    break;
                default:
                    throw new RuntimeException("Unsupported codec: " + codec);
            }
            os.write(input);
            os.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return baos.toByteArray();
    }
}
