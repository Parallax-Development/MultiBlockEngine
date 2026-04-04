package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.api.blueprint.BlueprintService;
import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public final class BlueprintCommandService implements MbeCommandService {
    private final BlueprintService blueprintService;

    public BlueprintCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.blueprintService = plugin.getAddonLifecycleService().getCoreService(BlueprintService.class);
    }

    public void openCatalog(Player player) {
        if (player == null) {
            return;
        }
        if (blueprintService == null) {
            player.sendMessage(Component.text("BlueprintService no disponible.", NamedTextColor.RED));
            return;
        }
        blueprintService.openCatalog(player);
    }

    @Override
    public String id() {
        return "blueprint";
    }

    @Override
    public String description() {
        return "Operaciones de catálogo de blueprints";
    }

    @Override
    public List<String> infoUsage() {
        return List.of("/mbe services call blueprint info");
    }

    @Override
    public List<String> executeUsage() {
        return List.of("/mbe services call blueprint execute catalog");
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        sender.sendMessage(Component.text("Servicio blueprint: execute catalog", NamedTextColor.GRAY));
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el catálogo.", NamedTextColor.RED));
            return;
        }
        if (args == null || args.isEmpty()) {
            sender.sendMessage(Component.text("/mbe services call blueprint execute catalog", NamedTextColor.GRAY));
            return;
        }
        String op = args.get(0) == null ? "" : args.get(0).trim().toLowerCase(java.util.Locale.ROOT);
        if (!"catalog".equals(op)) {
            sender.sendMessage(Component.text("Subcomando inválido. Usa: catalog", NamedTextColor.RED));
            return;
        }
        openCatalog(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        if ("info".equalsIgnoreCase(mode)) {
            return List.of();
        }
        if (args == null || args.size() <= 1) {
            String input = args == null || args.isEmpty() || args.get(0) == null ? "" : args.get(0).toLowerCase(java.util.Locale.ROOT);
            if ("catalog".startsWith(input)) {
                return List.of("catalog");
            }
        }
        return List.of();
    }
}
