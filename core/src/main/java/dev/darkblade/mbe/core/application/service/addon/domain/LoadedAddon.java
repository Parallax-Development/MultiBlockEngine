package dev.darkblade.mbe.core.application.service.addon.domain;

import dev.darkblade.mbe.api.addon.MultiblockAddon;
import dev.darkblade.mbe.api.logging.AddonLogger;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.core.application.service.addon.AddonMetadata;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public record LoadedAddon(
        AddonMetadata metadata,
        MultiblockAddon addon,
        dev.darkblade.mbe.core.application.service.addon.AddonClassLoader classLoader,
        AddonLogger logger,
        AtomicReference<LogPhase> phase,
        Path dataFolder) {}
