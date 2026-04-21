package dev.darkblade.mbe.api.block;

import java.util.Collection;

public interface BlockRegistry {

    void register(BlockDefinition definition);

    BlockDefinition get(BlockKey key);

    boolean exists(BlockKey key);

    Collection<BlockDefinition> all();
}
