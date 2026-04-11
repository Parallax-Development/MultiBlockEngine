package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolRegistry;
import dev.darkblade.mbe.api.tool.ToolState;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;

public final class PdcToolStateResolver implements ToolStateResolver {

    private static final NamespacedKey TOOL_ID_KEY = Objects.requireNonNull(NamespacedKey.fromString("mbe:tool_id"));
    private static final NamespacedKey TOOL_MODE_KEY = Objects.requireNonNull(NamespacedKey.fromString("mbe:tool_mode"));

    private final ItemStackBridge itemStackBridge;
    private final ToolRegistry toolRegistry;

    public PdcToolStateResolver(ItemStackBridge itemStackBridge, ToolRegistry toolRegistry) {
        this.itemStackBridge = Objects.requireNonNull(itemStackBridge, "itemStackBridge");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
    }

    @Override
    public ToolState resolve(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String pdcToolId = "";
        String pdcModeId = "";
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdcToolId = normalize(pdc.get(TOOL_ID_KEY, PersistentDataType.STRING));
            pdcModeId = normalize(pdc.get(TOOL_MODE_KEY, PersistentDataType.STRING));
        }
        String toolId = pdcToolId;
        if (toolId.isBlank()) {
            toolId = inferToolId(item);
        }
        if (toolId.isBlank()) {
            return null;
        }
        Tool tool = toolRegistry.get(toolId);
        if (tool == null) {
            return null;
        }
        String modeId = pdcModeId;
        String requestedMode = modeId;
        boolean unsupportedMode = requestedMode.isBlank() || tool.modes().stream().noneMatch(mode -> normalize(mode.id()).equals(requestedMode));
        if (unsupportedMode) {
            modeId = normalize(tool.defaultMode());
            save(item, new ToolState(toolId, modeId));
        }
        return new ToolState(toolId, modeId);
    }

    @Override
    public void save(ItemStack item, ToolState state) {
        if (item == null || state == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(TOOL_ID_KEY, PersistentDataType.STRING, normalize(state.toolId()));
        pdc.set(TOOL_MODE_KEY, PersistentDataType.STRING, normalize(state.modeId()));
        item.setItemMeta(meta);
    }

    private String inferToolId(ItemStack item) {
        ItemInstance instance;
        try {
            instance = itemStackBridge.fromItemStack(item);
        } catch (Throwable t) {
            return "";
        }
        if (instance == null || instance.definition() == null || instance.definition().key() == null || instance.definition().key().id() == null) {
            return "";
        }
        String key = normalize(instance.definition().key().id().key());
        if (key.isBlank()) {
            return "";
        }
        Tool tool = toolRegistry.get(key);
        if (tool == null) {
            return "";
        }
        return key;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
