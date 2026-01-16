package com.darkbladedev.engine.command.services;

import com.darkbladedev.engine.api.command.MbeCommandService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ServiceRegistry {

    private final Map<String, MbeCommandService> byId = new HashMap<>();
    private final Map<String, String> aliasToId = new HashMap<>();

    public void register(MbeCommandService service) {
        Objects.requireNonNull(service, "service");
        String id = normalize(service.id());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("service.id is blank");
        }
        if (byId.containsKey(id)) {
            throw new IllegalStateException("Service already registered: " + id);
        }
        byId.put(id, service);

        for (String alias : service.aliases()) {
            String a = normalize(alias);
            if (!a.isEmpty() && !aliasToId.containsKey(a)) {
                aliasToId.put(a, id);
            }
        }
    }

    public Optional<MbeCommandService> resolve(String idOrAlias) {
        String key = normalize(idOrAlias);
        if (key.isEmpty()) {
            return Optional.empty();
        }

        MbeCommandService direct = byId.get(key);
        if (direct != null) {
            return Optional.of(direct);
        }

        String id = aliasToId.get(key);
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(byId.get(id));
    }

    public List<MbeCommandService> list() {
        List<MbeCommandService> out = new ArrayList<>(byId.values());
        out.sort(Comparator.comparing(s -> normalize(s.id())));
        return List.copyOf(out);
    }

    public List<String> ids() {
        return list().stream().map(MbeCommandService::id).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

