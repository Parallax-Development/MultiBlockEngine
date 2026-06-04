package dev.darkblade.mbe.core.application.service.addon.domain;
import dev.darkblade.mbe.core.application.service.addon.AddonMetadata;
import java.io.File;

public record DiscoveredAddon(File file, AddonMetadata metadata) {}
