package dev.darkblade.mbe.api.wiring;

import java.util.Objects;

public record NetworkType(String id) {
    public static final NetworkType ENERGY = new NetworkType("energy");
    public static final NetworkType INFORMATION = new NetworkType("information");
    public static final NetworkType ITEM = new NetworkType("item");
    public static final NetworkType FLUID = new NetworkType("fluid");

    public NetworkType {
        Objects.requireNonNull(id, "NetworkType id cannot be null");
    }
}
