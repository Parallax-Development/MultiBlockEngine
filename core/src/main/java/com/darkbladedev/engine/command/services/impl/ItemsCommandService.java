package com.darkbladedev.engine.command.services.impl;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.command.MbeCommandService;
import com.darkbladedev.engine.api.item.ItemDefinition;
import com.darkbladedev.engine.api.item.ItemInstance;
import com.darkbladedev.engine.api.item.ItemKey;
import com.darkbladedev.engine.api.item.ItemKeys;
import com.darkbladedev.engine.api.item.ItemService;
import com.darkbladedev.engine.item.bridge.ItemStackBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ItemsCommandService implements MbeCommandService {

    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;

    public ItemsCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.itemService = plugin.getAddonManager().getCoreService(ItemService.class);
        this.itemStackBridge = plugin.getAddonManager().getCoreService(ItemStackBridge.class);
    }

    @Override
    public String id() {
        return "items";
    }

    @Override
    public String description() {
        return "Consulta y diagnóstico del registro de items";
    }

    @Override
    public List<String> infoUsage() {
        return List.of(
                "/mbe services call items info",
                "/mbe services call items info <list|get|exists>"
        );
    }

    @Override
    public List<String> executeUsage() {
        return List.of(
                "/mbe services call items execute list [filtro]",
                "/mbe services call items execute get <namespace:key> [version]",
                "/mbe services call items execute exists <namespace:key> [version]",
                "/mbe services call items execute give <namespace:key> [version] [cantidad]"
        );
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        sender.sendMessage(Component.text("Servicio: items", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(description(), NamedTextColor.GRAY));
        for (String line : executeUsage()) {
            sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }

        if (itemService == null) {
            sender.sendMessage(Component.text("ItemService no disponible.", NamedTextColor.RED));
            return;
        }

        int total = itemService.registry().all().size();
        sender.sendMessage(Component.text("Registrados: " + total, NamedTextColor.GRAY));
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        if (itemService == null) {
            sender.sendMessage(Component.text("ItemService no disponible.", NamedTextColor.RED));
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
            case "list" -> executeList(sender, args.size() >= 2 ? args.get(1) : "");
            case "get" -> executeGet(sender, args);
            case "exists" -> executeExists(sender, args);
            case "give" -> executeGive(sender, args);
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
        if (!m.equals("execute")) {
            return List.of();
        }

        if (args == null) {
            return List.of();
        }

        if (args.size() <= 1) {
            return List.of("list", "get", "exists", "give");
        }

        String sub = args.get(0).toLowerCase(Locale.ROOT);
        if ((sub.equals("get") || sub.equals("exists") || sub.equals("give")) && args.size() == 2) {
            if (itemService == null) {
                return List.of();
            }

            List<String> keys = new ArrayList<>();
            for (ItemDefinition def : itemService.registry().all()) {
                ItemKey k = def == null ? null : def.key();
                if (k != null) {
                    keys.add(k.id().toString());
                }
                if (keys.size() >= 100) {
                    break;
                }
            }
            keys.sort(String::compareToIgnoreCase);
            return List.copyOf(keys);
        }

        if (sub.equals("give") && args.size() == 4) {
            return List.of("1", "64");
        }

        return List.of();
    }

    private void executeList(CommandSender sender, String filter) {
        String f = filter == null ? "" : filter.toLowerCase(Locale.ROOT);
        List<ItemDefinition> defs = new ArrayList<>(itemService.registry().all());
        defs.sort(Comparator.comparing(d -> d.key().id().toString()));

        int shown = 0;
        int matched = 0;
        for (ItemDefinition def : defs) {
            if (def == null || def.key() == null) {
                continue;
            }
            String id = def.key().id().toString();
            if (!f.isEmpty() && !id.toLowerCase(Locale.ROOT).contains(f)) {
                continue;
            }
            matched++;
            if (shown < 50) {
                sender.sendMessage(Component.text("- " + id + " v" + def.key().version(), NamedTextColor.GRAY));
                shown++;
            }
        }

        sender.sendMessage(Component.text("Coincidencias: " + matched + (matched > shown ? " (mostrando " + shown + ")" : ""), NamedTextColor.YELLOW));
    }

    private void executeGet(CommandSender sender, List<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(Component.text("Uso: /mbe services call items execute get <namespace:key> [version]", NamedTextColor.RED));
            return;
        }

        ParsedKey parsed = parseKey(args.get(1), args.size() >= 3 ? args.get(2) : null);
        if (!parsed.ok) {
            sender.sendMessage(Component.text(parsed.error, NamedTextColor.RED));
            return;
        }

        if (!itemService.registry().exists(parsed.key)) {
            sender.sendMessage(Component.text("No existe: " + parsed.key.id() + " v" + parsed.key.version(), NamedTextColor.RED));
            return;
        }

        ItemDefinition def = itemService.registry().get(parsed.key);
        sender.sendMessage(Component.text("Item: " + def.key().id() + " v" + def.key().version(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Nombre: " + safe(def.displayName()), NamedTextColor.GRAY));
        Map<String, Object> props = def.properties();
        sender.sendMessage(Component.text("Propiedades: " + (props == null ? 0 : props.size()), NamedTextColor.GRAY));
    }

    private void executeExists(CommandSender sender, List<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(Component.text("Uso: /mbe services call items execute exists <namespace:key> [version]", NamedTextColor.RED));
            return;
        }

        ParsedKey parsed = parseKey(args.get(1), args.size() >= 3 ? args.get(2) : null);
        if (!parsed.ok) {
            sender.sendMessage(Component.text(parsed.error, NamedTextColor.RED));
            return;
        }

        boolean exists = itemService.registry().exists(parsed.key);
        sender.sendMessage(Component.text(exists ? "OK" : "NO", exists ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void executeGive(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores.", NamedTextColor.RED));
            return;
        }

        if (itemStackBridge == null) {
            player.sendMessage(Component.text("ItemStackBridge no disponible.", NamedTextColor.RED));
            return;
        }

        if (args.size() < 2) {
            player.sendMessage(Component.text("Uso: /mbe services call items execute give <namespace:key> [version] [cantidad]", NamedTextColor.RED));
            return;
        }

        ParsedKey parsed = parseKey(args.get(1), args.size() >= 3 ? args.get(2) : null);
        if (!parsed.ok) {
            player.sendMessage(Component.text(parsed.error, NamedTextColor.RED));
            return;
        }

        if (!itemService.registry().exists(parsed.key)) {
            player.sendMessage(Component.text("No existe: " + parsed.key.id() + " v" + parsed.key.version(), NamedTextColor.RED));
            return;
        }

        int amount = 1;
        if (args.size() >= 4) {
            try {
                amount = Integer.parseInt(args.get(3));
            } catch (NumberFormatException ex) {
                player.sendMessage(Component.text("Cantidad inválida.", NamedTextColor.RED));
                return;
            }
        }
        if (amount <= 0) {
            player.sendMessage(Component.text("Cantidad debe ser > 0.", NamedTextColor.RED));
            return;
        }

        ItemInstance instance;
        try {
            instance = itemService.factory().create(parsed.key);
        } catch (RuntimeException ex) {
            player.sendMessage(Component.text("No se pudo crear el item.", NamedTextColor.RED));
            return;
        }

        if (instance == null) {
            player.sendMessage(Component.text("No se pudo crear el item.", NamedTextColor.RED));
            return;
        }

        ItemStack proto;
        try {
            proto = itemStackBridge.toItemStack(instance);
        } catch (RuntimeException ex) {
            player.sendMessage(Component.text("No se pudo convertir el item.", NamedTextColor.RED));
            return;
        }

        if (proto == null || proto.getType() == Material.AIR) {
            player.sendMessage(Component.text("Item inválido para entrega.", NamedTextColor.RED));
            return;
        }

        int remaining = amount;
        int given = 0;

        while (remaining > 0) {
            int maxStack = Math.max(1, proto.getMaxStackSize());
            int toGive = Math.min(remaining, maxStack);

            ItemStack stack = proto.clone();
            stack.setAmount(toGive);

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (overflow != null && !overflow.isEmpty()) {
                for (ItemStack extra : overflow.values()) {
                    if (extra == null || extra.getType() == Material.AIR || extra.getAmount() <= 0) {
                        continue;
                    }
                    if (player.getWorld() != null) {
                        player.getWorld().dropItemNaturally(player.getLocation(), extra);
                    }
                }
            }

            given += toGive;
            remaining -= toGive;
        }

        player.sendMessage(Component.text("Entregado: " + given + " x " + parsed.key.id() + " v" + parsed.key.version(), NamedTextColor.GREEN));
    }

    private static ParsedKey parseKey(String namespacedId, String versionText) {
        try {
            int version = 0;
            if (versionText != null && !versionText.isBlank()) {
                version = Integer.parseInt(versionText);
            }
            ItemKey key = ItemKeys.of(namespacedId, Math.max(0, version));
            return new ParsedKey(true, key, "");
        } catch (Exception e) {
            return new ParsedKey(false, null, "ItemKey inválido. Usa: <namespace:key> [version]");
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private record ParsedKey(boolean ok, ItemKey key, String error) {
    }
}
