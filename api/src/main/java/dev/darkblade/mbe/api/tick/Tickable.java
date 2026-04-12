package dev.darkblade.mbe.api.tick;

public interface Tickable {
    void tick();

    default int getTickInterval() {
        return 1;
    }
}
