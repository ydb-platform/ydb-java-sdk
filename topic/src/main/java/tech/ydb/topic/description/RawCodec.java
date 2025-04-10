package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RawCodec implements TopicCodec{

    @Override
    public OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getId() {
        return 0;
    }
}
