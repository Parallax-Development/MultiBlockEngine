package dev.darkblade.mbe.api.io;

import dev.darkblade.mbe.api.service.MBEService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.Collection;

public interface IOService extends MBEService {

    Collection<IOPort> getPorts(MultiblockInstance instance);

    void registerPort(IOPort port);

    void unregisterPort(IOPort port);

    TransferResult transfer(IOPort from, IOPort to, IOPayload payload);
}
