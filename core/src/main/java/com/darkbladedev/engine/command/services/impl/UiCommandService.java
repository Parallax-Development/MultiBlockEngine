package com.darkbladedev.engine.command.services.impl;

import com.darkbladedev.engine.api.command.MbeCommandService;
import com.mbe.ui.api.services.ux.UiUxMenusService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UiCommandService implements MbeCommandService {

    @Override
    public String id() {
        return "ui";
    }

    @Override
    public String description() {
        return "Acciones sobre menús UI (requiere MBE-UI)";
    }

    @Override
    public List<String> infoUsage() {
        return List.of(
                "/mbe services call ui info",
                "/mbe services call ui info open",
                "/mbe services call ui info reload"
        );
    }

    @Override
    public List<String> executeUsage() {
        return List.of(
                "/mbe services call ui execute open [player] <menuId> [key=value...]",
                "/mbe services call ui execute reload"
        );
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        sender.sendMessage(Component.text("Servicio: ui", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(description(), NamedTextColor.GRAY));
        for (String line : executeUsage()) {
            sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }

        UiUxMenusService svc = ui();
        if (svc == null) {
            sender.sendMessage(Component.text("UiUxMenusService no disponible (¿MBE-UI cargado?)", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("API: " + svc.apiVersion(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Dir: " + svc.menusDir(), NamedTextColor.GRAY));
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        UiUxMenusService svc = ui();
        if (svc == null) {
            sender.sendMessage(Component.text("UiUxMenusService no disponible (¿MBE-UI cargado?)", NamedTextColor.RED));
            return;
        }

        if (args == null || args.isEmpty()) {
            sender.sendMessage(Component.text("Falta subacción.", NamedTextColor.RED));
            for (String line : executeUsage()) {
                sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
            }
            return;
        }

        String sub = args.get(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                svc.reload();
                sender.sendMessage(Component.text("UI recargada.", NamedTextColor.GREEN));
            }
            case "open" -> executeOpen(sender, svc, args);
            default -> {
                sender.sendMessage(Component.text("Subacción inválida: " + sub, NamedTextColor.RED));
                for (String line : executeUsage()) {
                    sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
                }
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT);
        if (!m.equals("execute") || args == null) {
            return List.of();
        }

        if (args.size() <= 1) {
            return List.of("open", "reload");
        }

        String sub = args.get(0).toLowerCase(Locale.ROOT);
        if (sub.equals("open") && args.size() == 2) {
            List<String> out = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return out;
        }

        return List.of();
    }

    private void executeOpen(CommandSender sender, UiUxMenusService svc, List<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(Component.text("Uso: /mbe services call ui execute open [player] <menuId> [key=value...]", NamedTextColor.RED));
            return;
        }

        int idx = 1;
        Player target = null;
        String first = args.get(idx);
        if (first != null && !first.isBlank()) {
            Player maybePlayer = Bukkit.getPlayer(first);
            if (maybePlayer != null) {
                target = maybePlayer;
                idx++;
            }
        }

        if (target == null) {
            if (sender instanceof Player p) {
                target = p;
            } else {
                sender.sendMessage(Component.text("Debes especificar un jugador: open <player> <menuId>", NamedTextColor.RED));
                return;
            }
        }

        if (args.size() <= idx) {
            sender.sendMessage(Component.text("Falta menuId.", NamedTextColor.RED));
            return;
        }

        String menuId = args.get(idx);
        idx++;

        Map<String, Object> vars = new HashMap<>();
        for (int i = idx; i < args.size(); i++) {
            String token = args.get(i);
            if (token == null || token.isBlank()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq <= 0 || eq == token.length() - 1) {
                continue;
            }
            String k = token.substring(0, eq).trim();
            String v = token.substring(eq + 1).trim();
            if (k.isEmpty() || v.isEmpty()) {
                continue;
            }
            vars.put(k, coerce(v));
        }

        svc.open(target, menuId, vars, Optional.empty());
        sender.sendMessage(Component.text("Menú abierto: " + menuId + " -> " + target.getName(), NamedTextColor.GREEN));
    }

    private static Object coerce(String value) {
        Objects.requireNonNull(value, "value");
        String v = value.trim();
        if (v.equalsIgnoreCase("true")) {
            return true;
        }
        if (v.equalsIgnoreCase("false")) {
            return false;
        }
        try {
            if (v.matches("-?\\d+")) {
                long n = Long.parseLong(v);
                if (n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
                    return (int) n;
                }
                return n;
            }
            if (v.matches("-?\\d+\\.\\d+")) {
                return Double.parseDouble(v);
            }
        } catch (Exception ignored) {
        }
        return v;
    }

    private static UiUxMenusService ui() {
        return Bukkit.getServicesManager().load(UiUxMenusService.class);
    }
}

