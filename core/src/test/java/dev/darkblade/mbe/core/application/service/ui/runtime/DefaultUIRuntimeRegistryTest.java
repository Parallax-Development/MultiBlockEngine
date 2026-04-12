package dev.darkblade.mbe.core.application.service.ui.runtime;

import dev.darkblade.mbe.api.ui.runtime.PanelDefinition;
import dev.darkblade.mbe.api.ui.runtime.PanelId;
import dev.darkblade.mbe.api.ui.runtime.PanelLayout;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultUIRuntimeRegistryTest {

    @Test
    void registersAndReadsPanelsFromSingleSource() {
        DefaultUIRuntimeRegistry registry = new DefaultUIRuntimeRegistry();
        PanelId id = PanelId.of("crafting_station");
        PanelDefinition panel = new PanelDefinition("crafting_station", new PanelLayout(Map.of("title", "Crafting")), Map.of());

        registry.registerPanel(id, panel);

        assertTrue(registry.getPanel(id).isPresent());
        assertEquals(1, registry.getAllPanels().size());
        assertEquals("crafting_station", registry.getAllPanels().keySet().iterator().next().value());
    }

    @Test
    void rejectsDuplicateRegistrationForSamePanel() {
        DefaultUIRuntimeRegistry registry = new DefaultUIRuntimeRegistry();
        PanelId id = PanelId.of("furnace_panel");
        PanelDefinition panel = new PanelDefinition("furnace_panel", new PanelLayout(Map.of()), Map.of());

        registry.registerPanel(id, panel);

        assertThrows(IllegalStateException.class, () -> registry.registerPanel(id, panel));
    }

    @Test
    void unregistersRegisteredPanel() {
        DefaultUIRuntimeRegistry registry = new DefaultUIRuntimeRegistry();
        PanelId id = PanelId.of("furnace_panel");
        PanelDefinition panel = new PanelDefinition("furnace_panel", new PanelLayout(Map.of()), Map.of());
        registry.registerPanel(id, panel);

        assertTrue(registry.unregisterPanel(id));
        assertFalse(registry.getPanel(id).isPresent());
    }
}
