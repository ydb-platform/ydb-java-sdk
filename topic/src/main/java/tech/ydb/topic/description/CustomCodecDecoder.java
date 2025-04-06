package tech.ydb.topic.description;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class CustomCodecDecoder {

    private CustomCodecDecoder() {
    }

    private final static CustomCodecDecoder instance = new CustomCodecDecoder();
    private volatile InputStreamDecoder inputStreamDecoder;

    public static CustomCodecDecoder getInstance() {
        return instance;
    }

    public void registerInputStreamDecoder(InputStreamDecoder inputStreamDecoder) {
        this.inputStreamDecoder = inputStreamDecoder;
    }

    public OutputStream getStream(ByteArrayOutputStream byteArrayOutputStream) {
        if (inputStreamDecoder == null) {
            throw new RuntimeException("Custom codec decoder not initialized");
        }

        return inputStreamDecoder.decode(byteArrayOutputStream);
    }
}
