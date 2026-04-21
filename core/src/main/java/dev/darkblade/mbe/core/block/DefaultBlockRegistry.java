package dev.darkblade.mbe.core.block;

import dev.darkblade.mbe.api.block.BlockDefinition;
import dev.darkblade.mbe.api.block.BlockKey;
import dev.darkblade.mbe.api.block.BlockRegistry;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.infrastructure.config.parser.MultiblockParser;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultBlockRegistry implements BlockRegistry {
    private final Map<BlockKey, BlockDefinition> registry = new HashMap<>();
    private final MultiblockRuntimeService runtimeService;

    public DefaultBlockRegistry(MultiblockRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public void register(BlockDefinition definition) {
        registry.put(definition.key(), definition);
        MultiblockType type = BlockToMultiblockAdapter.adapt(definition);
        try {
            runtimeService.registerType(type, new dev.darkblade.mbe.core.domain.MultiblockSource(dev.darkblade.mbe.core.domain.MultiblockSource.Type.CORE_DEFAULT, "builtin"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public BlockDefinition get(BlockKey key) {
        return registry.get(key);
    }

    @Override
    public boolean exists(BlockKey key) {
        return registry.containsKey(key);
    }

    @Override
    public Collection<BlockDefinition> all() {
        return Collections.unmodifiableCollection(registry.values());
    }
}
