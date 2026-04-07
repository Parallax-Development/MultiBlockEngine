package dev.darkblade.mbe.core.application.service.assembly;

import dev.darkblade.mbe.api.assembly.AssemblyReport;
import dev.darkblade.mbe.api.service.MBEService;

import java.util.Optional;
import java.util.UUID;

public interface AssemblyReportService extends MBEService {

    void store(UUID playerId, AssemblyReport report);

    Optional<AssemblyReport> get(UUID playerId);
}
