package dev.darkblade.mbe.api.wiring.debug;

@FunctionalInterface
public interface NetworkInspectContributor {

    void contribute(NetworkSnapshot snapshot, NetworkInspectView view);
}

