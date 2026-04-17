package dev.darkblade.mbe.api.compat;

public interface ServerVersionService {
    ServerVersion current();

    default boolean isAtLeast(int major, int minor, int patch) {
        return current().isAtLeast(new ServerVersion(major, minor, patch));
    }
}
