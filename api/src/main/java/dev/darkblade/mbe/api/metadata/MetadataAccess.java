package dev.darkblade.mbe.api.metadata;

public enum MetadataAccess {
    NONE,
    READ,
    WRITE,
    READ_WRITE;

    public boolean canRead() {
        return this == READ || this == READ_WRITE;
    }

    public boolean canWrite() {
        return this == WRITE || this == READ_WRITE;
    }
}
