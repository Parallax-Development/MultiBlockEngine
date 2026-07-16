package dev.darkblade.mbe.core.application.command;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.SenderMapper;

public class MBECommandManager extends LegacyPaperCommandManager<CommandSender> {

    public MBECommandManager(Plugin owningPlugin) {
        super(
                owningPlugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );

        if (this.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            this.registerBrigadier();
        } else if (this.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            this.registerAsynchronousCompletions();
        }
    }
    
    // Future expansion: we can add helper methods here if needed
}
