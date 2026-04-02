package dev.darkblade.mbe.api.electricity;

import dev.darkblade.mbe.api.wiring.NetworkGraph;

public interface EnergyNetwork {
    NetworkGraph graph();

    long produced();

    long consumed();

    long stored();

    long starved();

    long overflowed();
}

