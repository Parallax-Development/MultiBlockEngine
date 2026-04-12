package dev.darkblade.mbe.core.application.service;

import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.addon.crossref.CrossReferenceHandle;
import dev.darkblade.mbe.api.addon.crossref.InjectCrossReference;
import dev.darkblade.mbe.api.service.InjectService;
import dev.darkblade.mbe.core.application.service.addon.crossref.AddonCrossReferenceService;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public final class ServiceInjector {
    private static final BiFunction<String, Class<?>, Optional<?>> NO_EXTERNAL_RESOLVER = (ownerId, type) -> Optional.empty();
    private final MBEServiceRegistry registry;
    private final AddonCrossReferenceService crossReferenceManager;
    private final BiFunction<String, Class<?>, Optional<?>> externalResolver;
    private final CoreLogger log;

    public ServiceInjector(MBEServiceRegistry registry, CoreLogger log) {
        this(registry, null, log, NO_EXTERNAL_RESOLVER);
    }

    public ServiceInjector(MBEServiceRegistry registry, AddonCrossReferenceService crossReferenceManager, CoreLogger log) {
        this(registry, crossReferenceManager, log, NO_EXTERNAL_RESOLVER);
    }

    public ServiceInjector(MBEServiceRegistry registry, AddonCrossReferenceService crossReferenceManager, CoreLogger log, BiFunction<String, Class<?>, Optional<?>> externalResolver) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.crossReferenceManager = crossReferenceManager;
        this.externalResolver = externalResolver == null ? NO_EXTERNAL_RESOLVER : externalResolver;
        this.log = Objects.requireNonNull(log, "log");
    }

    public void inject(Object target, String ownerId) {
        if (target == null) {
            return;
        }
        for (Field field : fieldsOf(target.getClass())) {
            InjectService marker = field.getAnnotation(InjectService.class);
            if (marker != null) {
                injectField(target, field, marker.value(), ownerId);
            }
            InjectCrossReference crossReference = field.getAnnotation(InjectCrossReference.class);
            if (crossReference != null) {
                injectCrossReference(target, field, crossReference, ownerId);
            }
        }
    }

    private void injectField(Object target, Field field, String serviceId, String ownerId) {
        String id = serviceId == null ? "" : serviceId.trim();
        boolean optionalTarget = Optional.class.equals(field.getType());
        field.setAccessible(true);

        try {
            if (id.isEmpty()) {
                if (optionalTarget) {
                    Class<?> wrappedType = optionalFieldType(field);
                    if (wrappedType == null) {
                        warn(ownerId, "Service injection skipped: Optional field without concrete generic type", field, id, null);
                        field.set(target, Optional.empty());
                        return;
                    }
                    java.util.List<?> matches = registry.getByType(wrappedType);
                    if (matches.isEmpty()) {
                        Optional<?> external = resolveExternal(ownerId, wrappedType);
                        if (external.isPresent()) {
                            field.set(target, Optional.of(external.get()));
                            return;
                        }
                        field.set(target, Optional.empty());
                        optionalMissing(ownerId, field, id, wrappedType);
                        return;
                    }
                    field.set(target, Optional.of(matches.get(0)));
                    return;
                }

                java.util.List<?> matches = registry.getByType(field.getType());
                if (!matches.isEmpty()) {
                    field.set(target, matches.get(0));
                    return;
                }
                Optional<?> external = resolveExternal(ownerId, field.getType());
                if (external.isPresent()) {
                    field.set(target, external.get());
                    return;
                }
                warn(ownerId, "Service not available for type-based injection", field, id, field.getType());
                return;
            }

            if (optionalTarget) {
                Class<?> wrappedType = optionalFieldType(field);
                if (wrappedType == null) {
                    warn(ownerId, "Service injection skipped: Optional field without concrete generic type", field, id, null);
                    field.set(target, Optional.empty());
                    return;
                }
                Object value = registry.get(id, wrappedType);
                if (((Optional<?>) value).isEmpty()) {
                    Optional<?> external = resolveExternal(ownerId, wrappedType);
                    if (external.isPresent()) {
                        value = Optional.of(external.get());
                    }
                }
                field.set(target, value);
                if (((Optional<?>) value).isEmpty()) {
                    optionalMissing(ownerId, field, id, wrappedType);
                }
                return;
            }

            Optional<?> resolved = registry.get(id, field.getType());
            if (resolved.isEmpty()) {
                resolved = resolveExternal(ownerId, field.getType());
            }
            if (resolved.isPresent()) {
                field.set(target, resolved.get());
                return;
            }

            warn(ownerId, "Service not available for injection", field, id, field.getType());
        } catch (Throwable t) {
            log.logInternal(scope(ownerId), LogPhase.SERVICE_RESOLVE, LogLevel.WARN, "Service injection failed", t, new LogKv[] {
                LogKv.kv("owner", ownerId == null ? "unknown" : ownerId),
                LogKv.kv("targetType", target.getClass().getName()),
                LogKv.kv("field", field.getName()),
                LogKv.kv("serviceId", id)
            }, Set.of());
        }
    }

    private void warn(String ownerId, String message, Field field, String serviceId, Class<?> expectedType) {
        log.logInternal(scope(ownerId), LogPhase.SERVICE_RESOLVE, LogLevel.WARN, message, null, new LogKv[] {
            LogKv.kv("owner", ownerId == null ? "unknown" : ownerId),
            LogKv.kv("targetField", field.getDeclaringClass().getName() + "#" + field.getName()),
            LogKv.kv("serviceId", serviceId == null ? "" : serviceId),
            LogKv.kv("expectedType", expectedType == null ? "" : expectedType.getName())
        }, Set.of());
    }

    private void optionalMissing(String ownerId, Field field, String serviceId, Class<?> expectedType) {
        log.logInternal(scope(ownerId), LogPhase.SERVICE_RESOLVE, LogLevel.DEBUG, "Optional service not available", null, new LogKv[] {
            LogKv.kv("owner", ownerId == null ? "unknown" : ownerId),
            LogKv.kv("targetField", field.getDeclaringClass().getName() + "#" + field.getName()),
            LogKv.kv("serviceId", serviceId == null ? "" : serviceId),
            LogKv.kv("expectedType", expectedType == null ? "" : expectedType.getName())
        }, Set.of());
    }

    private Optional<?> resolveExternal(String ownerId, Class<?> expectedType) {
        if (expectedType == null) {
            return Optional.empty();
        }
        try {
            Optional<?> resolved = externalResolver.apply(ownerId, expectedType);
            return resolved == null ? Optional.empty() : resolved;
        } catch (Throwable t) {
            log.logInternal(scope(ownerId), LogPhase.SERVICE_RESOLVE, LogLevel.WARN, "External service resolve failed", t, new LogKv[] {
                LogKv.kv("owner", ownerId == null ? "unknown" : ownerId),
                LogKv.kv("expectedType", expectedType.getName())
            }, Set.of());
            return Optional.empty();
        }
    }

    private void injectCrossReference(Object target, Field field, InjectCrossReference marker, String ownerId) {
        if (crossReferenceManager == null) {
            warn(ownerId, "Cross-reference injection skipped: manager unavailable", field, marker.value(), field.getType());
            return;
        }
        field.setAccessible(true);
        try {
            if (CrossReferenceHandle.class.equals(field.getType())) {
                Class<?> wrappedType = optionalFieldType(field);
                if (wrappedType == null) {
                    warn(ownerId, "Cross-reference injection skipped: CrossReferenceHandle field without concrete generic type", field, marker.value(), null);
                    field.set(target, CrossReferenceHandle.unresolved());
                    return;
                }
                field.set(target, crossReferenceManager.handle(marker.value(), wrappedType));
                return;
            }

            Optional<?> resolved = crossReferenceManager.resolve(marker.value(), field.getType());
            if (resolved.isPresent()) {
                field.set(target, resolved.get());
                return;
            }
            if (marker.required()) {
                warn(ownerId, "Required cross-reference not available for injection", field, marker.value(), field.getType());
            }
        } catch (Throwable t) {
            log.logInternal(scope(ownerId), LogPhase.SERVICE_RESOLVE, LogLevel.WARN, "Cross-reference injection failed", t, new LogKv[] {
                LogKv.kv("owner", ownerId == null ? "unknown" : ownerId),
                LogKv.kv("targetType", target.getClass().getName()),
                LogKv.kv("field", field.getName()),
                LogKv.kv("crossReferenceId", marker.value())
            }, Set.of());
        }
    }

    private static Class<?> optionalFieldType(Field field) {
        Type type = field.getGenericType();
        if (!(type instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        if (args.length != 1 || !(args[0] instanceof Class<?> c)) {
            return null;
        }
        return c;
    }

    private static Field[] fieldsOf(Class<?> type) {
        java.util.ArrayList<Field> fields = new java.util.ArrayList<>();
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                fields.add(field);
            }
            cursor = cursor.getSuperclass();
        }
        return fields.toArray(Field[]::new);
    }

    private static LogScope scope(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            return new LogScope.Core();
        }
        return new LogScope.Addon(ownerId, "unknown");
    }
}
