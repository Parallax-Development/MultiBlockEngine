package dev.darkblade.mbe.api.wiring.debug;

public interface NetworkDebugRegistry {

    void registerInspectContributor(NetworkInspectContributor contributor);

    void unregisterInspectContributor(NetworkInspectContributor contributor);

    void registerDebugContributor(NetworkDebugContributor contributor);

    void unregisterDebugContributor(NetworkDebugContributor contributor);
}
