package dev.darkblade.mbe.core.application.service.io;

import dev.darkblade.mbe.api.io.IOPort;
import dev.darkblade.mbe.api.io.IOService;
import dev.darkblade.mbe.api.io.IOTickService;
import dev.darkblade.mbe.api.io.IOType;
import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DefaultIOTickService implements IOTickService {

    private final IOService ioService;

    public DefaultIOTickService(IOService ioService) {
        this.ioService = Objects.requireNonNull(ioService, "ioService");
    }

    @Override
    public String getServiceId() {
        return "mbe:io.tick";
    }

    @Override
    public void tick(MultiblockInstance instance) {
        if (instance == null) {
            return;
        }
        List<IOPort> outputs = new ArrayList<>();
        List<IOPort> inputs = new ArrayList<>();
        for (IOPort port : ioService.getPorts(instance)) {
            if (port.getType() == IOType.OUTPUT || port.getType() == IOType.BOTH) {
                outputs.add(port);
            }
            if (port.getType() == IOType.INPUT || port.getType() == IOType.BOTH) {
                inputs.add(port);
            }
        }
        for (IOPort output : outputs) {
            for (IOPort input : inputs) {
                if (output == input) {
                    continue;
                }
                if (output.getChannel() != input.getChannel()) {
                    continue;
                }
                if (!Objects.equals(output.getNetworkId(), input.getNetworkId())) {
                    continue;
                }
                ioService.transfer(output, input, new SimpleIOPayload(output.getChannel(), null));
            }
        }
    }
}
