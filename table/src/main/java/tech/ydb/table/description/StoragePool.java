package tech.ydb.table.description;

public final class StoragePool {
    private final String media;

    public StoragePool(String media) {
        this.media = media;
    }

    public String getMedia() {
        return media;
    }
}
