package dev.darkblade.mbe.api.io;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

public interface IOTickService extends MBEService {

    void tick(MultiblockInstance instance);
}
