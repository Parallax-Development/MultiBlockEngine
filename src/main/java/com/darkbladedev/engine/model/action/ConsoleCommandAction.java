package com.darkbladedev.engine.model.action;

import com.darkbladedev.engine.model.MultiblockInstance;
import com.darkbladedev.engine.util.StringUtil;
import org.bukkit.Bukkit;

public record ConsoleCommandAction(String command) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        String cmd = StringUtil.parsePlaceholders(command, instance);
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
