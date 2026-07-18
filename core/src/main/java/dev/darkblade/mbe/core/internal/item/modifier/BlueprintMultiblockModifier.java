package dev.darkblade.mbe.core.internal.item.modifier;

import dev.darkblade.mbe.api.item.ItemModifier;
import dev.darkblade.mbe.api.persistence.item.NamespacedKey;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import net.kyori.adventure.key.Key;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;

import java.util.List;
import java.util.stream.Collectors;

public class BlueprintMultiblockModifier implements ItemModifier<CommandSender, Key> {

    private final MultiblockRuntimeService runtimeService;
    private final Key id = Key.key("mbe", "multiblock");

    public BlueprintMultiblockModifier(MultiblockRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public Key id() {
        return id;
    }

    @Override
    public Class<Key> type() {
        return Key.class;
    }

    @Override
    public Key parse(String value) throws IllegalArgumentException {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Multiblock ID cannot be blank");
        }
        return Key.key(value);
    }

    @Override
    public List<String> suggestions(CommandContext<CommandSender> context) {
        return runtimeService.getTypes().stream()
                .map(t -> {
                    String typeId = t.id();
                    return typeId.contains(":") ? typeId : "mbe:" + typeId;
                })
                .collect(Collectors.toList());
    }
}
