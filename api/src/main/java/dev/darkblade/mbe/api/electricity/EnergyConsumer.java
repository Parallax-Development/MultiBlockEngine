package dev.darkblade.mbe.api.electricity;

public interface EnergyConsumer extends EnergyNode {
    long demandPerTick();

    default long consume(long available) {
        long a = Math.max(0L, available);
        long need = Math.max(0L, Math.min(demandPerTick(), capacity()));
        long taken = Math.min(a, need);
        onEnergyReceived(taken);
        return taken;
    }

    default void onEnergyReceived(long amount) {
    }
}
