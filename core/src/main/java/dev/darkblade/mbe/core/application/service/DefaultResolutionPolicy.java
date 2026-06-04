package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.service.ResolutionPolicy;
import dev.darkblade.mbe.api.service.ServiceDescriptor;
import dev.darkblade.mbe.api.service.ServiceScope;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DefaultResolutionPolicy implements ResolutionPolicy {
    @Override
    public <T> Optional<ServiceDescriptor<T>> select(List<ServiceDescriptor<T>> candidates, String ownerHint, ServiceScope requiredScope) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .filter(c -> requiredScope == null || c.getScope() == requiredScope)
                .max(Comparator.comparingInt((ServiceDescriptor<T> c) -> c.getPriority())
                        .thenComparing(c -> ownerHint != null && ownerHint.equals(c.getOwnerAddonId()) ? 1 : 0));
    }
}
