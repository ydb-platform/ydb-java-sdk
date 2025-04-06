package tech.ydb.topic.description;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public interface InputStreamDecoder {

    OutputStream decode(ByteArrayOutputStream byteArrayOutputStream);
}
