package dev.darkblade.mbe.api.service.lifecycle;

import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import org.jetbrains.annotations.Nullable;

public interface MultiblockLifecycleService {

    /**
     * Attempts to disassemble/destroy a multiblock cleanly.
     * Handles cancellation events, action execution, memory cleanup, and limit unregistering.
     *
     * @param instance The multiblock instance to destroy.
     * @param actor The player who caused the destruction (can be null).
     * @return true if the multiblock was successfully destroyed, false if the event was cancelled.
     */
    boolean tryDisassemble(MultiblockInstance instance, @Nullable MBEPlayer actor);
}
