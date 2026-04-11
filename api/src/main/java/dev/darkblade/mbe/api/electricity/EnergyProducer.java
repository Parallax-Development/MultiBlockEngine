package dev.darkblade.mbe.api.electricity;

public interface EnergyProducer extends EnergyNode {
    long producePerTick();

    default long produce(long maxRequested) {
        long req = Math.max(0L, maxRequested);
        if (req <= 0L) {
            return 0L;
        }
        long out = Math.max(0L, Math.min(producePerTick(), capacity()));
        return Math.min(req, out);
    }

    default void onEnergyProduced(long amount) {
    }
}
