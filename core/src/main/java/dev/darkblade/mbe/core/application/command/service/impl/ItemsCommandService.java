package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.core.MultiBlockEngine;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
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

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

public final class ItemsCommandService {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_INFO_TITLE = MessageKey.of(ORIGIN, "services.items.info.title");
    private static final MessageKey MSG_INFO_DESCRIPTION = MessageKey.of(ORIGIN, "services.items.info.description");
    private static final MessageKey MSG_ITEM_SERVICE_UNAVAILABLE = MessageKey.of(ORIGIN, "services.items.error.item_service_unavailable");
    private static final MessageKey MSG_REGISTERED = MessageKey.of(ORIGIN, "services.items.info.registered");
    private static final MessageKey MSG_MISSING_SUBACTION = MessageKey.of(ORIGIN, "services.items.error.missing_subaction");
    private static final MessageKey MSG_INVALID_SUBACTION = MessageKey.of(ORIGIN, "services.items.error.invalid_subaction");
    private static final MessageKey MSG_LIST_ENTRY = MessageKey.of(ORIGIN, "services.items.list.entry");
    private static final MessageKey MSG_LIST_SUMMARY = MessageKey.of(ORIGIN, "services.items.list.summary");
    private static final MessageKey MSG_USAGE_GET = MessageKey.of(ORIGIN, "services.items.usage.get");
    private static final MessageKey MSG_USAGE_EXISTS = MessageKey.of(ORIGIN, "services.items.usage.exists");
    private static final MessageKey MSG_USAGE_GIVE = MessageKey.of(ORIGIN, "services.items.usage.give");
    private static final MessageKey MSG_USAGE_LIST = MessageKey.of(ORIGIN, "services.items.usage.list");
    private static final MessageKey MSG_NOT_EXISTS = MessageKey.of(ORIGIN, "services.items.error.not_exists");
    private static final MessageKey MSG_GET_ITEM = MessageKey.of(ORIGIN, "services.items.get.item");
    private static final MessageKey MSG_GET_NAME = MessageKey.of(ORIGIN, "services.items.get.name");
    private static final MessageKey MSG_GET_PROPERTIES = MessageKey.of(ORIGIN, "services.items.get.properties");
    private static final MessageKey MSG_EXISTS_OK = MessageKey.of(ORIGIN, "services.items.exists.ok");
    private static final MessageKey MSG_EXISTS_NO = MessageKey.of(ORIGIN, "services.items.exists.no");
    private static final MessageKey MSG_PLAYERS_ONLY = MessageKey.of(ORIGIN, "services.items.error.players_only");
    private static final MessageKey MSG_ITEM_STACK_BRIDGE_UNAVAILABLE = MessageKey.of(ORIGIN, "services.items.error.item_stack_bridge_unavailable");
    private static final MessageKey MSG_INVALID_AMOUNT = MessageKey.of(ORIGIN, "services.items.error.invalid_amount");
    private static final MessageKey MSG_AMOUNT_POSITIVE = MessageKey.of(ORIGIN, "services.items.error.amount_positive");
    private static final MessageKey MSG_CREATE_FAILED = MessageKey.of(ORIGIN, "services.items.error.create_failed");
    private static final MessageKey MSG_CONVERT_FAILED = MessageKey.of(ORIGIN, "services.items.error.convert_failed");
    private static final MessageKey MSG_INVALID_DELIVERY_ITEM = MessageKey.of(ORIGIN, "services.items.error.invalid_delivery_item");
    private static final MessageKey MSG_GIVE_SUCCESS = MessageKey.of(ORIGIN, "services.items.give.success");
    private static final MessageKey MSG_INVALID_KEY = MessageKey.of(ORIGIN, "services.items.error.invalid_key");

    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;
    private final I18nService i18n;
    private final PlayerMessageService messageService;

    public ItemsCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.itemService = plugin.getAddonLifecycleService().getCoreService(ItemService.class);
        this.itemStackBridge = plugin.getAddonLifecycleService().getCoreService(ItemStackBridge.class);
        this.i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        this.messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
    }



    @Command("mbe dev services items info")
    @Permission("multiblockengine.admin.services")
    public void info(dev.darkblade.mbe.core.application.command.MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        send(sender, MSG_INFO_TITLE);
        send(sender, MSG_INFO_DESCRIPTION);
        send(sender, MSG_USAGE_LIST);
        send(sender, MSG_USAGE_GET);
        send(sender, MSG_USAGE_EXISTS);
        send(sender, MSG_USAGE_GIVE);

        if (itemService == null) {
            send(sender, MSG_ITEM_SERVICE_UNAVAILABLE);
            return;
        }

        int total = itemService.registry().all().size();
        send(sender, MSG_REGISTERED, Map.of("count", total));
    }

    @org.incendo.cloud.annotations.suggestion.Suggestions("itemKeys")
    public List<String> suggestItemKeys(org.incendo.cloud.context.CommandContext<dev.darkblade.mbe.core.application.command.MBESender> context, String input) {
        if (itemService == null) {
            return List.of();
        }
        List<String> keys = new ArrayList<>();
        for (ItemDefinition def : itemService.registry().all()) {
            ItemKey k = def == null ? null : def.key();
            if (k != null) {
                keys.add(k.id().toString());
            }
        }
        keys.sort(String::compareToIgnoreCase);
        return keys;
    }

    @Command("mbe dev services items list [filter]")
    @Permission("multiblockengine.admin.services")
    public void executeList(dev.darkblade.mbe.core.application.command.MBESender mbeSender, @Argument("filter") String filter) {
        CommandSender sender = mbeSender.getSender();
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
                send(sender, MSG_LIST_ENTRY, Map.of("id", id, "version", def.key().version()));
                shown++;
            }
        }

        send(sender, MSG_LIST_SUMMARY, Map.of("matched", matched, "shown", shown));
    }

    @Command("mbe dev services items get <key> [version]")
    @Permission("multiblockengine.admin.services")
    public void executeGet(dev.darkblade.mbe.core.application.command.MBESender mbeSender, @Argument(value = "key", suggestions = "itemKeys") String key, @Argument("version") String version) {
        CommandSender sender = mbeSender.getSender();
        ParsedKey parsed = parseKey(key, version);
        if (!parsed.ok) {
            send(sender, MSG_INVALID_KEY);
            return;
        }

        if (!itemService.registry().exists(parsed.key)) {
            send(sender, MSG_NOT_EXISTS, Map.of("id", parsed.key.id().toString(), "version", parsed.key.version()));
            return;
        }

        ItemDefinition def = itemService.registry().get(parsed.key);
        send(sender, MSG_GET_ITEM, Map.of("id", def.key().id().toString(), "version", def.key().version()));
        send(sender, MSG_GET_NAME, Map.of("name", safe(def.displayName())));
        Map<String, Object> props = def.properties();
        send(sender, MSG_GET_PROPERTIES, Map.of("count", props == null ? 0 : props.size()));
    }

    @Command("mbe dev services items exists <key> [version]")
    @Permission("multiblockengine.admin.services")
    public void executeExists(dev.darkblade.mbe.core.application.command.MBESender mbeSender, @Argument(value = "key", suggestions = "itemKeys") String key, @Argument("version") String version) {
        CommandSender sender = mbeSender.getSender();
        ParsedKey parsed = parseKey(key, version);
        if (!parsed.ok) {
            send(sender, MSG_INVALID_KEY);
            return;
        }

        boolean exists = itemService.registry().exists(parsed.key);
        send(sender, exists ? MSG_EXISTS_OK : MSG_EXISTS_NO);
    }

    @Command("mbe dev services items give <key> [version] [amount]")
    @Permission("multiblockengine.admin.services")
    public void executeGive(dev.darkblade.mbe.core.application.command.MBESender mbeSender, @Argument(value = "key", suggestions = "itemKeys") String key, @Argument("version") String version, @Argument("amount") Integer amountArg) {
        CommandSender sender = mbeSender.getSender();
        if (!(sender instanceof Player player)) {
            send(sender, MSG_PLAYERS_ONLY);
            return;
        }

        if (itemStackBridge == null) {
            send(player, MSG_ITEM_STACK_BRIDGE_UNAVAILABLE);
            return;
        }

        ParsedKey parsed = parseKey(key, version);
        if (!parsed.ok) {
            send(player, MSG_INVALID_KEY);
            return;
        }

        if (!itemService.registry().exists(parsed.key)) {
            send(player, MSG_NOT_EXISTS, Map.of("id", parsed.key.id().toString(), "version", parsed.key.version()));
            return;
        }

        int amount = amountArg != null ? amountArg : 1;
        if (amount <= 0) {
            send(player, MSG_AMOUNT_POSITIVE);
            return;
        }

        ItemInstance instance;
        try {
            instance = itemService.factory().create(parsed.key);
        } catch (RuntimeException ex) {
            send(player, MSG_CREATE_FAILED);
            return;
        }

        if (instance == null) {
            send(player, MSG_CREATE_FAILED);
            return;
        }

        ItemStack proto;
        try {
            proto = itemStackBridge.toItemStack(instance, player);
        } catch (RuntimeException ex) {
            send(player, MSG_CONVERT_FAILED);
            return;
        }

        if (proto == null || proto.getType() == Material.AIR) {
            send(player, MSG_INVALID_DELIVERY_ITEM);
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

        send(player, MSG_GIVE_SUCCESS, Map.of(
                "amount", given,
                "id", parsed.key.id().toString(),
                "version", parsed.key.version()
        ));
    }

    private static ParsedKey parseKey(String namespacedId, String versionText) {
        try {
            int version = 0;
            if (versionText != null && !versionText.isBlank()) {
                version = Integer.parseInt(versionText);
            }
            ItemKey key = ItemKeys.of(namespacedId, Math.max(0, version));
            return new ParsedKey(true, key);
        } catch (Exception e) {
            return new ParsedKey(false, null);
        }
    }

    private void send(CommandSender sender, MessageKey key) {
        send(sender, key, Map.of());
    }

    private void send(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender == null || key == null) {
            return;
        }
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, params == null ? Map.of() : params));
            return;
        }
        if (i18n != null) {
            i18n.send(sender, key, params == null ? Map.of() : params);
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    private record ParsedKey(boolean ok, ItemKey key) {
    }
}
