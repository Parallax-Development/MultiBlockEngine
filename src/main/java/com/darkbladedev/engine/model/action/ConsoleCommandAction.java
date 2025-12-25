package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import org.bukkit.Bukkit;

public record ConsoleCommandAction(String command) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        // Replace placeholders if any (e.g. %x%, %y%, %z%)
        String cmd = command
                .replace("%x%", String.valueOf(instance.anchorLocation().getBlockX()))
                .replace("%y%", String.valueOf(instance.anchorLocation().getBlockY()))
                .replace("%z%", String.valueOf(instance.anchorLocation().getBlockZ()))
                .replace("%world%", instance.anchorLocation().getWorld().getName());
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
