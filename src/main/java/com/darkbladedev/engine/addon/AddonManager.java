package com.darkbladedev.engine.addon;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.MultiblockAPI;
import com.darkbladedev.engine.api.addon.AddonException;
import com.darkbladedev.engine.api.addon.MultiblockAddon;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;

public class AddonManager {

    public enum AddonState {
        LOADED,
        ENABLED,
        DISABLED,
        FAILED
    }

    private record AddonDescriptor(
        String id,
        String version,
        String mainClass,
        int apiVersion,
        List<String> depends
    ) {}

    private record LoadedAddon(
        AddonDescriptor descriptor,
        MultiblockAddon addon,
        URLClassLoader classLoader,
        Logger logger
    ) {}

    private final MultiBlockEngine plugin;
    private final MultiblockAPI api;
    private final File addonFolder;
    private final Map<String, LoadedAddon> loadedAddons = new HashMap<>();
    private final Map<String, AddonState> states = new HashMap<>();
    private final ArrayDeque<String> enableOrder = new ArrayDeque<>();

    public AddonManager(MultiBlockEngine plugin, MultiblockAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.addonFolder = new File(plugin.getDataFolder(), "addons");
    }

    public void loadAddons() {
        if (!addonFolder.exists()) {
            addonFolder.mkdirs();
        }

        File[] files = addonFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null) return;

        for (File file : files) {
            try {
                loadAddon(file);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load addon from file: " + file.getName(), e);
            }
        }
    }

    public AddonState getState(String addonId) {
        return states.getOrDefault(addonId, AddonState.DISABLED);
    }

    public void failAddon(String addonId, AddonException.Phase phase, String message, Throwable cause, boolean fatal) {
        if (addonId == null || addonId.isBlank()) {
            addonId = "unknown";
        }

        LoadedAddon loaded = loadedAddons.get(addonId);
        String header = "[MultiBlockEngine][Addon:" + addonId + "][" + phase.name() + "] ";

        if (cause == null) {
            plugin.getLogger().severe(header + message);
        } else {
            plugin.getLogger().log(Level.SEVERE, header + message + " Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage(), cause);
        }

        boolean markFailed = fatal || phase == AddonException.Phase.LOAD || phase == AddonException.Phase.ENABLE;
        if (markFailed) {
            states.put(addonId, AddonState.FAILED);
        }

        if (fatal && loaded != null) {
            try {
                loaded.addon().onDisable();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, header + "Error during disable after failure", t);
            }
        }
    }

    private void loadAddon(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            AddonDescriptor descriptor = readDescriptor(jar, file.getName());
            if (descriptor == null) {
                return;
            }

            if (loadedAddons.containsKey(descriptor.id())) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Duplicate addon ID: " + descriptor.id(), null, true);
                return;
            }

            if (descriptor.apiVersion() != MultiBlockEngine.getApiVersion()) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Incompatible API version. Addon=" + descriptor.apiVersion() + " Engine=" + MultiBlockEngine.getApiVersion(), null, true);
                return;
            }

            URL[] urls = {file.toURI().toURL()};
            URLClassLoader loader = new URLClassLoader(urls, plugin.getClass().getClassLoader());
            Logger addonLogger = Logger.getLogger("MultiBlockEngine-Addon-" + descriptor.id());
            addonLogger.setParent(plugin.getLogger());

            MultiblockAddon addon;
            try {
                Class<?> clazz = loader.loadClass(descriptor.mainClass());
                if (!MultiblockAddon.class.isAssignableFrom(clazz)) {
                    failAddon(descriptor.id(), AddonException.Phase.LOAD, "Main class does not implement MultiblockAddon: " + descriptor.mainClass(), null, true);
                    close(loader);
                    return;
                }
                addon = (MultiblockAddon) clazz.getDeclaredConstructor().newInstance();
            } catch (Throwable t) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Failed to instantiate addon main class: " + descriptor.mainClass(), t, true);
                close(loader);
                return;
            }

            String reportedId;
            try {
                reportedId = Objects.requireNonNull(addon.getId(), "addon.getId()");
            } catch (Throwable t) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Addon getId() failed", t, true);
                close(loader);
                return;
            }

            if (!reportedId.equals(descriptor.id())) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Addon id mismatch. Descriptor=" + descriptor.id() + " getId()=" + reportedId, null, true);
                close(loader);
                return;
            }

            SimpleAddonContext context = new SimpleAddonContext(descriptor.id(), plugin, api, addonLogger);
            try {
                addon.onLoad(context);
            } catch (AddonException e) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, e.getMessage(), e.getCause(), e.isFatal());
                close(loader);
                return;
            } catch (Throwable t) {
                failAddon(descriptor.id(), AddonException.Phase.LOAD, "Unhandled exception during onLoad", t, true);
                close(loader);
                return;
            }

            loadedAddons.put(descriptor.id(), new LoadedAddon(descriptor, addon, loader, addonLogger));
            states.put(descriptor.id(), AddonState.LOADED);
            plugin.getLogger().info("[MultiBlockEngine][Addon:" + descriptor.id() + "][LOAD] Loaded v" + descriptor.version());
        }
    }

    private AddonDescriptor readDescriptor(JarFile jar, String fileName) throws IOException {
        JarEntry entry = jar.getJarEntry("addon.properties");
        if (entry == null) {
            plugin.getLogger().warning("[MultiBlockEngine][AddonLoader] Skipping " + fileName + ": missing addon.properties");
            return null;
        }

        Properties props = new Properties();
        try (InputStream in = jar.getInputStream(entry)) {
            props.load(in);
        }

        String id = trimToNull(props.getProperty("id"));
        String version = trimToNull(props.getProperty("version"));
        String main = trimToNull(props.getProperty("main"));

        if (main == null) {
            Attributes attributes = jar.getManifest() != null ? jar.getManifest().getMainAttributes() : null;
            if (attributes != null) {
                main = trimToNull(attributes.getValue("Multiblock-Addon-Main"));
            }
        }

        if (id == null || version == null || main == null) {
            plugin.getLogger().warning("[MultiBlockEngine][AddonLoader] Skipping " + fileName + ": addon.properties requires id, version, main");
            return null;
        }

        if (!id.matches("[a-z0-9][a-z0-9_\\-]*(?::[a-z0-9][a-z0-9_\\-]*)?")) {
            plugin.getLogger().warning("[MultiBlockEngine][AddonLoader] Skipping " + fileName + ": invalid id '" + id + "'");
            return null;
        }

        int apiVersion = MultiBlockEngine.getApiVersion();
        String api = trimToNull(props.getProperty("api"));
        if (api == null) {
            api = trimToNull(props.getProperty("apiVersion"));
        }
        if (api != null) {
            try {
                apiVersion = Integer.parseInt(api);
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("[MultiBlockEngine][AddonLoader] Skipping " + fileName + ": invalid api version '" + api + "'");
                return null;
            }
        }

        List<String> depends = new ArrayList<>();
        String dependsStr = trimToNull(props.getProperty("depends"));
        if (dependsStr != null) {
            for (String part : dependsStr.split("[,; ]+")) {
                String dep = trimToNull(part);
                if (dep != null) {
                    depends.add(dep);
                }
            }
        }

        return new AddonDescriptor(id, version, main, apiVersion, List.copyOf(depends));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void close(URLClassLoader loader) {
        try {
            loader.close();
        } catch (IOException ignored) {
        }
    }

    public void enableAddons() {
        Set<String> enabled = new HashSet<>();
        Set<String> pending = new HashSet<>(loadedAddons.keySet());

        boolean progressed = true;
        while (progressed && !pending.isEmpty()) {
            progressed = false;

            List<String> toEnable = new ArrayList<>();
            for (String id : pending) {
                LoadedAddon addon = loadedAddons.get(id);
                if (addon == null) continue;
                if (states.getOrDefault(id, AddonState.DISABLED) != AddonState.LOADED) continue;

                boolean depsOk = true;
                for (String dep : addon.descriptor().depends()) {
                    if (!enabled.contains(dep)) {
                        depsOk = false;
                        break;
                    }
                }
                if (depsOk) {
                    toEnable.add(id);
                }
            }

            for (String id : toEnable) {
                progressed = true;
                pending.remove(id);

                LoadedAddon loaded = loadedAddons.get(id);
                if (loaded == null) continue;

                try {
                    loaded.addon().onEnable();
                    states.put(id, AddonState.ENABLED);
                    enabled.add(id);
                    enableOrder.addLast(id);
                    plugin.getLogger().info("[MultiBlockEngine][Addon:" + id + "][ENABLE] Enabled");
                } catch (AddonException e) {
                    failAddon(id, AddonException.Phase.ENABLE, e.getMessage(), e.getCause(), e.isFatal());
                } catch (Throwable t) {
                    failAddon(id, AddonException.Phase.ENABLE, "Unhandled exception during onEnable", t, true);
                }
            }
        }

        for (String id : pending) {
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;

            StringBuilder missing = new StringBuilder();
            for (String dep : loaded.descriptor().depends()) {
                if (states.getOrDefault(dep, AddonState.DISABLED) != AddonState.ENABLED) {
                    if (!missing.isEmpty()) missing.append(", ");
                    missing.append(dep);
                }
            }
            failAddon(id, AddonException.Phase.ENABLE, "Missing or failed dependencies: " + missing, null, true);
        }
    }

    public void disableAddons() {
        while (!enableOrder.isEmpty()) {
            String id = enableOrder.removeLast();
            LoadedAddon loaded = loadedAddons.get(id);
            if (loaded == null) continue;

            try {
                loaded.addon().onDisable();
                plugin.getLogger().info("[MultiBlockEngine][Addon:" + id + "][DISABLE] Disabled");
            } catch (Throwable t) {
                failAddon(id, AddonException.Phase.DISABLE, "Unhandled exception during onDisable", t, false);
            }

            states.put(id, AddonState.DISABLED);
            close(loaded.classLoader());
        }

        for (String id : new HashSet<>(loadedAddons.keySet())) {
            LoadedAddon loaded = loadedAddons.remove(id);
            if (loaded != null) {
                close(loaded.classLoader());
            }
            states.putIfAbsent(id, AddonState.DISABLED);
        }
    }
}
