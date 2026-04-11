package dev.darkblade.mbe.api.tool;

import java.util.Collection;

public interface Tool {

    String id();

    Collection<ToolMode> modes();

    default String defaultMode() {
        return modes().iterator().next().id();
    }
}
