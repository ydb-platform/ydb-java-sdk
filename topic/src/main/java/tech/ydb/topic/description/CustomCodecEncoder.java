package tech.ydb.topic.description;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class CustomCodecEncoder {

    private CustomCodecEncoder() {
    }

    private final static CustomCodecEncoder instance = new CustomCodecEncoder();
    private volatile InputStreamEncoder inputStreamEncoder;

    public static CustomCodecEncoder getInstance() {
        return instance;
    }

    public void registerInputStreamEncoder(InputStreamEncoder inputStreamEncoder) {
        this.inputStreamEncoder = inputStreamEncoder;
    }

    public InputStream getStream(ByteArrayInputStream byteArrayInputStream) {
        if (inputStreamEncoder == null) {
            throw new RuntimeException("Custom codec encoder not initialized");
        }

        return inputStreamEncoder.encode(byteArrayInputStream);
    }
}
