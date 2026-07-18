package dev.darkblade.mbe.api.item;

import net.kyori.adventure.key.Key;
import org.incendo.cloud.context.CommandContext;

import java.util.List;

public interface ItemModifier<C, T> {

    Key id();

    Class<T> type();

    T parse(String value) throws IllegalArgumentException;

    List<String> suggestions(CommandContext<C> context);

}
