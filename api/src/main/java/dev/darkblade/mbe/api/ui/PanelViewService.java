package dev.darkblade.mbe.api.ui;

import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PanelViewService {
    void registerPanel(PanelId id, PanelDefinition panel);

    Optional<PanelDefinition> getPanel(PanelId id);

    Map<PanelId, PanelDefinition> getAllPanels();

    default boolean unregisterPanel(PanelId id) {
        return false;
    }

    default void registerPanel(String id, PanelDefinition panel) {
        registerPanel(PanelId.of(id), panel);
    }

    default Optional<PanelDefinition> getPanel(String id) {
        return getPanel(PanelId.of(id));
    }

    default boolean unregisterPanel(String id) {
        return unregisterPanel(PanelId.of(id));
    }

    default boolean panelExists(String panelId) {
        return getPanel(panelId).isPresent();
    }

    default List<String> getRegisteredPanelIds() {
        return getAllPanels().keySet().stream().map(PanelId::value).sorted().toList();
    }

    void openPanel(Player player, String panelId);
}
