package dev.darkblade.mbe.api.tool;

import java.util.List;

public interface ToolItem {

    String getId();

    List<String> getSupportedModes();
}
