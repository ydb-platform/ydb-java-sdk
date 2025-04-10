package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipCode implements TopicCodec {

    @Override
    public OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        return new GZIPOutputStream(byteArrayOutputStream);
    }

    @Override
    public InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException {
        return new GZIPInputStream(byteArrayInputStream);
    }

    @Override
    public int getId() {
        return 0;
    }
}
