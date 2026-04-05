package dev.darkblade.mbe.core.application.service.query;

import dev.darkblade.mbe.core.domain.MultiblockInstance;

import java.util.List;
import java.util.UUID;

public interface PlayerMultiblockQueryService {

    List<MultiblockInstance> getPlayerInstances(UUID playerId, String multiblockId);

    List<Object> getVariableValues(UUID playerId, String multiblockId, String varName);

    double aggregate(UUID playerId, String multiblockId, String varName, AggregationType type);

    int countInstances(UUID playerId, String multiblockId);
}
