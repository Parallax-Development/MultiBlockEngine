package dev.darkblade.mbe.preview;

import java.util.concurrent.atomic.AtomicInteger;

public final class EntityIdAllocator {
    private final AtomicInteger counter = new AtomicInteger(2_000_000);

    public int nextId() {
        int id = counter.incrementAndGet();
        if (id < 0) {
            counter.set(2_000_000);
            return counter.incrementAndGet();
        }
        return id;
    }
}
