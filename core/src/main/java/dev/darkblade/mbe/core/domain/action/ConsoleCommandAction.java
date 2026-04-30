package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.service.security.TrustedCommandService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import org.bukkit.Bukkit;

public record ConsoleCommandAction(String command) implements Action {
    @Override
    public void execute(MultiblockInstance instance) {
        String cmd = StringUtil.parsePlaceholders(command, instance);
        
        TrustedCommandService security = MultiBlockEngine.getInstance().getTrustedCommandService();
        if (security != null && !security.isTrusted(cmd)) {
            Bukkit.getLogger().warning("[MBE-Security] BLOCKED untrusted console command: " + cmd);
            return;
        }
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
