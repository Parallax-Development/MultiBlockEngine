package dev.darkblade.mbe.uiengine;

import java.util.List;
import java.util.Map;

public record InventoryViewDefinition(
        String id,
        String title,
        int size,
        List<String> layout,
        Map<Character, InventoryItemDefinition> items,
        List<DynamicSection> dynamicSections,
        Map<Integer, SlotRole> slotRoles
) {
    public InventoryViewDefinition {
        id = id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
        title = title == null ? "" : title;
        size = normalizeSize(size);
        layout = layout == null ? List.of() : List.copyOf(layout);
        items = items == null ? Map.of() : Map.copyOf(items);
        dynamicSections = dynamicSections == null ? List.of() : List.copyOf(dynamicSections);
        slotRoles = slotRoles == null ? Map.of() : Map.copyOf(slotRoles);
    }

    private static int normalizeSize(int raw) {
        int clamped = Math.max(9, Math.min(54, raw));
        int mod = clamped % 9;
        return mod == 0 ? clamped : clamped + (9 - mod);
    }
}

