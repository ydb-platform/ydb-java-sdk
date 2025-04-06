package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public interface InputStreamEncoder {

   InputStream encode(ByteArrayInputStream byteArrayInputStream);
}
