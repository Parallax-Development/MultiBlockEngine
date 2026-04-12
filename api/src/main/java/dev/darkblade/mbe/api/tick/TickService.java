package dev.darkblade.mbe.api.tick;

public interface TickService {
    boolean register(Tickable tickable);

    boolean unregister(Tickable tickable);
}
