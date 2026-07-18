package dev.darkblade.mbe.core.internal.item;

import dev.darkblade.mbe.api.item.ItemModifier;
import dev.darkblade.mbe.api.item.ItemModifierRegistry;
import dev.darkblade.mbe.api.persistence.item.NamespacedKey;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ItemModifierRegistryImpl<C> implements ItemModifierRegistry<C> {

    private final Set<ItemModifier<C, ?>> globalModifiers = ConcurrentHashMap.newKeySet();
    private final Map<NamespacedKey, Set<ItemModifier<C, ?>>> itemModifiers = new ConcurrentHashMap<>();

    @Override
    public void register(NamespacedKey itemId, ItemModifier<C, ?> modifier) {
        if (itemId == null || modifier == null) {
            throw new IllegalArgumentException("itemId and modifier cannot be null");
        }
        itemModifiers.computeIfAbsent(itemId, k -> ConcurrentHashMap.newKeySet()).add(modifier);
    }

    @Override
    public void registerGlobal(ItemModifier<C, ?> modifier) {
        if (modifier == null) {
            throw new IllegalArgumentException("modifier cannot be null");
        }
        globalModifiers.add(modifier);
    }

    @Override
    public Collection<ItemModifier<C, ?>> getModifiers(NamespacedKey itemId) {
        if (itemId == null) {
            return Collections.emptyList();
        }
        Set<ItemModifier<C, ?>> all = new HashSet<>(globalModifiers);
        Set<ItemModifier<C, ?>> specific = itemModifiers.get(itemId);
        if (specific != null) {
            all.addAll(specific);
        }
        return all;
    }

    @Override
    public Optional<ItemModifier<C, ?>> getModifier(NamespacedKey itemId, Key modifierId) {
        if (itemId == null || modifierId == null) {
            return Optional.empty();
        }
        for (ItemModifier<C, ?> modifier : getModifiers(itemId)) {
            if (modifier.id().equals(modifierId)) {
                return Optional.of(modifier);
            }
        }
        return Optional.empty();
    }
}
