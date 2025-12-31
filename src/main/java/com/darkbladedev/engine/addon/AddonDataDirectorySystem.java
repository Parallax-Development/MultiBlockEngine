package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import com.darkbladedev.engine.api.logging.LogScope;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class AddonDataDirectorySystem {

    private final CoreLogger log;
    private final Path addonsRoot;

    public AddonDataDirectorySystem(CoreLogger log, Path addonsRoot) {
        this.log = Objects.requireNonNull(log, "log");
        this.addonsRoot = addonsRoot;
    }

    public Path getAddonsRoot() {
        return addonsRoot;
    }

    public Path ensureRootDirectory() throws IOException {
        Files.createDirectories(addonsRoot);

        if (!Files.isDirectory(addonsRoot)) {
            throw new IOException("Addons root is not a directory: " + addonsRoot);
        }

        if (!Files.isReadable(addonsRoot) || !Files.isWritable(addonsRoot)) {
            throw new AccessDeniedException(addonsRoot.toString());
        }

        if (Files.isSymbolicLink(addonsRoot)) {
            throw new IOException("Addons root cannot be a symlink: " + addonsRoot);
        }

        return addonsRoot;
    }

    public Path ensureAddonDataFolder(String addonId) throws IOException {
        ensureRootDirectory();

        String folderName = normalizeAddonFolderName(addonId);
        Path candidate = addonsRoot.resolve(folderName).normalize();

        Path rootReal = addonsRoot.toRealPath();
        if (!candidate.startsWith(addonsRoot)) {
            throw new IOException("Resolved addon folder escapes root. Root=" + addonsRoot + " Candidate=" + candidate);
        }

        ensureNoSymlinksWithinRoot(addonsRoot, candidate);
        Files.createDirectories(candidate);

        if (!Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Addon data folder is not a directory: " + candidate);
        }

        if (!Files.isReadable(candidate) || !Files.isWritable(candidate)) {
            throw new AccessDeniedException(candidate.toString());
        }

        Path candidateReal = candidate.toRealPath();
        if (!candidateReal.startsWith(rootReal)) {
            throw new IOException("Addon folder resolves outside root. Root=" + rootReal + " Real=" + candidateReal);
        }

        return candidate;
    }

    public void logFs(String addonId, String phase, Path path, Throwable cause, String action) {
        LogPhase p = switch (phase == null ? "" : phase.trim().toUpperCase(Locale.ROOT)) {
            case "LOAD" -> LogPhase.LOAD;
            case "ENABLE" -> LogPhase.ENABLE;
            case "DISABLE" -> LogPhase.DISABLE;
            default -> LogPhase.RUNTIME;
        };

        log.logInternal(new LogScope.Addon(addonId == null ? "unknown" : addonId, "unknown"), p, LogLevel.WARN, "Addon FS", cause, new LogKv[] {
            LogKv.kv("path", path == null ? "" : path.toString()),
            LogKv.kv("action", action),
            LogKv.kv("cause", formatCause(cause))
        }, Set.of());
    }

    public static String normalizeAddonFolderName(String addonId) {
        if (addonId == null) {
            throw new IllegalArgumentException("addonId");
        }

        String normalized = addonId.toLowerCase(Locale.ROOT).replace(':', '-');
        if (!normalized.matches("[a-z0-9][a-z0-9_\\-]*")) {
            throw new IllegalArgumentException("Invalid addon folder name derived from id: " + addonId);
        }

        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Invalid addon folder name: " + normalized);
        }

        return normalized;
    }

    private static void ensureNoSymlinksWithinRoot(Path root, Path candidate) throws IOException {
        Path relative = root.relativize(candidate);
        Path current = root;

        for (Path segment : relative) {
            current = current.resolve(segment);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) {
                throw new IOException("Symlink detected within addon scope: " + current);
            }
        }
    }

    private static String formatCause(Throwable cause) {
        if (cause == null) {
            return "n/a";
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName() + ": " + message;
    }
}
