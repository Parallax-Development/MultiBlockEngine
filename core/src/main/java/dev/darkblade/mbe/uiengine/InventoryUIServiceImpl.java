package dev.darkblade.mbe.uiengine;

import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryUIServiceImpl implements InventoryUIService {
    private final InventoryConfigLoader loader;
    private final InventoryRenderer renderer;
    private final InventorySessionStore sessions;
    private final Map<String, InventoryViewDefinition> views = new ConcurrentHashMap<>();

    public InventoryUIServiceImpl(InventoryConfigLoader loader, InventoryRenderer renderer, InventorySessionStore sessions) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        reload();
    }

    public void reload() {
        views.clear();
        views.putAll(loader.load());
    }

    @Override
    public void open(Player player, String viewId) {
        if (player == null || viewId == null || viewId.isBlank()) {
            return;
        }
        InventoryViewDefinition view = views.get(viewId.trim().toLowerCase(Locale.ROOT));
        if (view == null) {
            return;
        }
        InventoryRenderer.RenderResult rendered = renderer.renderWithBindings(player, view);
        sessions.put(player, new PlayerInventorySession(view.id(), rendered.inventory(), rendered.bindings()));
        player.openInventory(rendered.inventory());
    }
}
