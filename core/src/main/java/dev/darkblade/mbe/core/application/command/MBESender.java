package dev.darkblade.mbe.core.application.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MBESender {
    private final CommandSender sender;

    public MBESender(CommandSender sender) {
        this.sender = sender;
    }

    public CommandSender getSender() {
        return sender;
    }
    
    public Player getPlayer() {
        return (Player) sender;
    }
    
    public boolean isPlayer() {
        return sender instanceof Player;
    }
}
