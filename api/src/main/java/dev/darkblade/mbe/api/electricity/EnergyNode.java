package dev.darkblade.mbe.api.electricity;

import dev.darkblade.mbe.api.wiring.NetworkNode;

public interface EnergyNode {
    NetworkNode networkNode();

    long capacity();
}

