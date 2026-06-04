package dev.darkblade.mbe.api.service;

public interface ManagedRuntimeService {
    default void onLoad() {}
    default void onEnable() {}
    default void onDisable() {}
    default void onReload() {}
}
