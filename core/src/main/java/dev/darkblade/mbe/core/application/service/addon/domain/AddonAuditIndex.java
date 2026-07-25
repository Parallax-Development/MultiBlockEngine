package dev.darkblade.mbe.core.application.service.addon.domain;
import java.util.List;
import java.util.Set;
public record AddonAuditIndex(
        String addonId,
        String fileName,
        String mainClass,
        String rootPrefixInternal,
        List<String> classEntries,
        Set<String> classInternalNames,
        Set<String> apiClasses,
        Set<String> apiContractClasses,
        Set<String> embeddedCoreApiClasses,
        Set<String> embeddedJars,
        Set<String> requiredCapabilities) {}
