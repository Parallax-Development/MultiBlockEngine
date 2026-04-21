package dev.darkblade.mbe.core.block;

import dev.darkblade.mbe.api.block.BlockDefinition;
import dev.darkblade.mbe.core.domain.MultiblockType;
import dev.darkblade.mbe.core.domain.rule.ExactMaterialMatcher;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Collections;

public class BlockToMultiblockAdapter {

    public static MultiblockType adapt(BlockDefinition block) {
        Material material = Material.matchMaterial(block.blockMaterial());
        if (material == null) {
            material = Material.STONE;
        }

        return new MultiblockType(
                block.key().toString(),
                block.version(),
                block.assemblyTrigger(),
                new Vector(0, 0, 0),
                new ExactMaterialMatcher(material),
                Collections.emptyList(),
                true,
                block.behaviorConfig(),
                block.defaultVariables(),
                block.ports(),
                block.extensions(),
                block.onCreateActions(),
                block.onTickActions(),
                block.onInteractActions(),
                block.onBreakActions(),
                block.displayName(),
                block.tickInterval(),
                block.capabilityFactories()
        );
    }
}
