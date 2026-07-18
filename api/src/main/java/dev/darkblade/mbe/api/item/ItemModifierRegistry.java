package dev.darkblade.mbe.api.item;

import dev.darkblade.mbe.api.persistence.item.NamespacedKey;
import net.kyori.adventure.key.Key;

import java.util.Collection;
import java.util.Optional;

public interface ItemModifierRegistry<C> {

    void register(NamespacedKey itemId, ItemModifier<C, ?> modifier);

    void registerGlobal(ItemModifier<C, ?> modifier);

    Collection<ItemModifier<C, ?>> getModifiers(NamespacedKey itemId);

    Optional<ItemModifier<C, ?>> getModifier(NamespacedKey itemId, Key modifierId);

}
