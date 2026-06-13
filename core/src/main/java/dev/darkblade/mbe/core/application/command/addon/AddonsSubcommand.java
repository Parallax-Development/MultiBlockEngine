package dev.darkblade.mbe.core.application.command.addon;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Extensible interface for addon subcommands.
 * <p>
 * Register implementations via {@link AddonsCommandRouter#registerSubcommand(AddonsSubcommand)}
 * to add new subcommands under {@code /mbe addons <name>} without modifying the core router.
 */
public interface AddonsSubcommand {

    /**
     * The subcommand name (e.g. "list", "status").
     * Must be lowercase, unique within the router.
     */
    String name();

    /**
     * Execute this subcommand.
     *
     * @param sender the command sender
     * @param label  the command label used (e.g. "mbe")
     * @param args   arguments after the subcommand name
     */
    void execute(CommandSender sender, String label, List<String> args);

    /**
     * Provide tab-completion suggestions.
     *
     * @param sender the command sender
     * @param args   arguments after the subcommand name
     * @return suggestions (may be empty)
     */
    default List<String> tabComplete(CommandSender sender, List<String> args) {
        return List.of();
    }
}
