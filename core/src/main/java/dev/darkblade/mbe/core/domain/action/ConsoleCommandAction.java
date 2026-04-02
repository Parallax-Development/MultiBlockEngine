package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import org.bukkit.Bukkit;

public record ConsoleCommandAction(String command) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        String cmd = StringUtil.parsePlaceholders(command, instance);
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
