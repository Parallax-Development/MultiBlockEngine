package dev.darkblade.mbe.api.ui;

import java.util.Optional;

/**
 * Service registry that allows addons to programmatically declare which
 * UI panel should open for a specific multiblock type.
 */
public interface PanelMappingRegistry {
    /**
     * Registers a mapping between a multiblock ID and a panel ID.
     *
     * @param multiblockId The fully qualified multiblock ID (e.g. "mbe-electrics:coal_generator")
     * @param panelId      The target panel ID (e.g. "coal_generator")
     */
    void registerMapping(String multiblockId, String panelId);

    /**
     * Retrieves the panel ID mapped to the given multiblock ID, if any.
     *
     * @param multiblockId The fully qualified multiblock ID
     * @return An Optional containing the panel ID if mapped
     */
    Optional<String> getMapping(String multiblockId);
}
