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
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§6§lMultiBlockEngine §8» §fHelp"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe catalog §7- Open Blueprint Catalog"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe blueprint <id> §7- Get a blueprint"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe structure assemble §7- Assemble a multiblock"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe structure disassemble §7- Disassemble a multiblock"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe structure export start §7- Start structure export"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe item list §7- List all MBE items"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe item give <item> §7- Give an item"));
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§e/mbe admin status §7- View plugin status"));
        } else {
            bukkitSender.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent("§cFor more detailed help, refer to the plugin documentation or use /mbe help."));
        }
    }
}
