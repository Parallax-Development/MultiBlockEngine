package dev.darkblade.mbe.core.application.service.addon;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class AddonClassLoader extends URLClassLoader {
    private final String addonId;
    private final List<AddonClassLoader> dependencies;

    public AddonClassLoader(String addonId, URL[] urls, ClassLoader parent, List<AddonClassLoader> dependencies) {
        super(urls, parent);
        this.addonId = addonId == null ? "unknown" : addonId;
        this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }

            if (isParentFirst(name)) {
                try {
                    Class<?> parentClass = getParent().loadClass(name);
                    if (resolve) {
                        resolveClass(parentClass);
                    }
                    return parentClass;
                } catch (ClassNotFoundException ignored) {
                }
            }

            Class<?> local = tryLoadOwnClass(name);
            if (local != null) {
                if (resolve) {
                    resolveClass(local);
                }
                return local;
            }

            for (AddonClassLoader dependency : dependencies) {
                if (dependency == null) {
                    continue;
                }
                Class<?> depClass = dependency.tryLoadOwnClass(name);
                if (depClass != null) {
                    if (resolve) {
                        resolveClass(depClass);
                    }
                    return depClass;
                }
            }

            Class<?> parentClass = getParent().loadClass(name);
            if (resolve) {
                resolveClass(parentClass);
            }
            return parentClass;
        }
    }

    private Class<?> tryLoadOwnClass(String name) {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                return loaded;
            }
            try {
                return findClass(name);
            } catch (ClassNotFoundException ignored) {
                return null;
            }
        }
    }

    private boolean isParentFirst(String name) {
        if (name == null || name.isBlank()) {
            return true;
        }
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("com.sun.")
                || name.startsWith("org.bukkit.")
                || name.startsWith("io.papermc.")
                || name.startsWith("net.minecraft.")
                || name.startsWith("com.destroystokyo.paper.");
    }

    @Override
    public String toString() {
        return "AddonClassLoader[" + addonId + "]";
    }
}
