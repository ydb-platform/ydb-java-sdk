package tech.ydb.topic.description;

import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoCompressor;
import org.anarres.lzo.LzoLibrary;
import org.anarres.lzo.LzoOutputStream;
import org.anarres.lzo.LzopInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LzopCodec implements TopicCodec{

    @Override
    public OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        LzoCompressor lzoCompressor = LzoLibrary.getInstance().newCompressor(LzoAlgorithm.LZO1X, null);
        return new LzoOutputStream(byteArrayOutputStream, lzoCompressor);
    }

    @Override
    public InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException {
        return new LzopInputStream(byteArrayInputStream);
    }

    @Override
    public int getId() {
        return 0;
    }
}
