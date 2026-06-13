package dev.darkblade.mbe.core.application.service.addon.domain;
import java.nio.file.Path;
public record AddonRuntime(String id, ClassLoader classLoader, Path dataFolder) {
    public AddonRuntime {
        if (id == null || id.isBlank()) {
            id = "unknown";
        }
        if (classLoader == null) {
            classLoader = dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService.class.getClassLoader();
        }
        if (dataFolder == null) {
            dataFolder = Path.of(".").toAbsolutePath().normalize();
        }
    }
}
