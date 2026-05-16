package dev.darkblade.mbe.core.application.command.service;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Extensible interface for services subcommands.
 * <p>
 * Register implementations via {@link ServicesCommandRouter#registerSubcommand(ServicesSubcommand)}
 * to add new subcommands under {@code /mbe services <name>} without modifying the core router.
 */
public interface ServicesSubcommand {

    /**
     * The subcommand name (e.g. "list", "info").
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
