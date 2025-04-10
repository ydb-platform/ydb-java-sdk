package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TopicCodec {

    OutputStream decode(ByteArrayOutputStream byteArrayOutputStream) throws IOException;

    InputStream encode(ByteArrayInputStream byteArrayInputStream) throws IOException;

    int getId();

}
