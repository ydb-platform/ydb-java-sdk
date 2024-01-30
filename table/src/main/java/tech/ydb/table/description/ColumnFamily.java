package tech.ydb.table.description;

public final class ColumnFamily {
    private final String name;
    private final StoragePool data;
    private final Compression compression;

    @Deprecated
    public ColumnFamily(String name, StoragePool data, Compression compression, boolean keepInMemory) {
        this.name = name;
        this.data = data;
        this.compression = compression;
//        this.keepInMemory = keepInMemory;
    }

    public ColumnFamily(String name, StoragePool data, Compression compression) {
        this.name = name;
        this.data = data;
        this.compression = compression;
    }

    public String getName() {
        return name;
    }

    public StoragePool getData() {
        return data;
    }

    public Compression getCompression() {
        return compression;
    }

    @Deprecated
    public boolean isKeepInMemory() {
        return false;
    }

    public enum Compression {
        COMPRESSION_NONE,
        COMPRESSION_LZ4
    }
}

