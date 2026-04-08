package dev.darkblade.mbe.platform.bukkit.preview.api;

public record DisplayBlockState(String namespacedId, String serializedState) {
    public static DisplayBlockState of(String namespacedId, String serializedState) {
        String safeId = namespacedId == null ? "" : namespacedId.trim().toLowerCase(java.util.Locale.ROOT);
        String safeState = serializedState == null ? "" : serializedState.trim();
        return new DisplayBlockState(safeId, safeState);
    }
}
