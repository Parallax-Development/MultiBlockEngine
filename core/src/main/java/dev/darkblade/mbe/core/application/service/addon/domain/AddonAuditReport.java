package dev.darkblade.mbe.core.application.service.addon.domain;
import java.util.List;
import java.util.Set;
public record AddonAuditReport(
        String addonId,
        String fileName,
        Set<String> sharedApis,
        Set<String> declaredAddonRefs,
        Set<String> undeclaredAddonRefs,
        Set<String> nonApiAccessRefs,
        Set<String> embeddedJars,
        List<String> violations,
        boolean fatal) {}
