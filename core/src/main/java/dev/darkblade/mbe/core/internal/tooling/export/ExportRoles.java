package dev.darkblade.mbe.core.internal.tooling.export;

import java.util.List;

/**
 * Well-known block roles used by the export tooling.
 *
 * <p>These are the roles that the {@link YamlStructureWriter} gives first-class treatment to when
 * building the YAML output (i.e. they map to specific top-level YAML fields rather than being
 * relegated to {@code extensions}). Any role string not listed here is still accepted and stored
 * as an extra role under {@code extensions.mbe-exporter.roles}.</p>
 */
public final class ExportRoles {

    /** Marks the block that acts as the multiblock controller. */
    public static final String CONTROLLER = "controller";

    /** Marks a block as an input port. */
    public static final String INPUT = "input";

    /** Marks a block as an output port. */
    public static final String OUTPUT = "output";

    /**
     * All roles that receive first-class YAML treatment in export output, in suggested display
     * order.
     */
    public static final List<String> KNOWN = List.of(CONTROLLER, INPUT, OUTPUT);

    private ExportRoles() {
    }
}
