package tech.ydb.topic.description;

import com.github.luben.zstd.ZstdInputStreamNoFinalizer;
import com.github.luben.zstd.ZstdOutputStreamNoFinalizer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZctdCodec implements TopicCodec {

    @Override
    public OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        return new ZstdOutputStreamNoFinalizer(byteArrayOutputStream);
    }

    @Override
    public InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException {
        return new ZstdInputStreamNoFinalizer(byteArrayInputStream);
    }

    @Override
    public int getId() {
        return 0;
    }
}
