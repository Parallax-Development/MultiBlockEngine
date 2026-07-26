package dev.darkblade.mbe.api.ui;

import java.nio.file.Path;

/**
 * Service registry that allows addons to register external directories
 * containing UI panel configurations.
 * 
 * The UI system will iterate through these directories during its load cycle.
 */
public interface PanelDirectoryRegistry {
    /**
     * Registers an external directory for panel configuration scanning.
     *
     * @param directory The path to the directory containing panel YAMLs.
     */
    void registerDirectory(Path directory);
}
