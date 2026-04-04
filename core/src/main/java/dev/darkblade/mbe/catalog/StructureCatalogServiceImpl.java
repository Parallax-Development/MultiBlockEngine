package dev.darkblade.mbe.catalog;

import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.BlockMatcher;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.PatternEntry;
import dev.darkblade.mbe.core.domain.rule.AnyOfMatcher;
import dev.darkblade.mbe.core.domain.rule.BlockDataMatcher;
import dev.darkblade.mbe.core.domain.rule.ExactMaterialMatcher;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import dev.darkblade.mbe.preview.PreviewBlock;
import dev.darkblade.mbe.preview.SimpleMultiblockDefinition;
import dev.darkblade.mbe.preview.Vector3i;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class StructureCatalogServiceImpl implements StructureCatalogService {
    private final MultiblockRuntimeService runtime;

    public StructureCatalogServiceImpl(MultiblockRuntimeService runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public List<MultiblockDefinition> getAll() {
        List<MultiblockDefinition> out = new ArrayList<>();
        for (MultiblockType type : runtime.getTypesDeterministic()) {
            if (type == null) {
                continue;
            }
            out.add(toDefinition(type));
        }
        return List.copyOf(out);
    }

    private MultiblockDefinition toDefinition(MultiblockType type) {
        Map<Vector3i, PreviewBlock> unique = new LinkedHashMap<>();
        for (PatternEntry entry : type.pattern()) {
            if (entry == null || entry.offset() == null || entry.matcher() == null) {
                continue;
            }
            BlockData blockData = toBlockData(entry.matcher());
            if (blockData == null) {
                continue;
            }
            Vector3i position = Vector3i.fromVector(entry.offset());
            unique.put(position, new PreviewBlock(position, blockData));
        }
        if (type.controllerOffset() != null && type.controllerMatcher() != null) {
            BlockData controllerData = toBlockData(type.controllerMatcher());
            if (controllerData != null) {
                Vector3i controllerPosition = Vector3i.fromVector(type.controllerOffset());
                unique.put(controllerPosition, new PreviewBlock(controllerPosition, controllerData));
            }
        }
        return new SimpleMultiblockDefinition(type.id(), new ArrayList<>(unique.values()));
    }

    private BlockData toBlockData(BlockMatcher matcher) {
        if (matcher instanceof BlockDataMatcher blockDataMatcher) {
            BlockData data = blockDataMatcher.expectedData();
            return data == null ? Material.GLASS.createBlockData() : data.clone();
        }
        if (matcher instanceof ExactMaterialMatcher exactMaterialMatcher) {
            Material material = exactMaterialMatcher.material();
            if (material != null && material.isBlock()) {
                return material.createBlockData();
            }
            return Material.GLASS.createBlockData();
        }
        if (matcher instanceof AnyOfMatcher anyOfMatcher) {
            for (BlockMatcher child : anyOfMatcher.matchers()) {
                BlockData resolved = toBlockData(child);
                if (resolved != null) {
                    return resolved;
                }
            }
            return Material.GLASS.createBlockData();
        }
        return Material.GLASS.createBlockData();
    }
}
