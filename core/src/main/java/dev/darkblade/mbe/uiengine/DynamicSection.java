package dev.darkblade.mbe.uiengine;

public record DynamicSection(
        char symbol,
        String providerId
) {
    public DynamicSection {
        providerId = providerId == null ? "" : providerId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
