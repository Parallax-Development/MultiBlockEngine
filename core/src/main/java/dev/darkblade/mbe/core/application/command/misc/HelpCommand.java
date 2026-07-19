package dev.darkblade.mbe.core.application.command.misc;

import dev.darkblade.mbe.core.application.command.MBESender;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public class HelpCommand {

    @Command("mbe help [query]")
    @Permission("multiblockengine.help")
    public void help(MBESender sender, @Argument("query") String query) {
        CommandSender bukkitSender = sender.getSender();
        if (query == null || query.isBlank()) {
            bukkitSender.sendMessage("§6§lMultiBlockEngine §8» §fHelp");
            bukkitSender.sendMessage("§e/mbe catalog §7- Open Blueprint Catalog");
            bukkitSender.sendMessage("§e/mbe blueprint <id> §7- Get a blueprint");
            bukkitSender.sendMessage("§e/mbe structure assemble §7- Assemble a multiblock");
            bukkitSender.sendMessage("§e/mbe structure disassemble §7- Disassemble a multiblock");
            bukkitSender.sendMessage("§e/mbe structure export start §7- Start structure export");
            bukkitSender.sendMessage("§e/mbe item list §7- List all MBE items");
            bukkitSender.sendMessage("§e/mbe item give <item> §7- Give an item");
            bukkitSender.sendMessage("§e/mbe admin status §7- View plugin status");
        } else {
            bukkitSender.sendMessage("§cFor more detailed help, refer to the plugin documentation or use /mbe help.");
        }
    }
}
