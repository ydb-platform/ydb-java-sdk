package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TopicCodec {

    InputStream decode(ByteArrayInputStream byteArrayOutputStream) throws IOException;

    OutputStream encode(ByteArrayOutputStream byteArrayInputStream) throws IOException;

}
