package dev.darkblade.mbe.api.wiring;

import java.util.UUID;

public interface NetworkConnection {
    UUID id();

    NetworkNode from();

    NetworkNode to();
}

