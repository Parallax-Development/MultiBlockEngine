package dev.darkblade.mbe.core.application.service;

public interface ManagedCoreService {
    String getManagedCoreServiceId();

    default void onCoreLoad() {
    }

    default void onCoreEnable() {
    }

    default void onCoreDisable() {
    }
}
