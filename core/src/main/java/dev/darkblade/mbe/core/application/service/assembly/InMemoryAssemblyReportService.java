package dev.darkblade.mbe.core.application.service.assembly;

import dev.darkblade.mbe.api.assembly.AssemblyReport;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAssemblyReportService implements AssemblyReportService {

    private final Map<UUID, AssemblyReport> reports = new ConcurrentHashMap<>();

    @Override
    public String getServiceId() {
        return "mbe:assembly.reports";
    }

    @Override
    public void store(UUID playerId, AssemblyReport report) {
        if (playerId == null || report == null) {
            return;
        }
        reports.put(playerId, report);
    }

    @Override
    public Optional<AssemblyReport> get(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(reports.get(playerId));
    }
}
