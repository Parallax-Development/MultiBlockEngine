package dev.darkblade.mbe.api.service;

import java.util.List;
import java.util.Optional;

public interface ResolutionPolicy {
    <T> Optional<ServiceDescriptor<T>> select(List<ServiceDescriptor<T>> candidates, String ownerHint, ServiceScope requiredScope);
}
