package dev.darkblade.mbe.api.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface MbeCommandService {
    String id();

    String description();

    List<String> infoUsage();

    List<String> executeUsage();

    void info(CommandSender sender, List<String> args);

    void execute(CommandSender sender, List<String> args);

    default List<String> aliases() {
        return List.of();
    }

    default List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        return List.of();
    }
}

