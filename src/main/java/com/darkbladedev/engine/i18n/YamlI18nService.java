package com.darkbladedev.engine.i18n;

import com.darkbladedev.engine.api.i18n.I18nService;
import com.darkbladedev.engine.api.i18n.LocaleProvider;
import com.darkbladedev.engine.api.i18n.MessageKey;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LogPhase;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class YamlI18nService implements I18nService {

    public static final String CORE_ORIGIN = "mbe";
    public static final String LANG_DIR = "lang";
    public static final String DEFAULT_LOCALE_KEY = "en_us";

    private record BundleKey(String origin, String localeKey) {
        private BundleKey {
            origin = origin == null ? "unknown" : origin;
            localeKey = localeKey == null ? DEFAULT_LOCALE_KEY : localeKey;
        }
    }

    public record I18nSource(String origin, File dataFolder) {
        public I18nSource {
            origin = MessageKey.normalizeOrigin(origin);
        }
    }

    private final File coreDataFolder;
    private final Supplier<List<I18nSource>> sources;
    private final CoreLogger log;
    private final LocaleProvider localeProvider;
    private final BooleanSupplier debugMissingKeys;

    private final AtomicReference<Map<BundleKey, Map<String, MessageTemplate>>> bundles = new AtomicReference<>(Map.of());
    private final ConcurrentHashMap<String, List<String>> localeFallbackCache = new ConcurrentHashMap<>();

    public YamlI18nService(File coreDataFolder, Supplier<List<I18nSource>> sources, CoreLogger log, LocaleProvider localeProvider, BooleanSupplier debugMissingKeys) {
        this.coreDataFolder = Objects.requireNonNull(coreDataFolder, "coreDataFolder");
        this.sources = Objects.requireNonNull(sources, "sources");
        this.log = Objects.requireNonNull(log, "log");
        this.localeProvider = Objects.requireNonNull(localeProvider, "localeProvider");
        this.debugMissingKeys = debugMissingKeys == null ? () -> false : debugMissingKeys;
        reload();
    }

    @Override
    public LocaleProvider localeProvider() {
        return localeProvider;
    }

    @Override
    public String resolve(MessageKey key, Locale locale) {
        return resolve(key, locale, Map.of());
    }

    @Override
    public String resolve(MessageKey key, Locale locale, Map<String, ?> params) {
        String safeFallback = safeFallback(key, debugMissingKeys.getAsBoolean());
        try {
            if (key == null) {
                return safeFallback;
            }

            String origin = MessageKey.normalizeOrigin(key.origin());
            String path = MessageKey.normalizePath(key.path());
            String localeKey = LocaleParsing.toLocaleKey(localeProvider == null ? locale : (locale == null ? localeProvider.fallbackLocale() : locale));

            Map<BundleKey, Map<String, MessageTemplate>> snap = bundles.get();
            List<String> candidates = localeFallbackCache.computeIfAbsent(localeKey, this::buildLocaleFallback);

            Locale safeLocale = localeProvider == null ? locale : (locale == null ? localeProvider.fallbackLocale() : locale);

            for (String cand : candidates) {
                MessageTemplate t = findTemplate(snap, origin, cand, path, safeLocale, params);
                if (t != null) {
                    return safeRender(t, safeLocale, params);
                }
            }
            return safeFallback;
        } catch (Throwable t) {
            return safeFallback;
        }
    }

    @Override
    public void reload() {
        try {
            Map<BundleKey, Map<String, MessageTemplate>> next = new HashMap<>();

            loadOriginBundles(next, CORE_ORIGIN, coreDataFolder);

            for (I18nSource src : safeSources()) {
                if (src == null) continue;
                loadOriginBundles(next, src.origin(), src.dataFolder());
            }

            bundles.set(Map.copyOf(next));
            localeFallbackCache.clear();
        } catch (Throwable t) {
            bundles.set(Map.of());
            localeFallbackCache.clear();
        }
    }

    private List<I18nSource> safeSources() {
        try {
            List<I18nSource> list = sources.get();
            return list == null ? List.of() : list;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private void loadOriginBundles(Map<BundleKey, Map<String, MessageTemplate>> out, String origin, File dataFolder) {
        try {
            if (dataFolder == null) {
                return;
            }
            File langDir = new File(dataFolder, LANG_DIR);
            if (!langDir.exists() || !langDir.isDirectory()) {
                return;
            }

            File[] children = langDir.listFiles();
            if (children == null || children.length == 0) return;
            Arrays.sort(children, (a, b) -> {
                String an = a == null ? "" : a.getName();
                String bn = b == null ? "" : b.getName();
                return an.compareToIgnoreCase(bn);
            });

            for (File child : children) {
                if (child == null) continue;
                if (child.isDirectory()) {
                    String localeKey = normalizeLocaleFileName(child.getName());
                    if (!localeKey.isBlank()) {
                        loadLocaleDirectory(out, origin, localeKey, child);
                    }
                    continue;
                }

                if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    loadLocaleFile(out, origin, child);
                }
            }
        } catch (Throwable t) {
            safeLog(LogLevel.WARN, "i18n load failed", LogKv.kv("origin", origin));
        }
    }

    private void loadLocaleDirectory(Map<BundleKey, Map<String, MessageTemplate>> out, String origin, String localeKey, File dir) {
        try {
            if (dir == null || !dir.isDirectory()) return;
            File[] children = dir.listFiles();
            if (children == null || children.length == 0) return;
            Arrays.sort(children, (a, b) -> {
                String ap = a == null ? "" : a.getPath();
                String bp = b == null ? "" : b.getPath();
                return ap.compareToIgnoreCase(bp);
            });

            for (File child : children) {
                if (child == null) continue;
                if (child.isDirectory()) {
                    loadLocaleDirectory(out, origin, localeKey, child);
                    continue;
                }
                if (child.isFile() && child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                    loadLocaleFile(out, origin, localeKey, child);
                }
            }
        } catch (Throwable t) {
            safeLog(LogLevel.WARN, "i18n locale dir failed", LogKv.kv("origin", origin), LogKv.kv("locale", localeKey));
        }
    }

    private void loadLocaleFile(Map<BundleKey, Map<String, MessageTemplate>> out, String origin, File file) {
        try {
            if (file == null || !file.isFile()) {
                return;
            }
            String localeKey = normalizeLocaleFileName(file.getName());
            if (localeKey.isBlank()) {
                return;
            }
            loadLocaleFile(out, origin, localeKey, file);
        } catch (Throwable t) {
            safeLog(LogLevel.WARN, "i18n locale file failed", LogKv.kv("origin", origin), LogKv.kv("file", file == null ? "null" : file.getName()));
        }
    }

    private void loadLocaleFile(Map<BundleKey, Map<String, MessageTemplate>> out, String origin, String localeKey, File file) {
        try {
            if (file == null || !file.isFile()) {
                return;
            }
            if (localeKey == null || localeKey.isBlank()) {
                return;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = yaml.getKeys(true);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            Map<String, MessageTemplate> templates = new HashMap<>();
            for (String k : keys) {
                if (k == null || k.isBlank()) {
                    continue;
                }
                if (!yaml.isString(k)) {
                    continue;
                }
                String v = yaml.getString(k, "");
                templates.put(k, MessageTemplate.compile(v == null ? "" : v));
            }
            if (templates.isEmpty()) {
                return;
            }

            BundleKey bk = new BundleKey(MessageKey.normalizeOrigin(origin), localeKey);
            Map<String, MessageTemplate> prev = out.get(bk);
            if (prev == null || prev.isEmpty()) {
                out.put(bk, Map.copyOf(templates));
                return;
            }

            Map<String, MessageTemplate> merged = new HashMap<>(prev);
            merged.putAll(templates);
            out.put(bk, Map.copyOf(merged));
        } catch (Throwable t) {
            safeLog(LogLevel.WARN, "i18n locale file failed", LogKv.kv("origin", origin), LogKv.kv("file", file == null ? "null" : file.getName()));
        }
    }

    private static String normalizeLocaleFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        String v = fileName.trim();
        if (v.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            v = v.substring(0, v.length() - 4);
        }
        v = v.replace('-', '_');
        v = v.toLowerCase(Locale.ROOT);
        return v;
    }

    private MessageTemplate findTemplate(Map<BundleKey, Map<String, MessageTemplate>> snap, String origin, String localeKey, String path, Locale locale, Map<String, ?> params) {
        Map<String, MessageTemplate> byKey = snap.get(new BundleKey(origin, localeKey));
        MessageTemplate t = selectTemplate(byKey, path, locale, params);
        if (t != null) {
            return t;
        }
        if (!CORE_ORIGIN.equals(origin)) {
            Map<String, MessageTemplate> core = snap.get(new BundleKey(CORE_ORIGIN, localeKey));
            return selectTemplate(core, path, locale, params);
        }
        return null;
    }

    private static MessageTemplate selectTemplate(Map<String, MessageTemplate> byKey, String path, Locale locale, Map<String, ?> params) {
        if (byKey == null || byKey.isEmpty()) {
            return null;
        }
        if (path == null || path.isBlank()) {
            return null;
        }

        MessageTemplate direct = byKey.get(path);
        if (direct != null) {
            return direct;
        }

        Number count = extractCount(params);
        if (count == null) {
            return null;
        }

        String cat = pluralCategory(locale, count);
        MessageTemplate plural = byKey.get(path + "." + cat);
        if (plural != null) {
            return plural;
        }
        return byKey.get(path + ".other");
    }

    private static Number extractCount(Map<String, ?> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        Object v = params.get("count");
        if (v instanceof Number n) {
            return n;
        }
        Object n = params.get("n");
        if (n instanceof Number nn) {
            return nn;
        }
        return null;
    }

    private static String pluralCategory(Locale locale, Number count) {
        try {
            if (count == null) {
                return "other";
            }

            double abs = Math.abs(count.doubleValue());
            long i = (long) abs;
            boolean isInt = abs == i;

            String lang = locale == null ? "" : locale.getLanguage();
            String l = lang == null ? "" : lang.toLowerCase(Locale.ROOT);

            if (!isInt) {
                return "other";
            }

            return switch (l) {
                case "fr" -> (i == 0 || i == 1) ? "one" : "other";
                case "ru", "uk", "be" -> {
                    long mod10 = i % 10;
                    long mod100 = i % 100;
                    if (mod10 == 1 && mod100 != 11) yield "one";
                    if (mod10 >= 2 && mod10 <= 4 && !(mod100 >= 12 && mod100 <= 14)) yield "few";
                    if (mod10 == 0 || (mod10 >= 5 && mod10 <= 9) || (mod100 >= 11 && mod100 <= 14)) yield "many";
                    yield "other";
                }
                case "pl" -> {
                    long mod10 = i % 10;
                    long mod100 = i % 100;
                    if (i == 1) yield "one";
                    if (mod10 >= 2 && mod10 <= 4 && !(mod100 >= 12 && mod100 <= 14)) yield "few";
                    if (mod10 == 0 || mod10 == 1 || (mod10 >= 5 && mod10 <= 9) || (mod100 >= 12 && mod100 <= 14)) yield "many";
                    yield "other";
                }
                case "cs", "sk" -> {
                    if (i == 1) yield "one";
                    if (i >= 2 && i <= 4) yield "few";
                    yield "other";
                }
                default -> i == 1 ? "one" : "other";
            };
        } catch (Throwable t) {
            return "other";
        }
    }

    private List<String> buildLocaleFallback(String localeKey) {
        try {
            Set<String> out = new LinkedHashSet<>();
            String v = localeKey == null ? "" : localeKey.trim().toLowerCase(Locale.ROOT);
            if (!v.isBlank()) {
                out.add(v);
                int idx = v.indexOf('_');
                if (idx > 0) {
                    out.add(v.substring(0, idx));
                }
            }
            out.add(DEFAULT_LOCALE_KEY);
            out.add("en");
            return List.copyOf(out);
        } catch (Throwable t) {
            return List.of(DEFAULT_LOCALE_KEY);
        }
    }

    private static String safeRender(MessageTemplate t, Locale locale, Map<String, ?> params) {
        try {
            return t == null ? "" : t.render(locale, params);
        } catch (Throwable ex) {
            return "";
        }
    }

    private static String safeFallback(MessageKey key, boolean debugMissingKeys) {
        try {
            if (key == null) {
                return "";
            }
            String origin = MessageKey.normalizeOrigin(key.origin());
            String path = MessageKey.normalizePath(key.path());
            String v = origin + ":" + path;
            return debugMissingKeys ? ("??" + v + "??") : v;
        } catch (Throwable t) {
            return "";
        }
    }

    private void safeLog(LogLevel level, String msg, LogKv... fields) {
        try {
            log.logInternal(new com.darkbladedev.engine.api.logging.LogScope.Core(), LogPhase.RUNTIME, level, msg, null, fields, Set.of("i18n"));
        } catch (Throwable ignored) {
        }
    }
}
