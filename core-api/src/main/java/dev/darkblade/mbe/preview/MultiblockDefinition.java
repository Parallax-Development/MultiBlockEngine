package dev.darkblade.mbe.preview;

import java.util.Collection;

public interface MultiblockDefinition {
    String id();
    Collection<PreviewBlock> blocks();
}
