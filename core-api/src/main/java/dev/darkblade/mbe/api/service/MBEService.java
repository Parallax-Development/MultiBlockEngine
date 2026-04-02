package dev.darkblade.mbe.api.service;

public interface MBEService {
    String getServiceId();

    default void onLoad() {
    }

    default void onEnable() {
    }

    default void onDisable() {
    }
}
