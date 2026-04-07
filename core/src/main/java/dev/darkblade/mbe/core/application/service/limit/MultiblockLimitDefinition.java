package dev.darkblade.mbe.core.application.service.limit;

public record MultiblockLimitDefinition(
        String permission,
        int max,
        LimitScope scope,
        String multiblockId
) {
}
