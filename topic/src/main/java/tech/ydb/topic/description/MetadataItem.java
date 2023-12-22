package tech.ydb.topic.description;

import javax.annotation.Nonnull;

/**
 * @author Nikolay Perfilov
 */
public class MetadataItem {
    private final String key;
    private final byte[] value;

    public MetadataItem(@Nonnull String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }
}
