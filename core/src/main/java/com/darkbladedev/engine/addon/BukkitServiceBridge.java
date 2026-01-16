package com.darkbladedev.engine.addon;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

public final class BukkitServiceBridge {

    private BukkitServiceBridge() {
    }

    public static <T> void expose(Plugin plugin, Class<T> api, T implementation, ServicePriority priority) {
        exposeProvider(plugin, api, implementation, priority);
    }

    public static Object exposeProviderRaw(Plugin plugin, Class<?> api, Object implementation, ServicePriority priority) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(priority, "priority");

        if (!api.isInterface()) {
            throw new IllegalArgumentException("Service API must be an interface: " + api.getName());
        }

        if (!api.isInstance(implementation)) {
            throw new IllegalArgumentException("Implementation does not implement API (ClassLoader mismatch?): api=" + api.getName() + " impl=" + implementation.getClass().getName());
        }

        Object exposed = wrapIfNeededRaw(api, implementation);
        Bukkit.getServicesManager().register((Class) api, exposed, plugin, priority);
        return exposed;
    }

    public static <T> T exposeProvider(Plugin plugin, Class<T> api, T implementation, ServicePriority priority) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(implementation, "implementation");
        Objects.requireNonNull(priority, "priority");

        if (!api.isInterface()) {
            throw new IllegalArgumentException("Service API must be an interface: " + api.getName());
        }

        if (!api.isInstance(implementation)) {
            throw new IllegalArgumentException("Implementation does not implement API (ClassLoader mismatch?): api=" + api.getName() + " impl=" + implementation.getClass().getName());
        }

        Object exposed = wrapIfNeeded(api, implementation);
        T provider = api.cast(exposed);
        Bukkit.getServicesManager().register(api, provider, plugin, priority);
        return provider;
    }

    public static <T> void expose(Plugin plugin, Class<T> api, T implementation) {
        expose(plugin, api, implementation, ServicePriority.Normal);
    }

    public static void unexpose(Class<?> api, Object provider) {
        Objects.requireNonNull(api, "api");
        Objects.requireNonNull(provider, "provider");
        Bukkit.getServicesManager().unregister((Class) api, provider);
    }

    public static void unexposeAll(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        Bukkit.getServicesManager().unregisterAll(plugin);
    }

    private static <T> Object wrapIfNeeded(Class<T> api, T implementation) {
        return wrapIfNeededRaw(api, implementation);
    }

    private static Object wrapIfNeededRaw(Class<?> api, Object implementation) {
        ClassLoader apiCl = api.getClassLoader();
        ClassLoader implCl = implementation.getClass().getClassLoader();
        if (apiCl == implCl) {
            return implementation;
        }

        InvocationHandler handler = new DelegatingInvocationHandler(implementation);
        return Proxy.newProxyInstance(apiCl, new Class<?>[]{api}, handler);
    }

    private static final class DelegatingInvocationHandler implements InvocationHandler {
        private final Object delegate;

        private DelegatingInvocationHandler(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return switch (name) {
                    case "toString" -> "ServiceProxy(" + delegate + ")";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args != null && args.length > 0 ? args[0] : null);
                    default -> method.invoke(delegate, args);
                };
            }

            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
