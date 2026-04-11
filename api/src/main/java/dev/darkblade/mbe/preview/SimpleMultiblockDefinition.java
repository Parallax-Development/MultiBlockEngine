package dev.darkblade.mbe.preview;

import java.util.List;

public record SimpleMultiblockDefinition(String id, List<PreviewBlock> blocks) implements MultiblockDefinition {
    public SimpleMultiblockDefinition {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }
}
