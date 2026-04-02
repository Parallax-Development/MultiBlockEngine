package dev.darkblade.mbe.api.wiring.debug;

import dev.darkblade.mbe.api.wiring.NetworkNode;

import java.util.Objects;

@FunctionalInterface
public interface NetworkDebugContributor {

    DebugSeverity classify(NetworkSnapshot snapshot, NetworkNode node);

    static DebugSeverity safeClassify(NetworkDebugContributor contributor, NetworkSnapshot snapshot, NetworkNode node) {
        Objects.requireNonNull(contributor, "contributor");
        try {
            DebugSeverity s = contributor.classify(snapshot, node);
            return s == null ? DebugSeverity.OK : s;
        } catch (Throwable ignored) {
            return DebugSeverity.OK;
        }
    }
}

