package dev.darkblade.mbe.core.application.service.addon.domain;
import java.util.List;
public record AddonInfo(
        String id,
        String version,
        AddonState state,
        List<String> serviceIds,
        List<String> dependencies) {}
