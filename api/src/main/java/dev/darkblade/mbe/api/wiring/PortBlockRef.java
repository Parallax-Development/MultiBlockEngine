package dev.darkblade.mbe.api.wiring;

public sealed interface PortBlockRef permits PortBlockRef.Controller, PortBlockRef.Offset {

    record Controller() implements PortBlockRef {
    }

    record Offset(int dx, int dy, int dz) implements PortBlockRef {
    }
}

