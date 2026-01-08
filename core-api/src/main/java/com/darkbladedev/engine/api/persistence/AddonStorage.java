package com.darkbladedev.engine.api.persistence;

import com.darkbladedev.engine.api.addon.AddonContext;
import com.darkbladedev.engine.api.logging.AddonLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;

import java.util.Locale;
import java.util.Objects;

public final class AddonStorage {

    public record Options(
        boolean strictValidation,
        boolean normalizeSegments,
        boolean logNormalization
    ) {
        public static Options defaults() {
            return new Options(false, true, false);
        }
    }

    private final PersistentStorageService persistence;
    private final String addonId;
    private final String namespaceId;
    private final Options options;
    private final AddonLogger logger;

    private AddonStorage(PersistentStorageService persistence, String addonId, String namespaceId, Options options, AddonLogger logger) {
        this.persistence = Objects.requireNonNull(persistence, "persistence");
        this.addonId = Objects.requireNonNull(addonId, "addonId");
        this.namespaceId = Objects.requireNonNull(namespaceId, "namespaceId");
        this.options = Objects.requireNonNull(options, "options");
        this.logger = logger;
    }

    public static AddonStorage from(PersistentStorageService persistence, AddonContext context) {
        return from(persistence, context, Options.defaults());
    }

    public static AddonStorage from(PersistentStorageService persistence, AddonContext context, Options options) {
        Objects.requireNonNull(persistence, "persistence");
        Objects.requireNonNull(context, "context");
        Options opts = options == null ? Options.defaults() : options;

        String id = Objects.requireNonNull(context.getAddonId(), "context.addonId").trim();
        if (id.isEmpty()) {
            throw new IllegalStateException("Addon id is blank");
        }

        String ns = normalizeSegment("namespace", id, opts, context.getLogger());
        return new AddonStorage(persistence, id, ns, opts, context.getLogger());
    }

    public PersistentStorageService persistence() {
        return persistence;
    }

    public String addonId() {
        return addonId;
    }

    public String namespaceId() {
        return namespaceId;
    }

    public StorageNamespace namespace() {
        return persistence.namespace(namespaceId);
    }

    public StorageDomain domain(String domainId) {
        String id = normalizeSegment("domain", domainId, options, logger);
        return new AddonDomain(id);
    }

    public AddonStorage withOptions(Options options) {
        Options next = options == null ? Options.defaults() : options;
        return new AddonStorage(persistence, addonId, namespaceId, next, logger);
    }

    private final class AddonDomain implements StorageDomain {
        private final String id;

        private AddonDomain(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public StorageStore store(String storeId, StorageSchema schema) {
            String sid = normalizeSegment("store", storeId, options, logger);
            Objects.requireNonNull(schema, "schema");
            return persistence.namespace(namespaceId).domain(id).store(sid, schema);
        }
    }

    private static String normalizeSegment(String kind, String raw, Options options, AddonLogger logger) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            if (options.strictValidation()) {
                throw new IllegalArgumentException("Storage " + kind + " is blank");
            }
            return "unknown";
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (options.strictValidation()) {
            if (!lower.equals(value)) {
                throw new IllegalArgumentException("Storage " + kind + " must be lowercase: " + value);
            }
            if (!isSafeSegment(lower)) {
                throw new IllegalArgumentException("Storage " + kind + " contains invalid characters: " + value);
            }
            return lower;
        }

        String normalized = options.normalizeSegments()
            ? normalizeLenient(lower)
            : lower;

        if (options.logNormalization() && logger != null && !normalized.equals(value)) {
            logger.log(LogLevel.DEBUG, "Storage segment normalized", null,
                LogKv.kv("kind", kind),
                LogKv.kv("from", value),
                LogKv.kv("to", normalized)
            );
        }

        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private static boolean isSafeSegment(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.contains("..")) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok =
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '_' || c == '-' || c == '.';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeLenient(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean ok =
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '_' || c == '-' || c == '.';
            out.append(ok ? c : '_');
        }

        String normalized = out.toString();
        while (normalized.contains("..")) {
            normalized = normalized.replace("..", "_");
        }
        return normalized;
    }
}
