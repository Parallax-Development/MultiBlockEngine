package dev.darkblade.mbe.api.electricity;

public interface EnergyStorage extends EnergyNode {
    long stored();

    long maxStored();

    long charge(long amount);

    long discharge(long amount);

    default long insert(long amount) {
        return charge(amount);
    }

    default long extract(long amount) {
        return discharge(amount);
    }

    default void onCharged(long amount) {
    }

    default void onDischarged(long amount) {
    }
}
