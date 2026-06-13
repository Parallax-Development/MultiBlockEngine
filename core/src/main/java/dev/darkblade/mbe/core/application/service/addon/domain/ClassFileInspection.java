package dev.darkblade.mbe.core.application.service.addon.domain;
import java.util.Set;
public record ClassFileInspection(String ownerInternalName, Set<String> referencedClassNames,
        Set<String> classAnnotationDescriptors) {}
