package dev.darkblade.mbe.core.application.command.item;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemRequest;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;

import java.util.Map;

public class ItemCommand {

    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_ERROR_ITEM_CREATION_FAILED = MessageKey.of(ORIGIN, "commands.error.item_creation_failed");
    private static final MessageKey MSG_ITEM_GIVEN = MessageKey.of(ORIGIN, "commands.item.given");
    private static final MessageKey MSG_CONSOLE_NEEDS_PLAYER = MessageKey.of(ORIGIN, "commands.error.console_needs_player");

    private final MultiBlockEngine plugin;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;
    private final PlayerMessageService messageService;

    private final dev.darkblade.mbe.api.i18n.I18nService i18nService;

    public ItemCommand(MultiBlockEngine plugin, ItemService itemService, ItemStackBridge itemStackBridge, PlayerMessageService messageService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.itemStackBridge = itemStackBridge;
        this.messageService = messageService;
        this.i18nService = plugin.getAddonLifecycleService().getCoreService(dev.darkblade.mbe.api.i18n.I18nService.class);
    }

    @Command("mbe item give <item>")
    @Permission("multiblockengine.item.give")
    public void giveSelf(
            CommandSender sender,
            @Argument(value = "item", suggestions = "itemRequest") @org.incendo.cloud.annotation.specifier.Greedy String itemString
    ) {
        give(sender, itemString, null);
    }

    @Command("mbe item give to <target> <item>")
    @Permission("multiblockengine.item.give")
    public void giveOther(
            CommandSender sender,
            @Argument("target") Player targetArg,
            @Argument(value = "item", suggestions = "itemRequest") @org.incendo.cloud.annotation.specifier.Greedy String itemString
    ) {
        give(sender, itemString, targetArg);
    }

    private void give(CommandSender sender, String itemString, Player targetArg) {
        Player receiver = targetArg;
        if (receiver == null) {
            if (sender instanceof Player p) {
                receiver = p;
            } else {
                sendMessage(sender, MSG_CONSOLE_NEEDS_PLAYER, MessageUtils.params("label", "mbe item give"));
                return;
            }
        }
        
        ItemRequest request;
        try {
            request = new dev.darkblade.mbe.core.application.command.parser.ItemRequestParser<CommandSender>(itemService)
                    .parseString(itemString);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, dev.darkblade.mbe.api.i18n.MessageKey.of("mbe", "commands.error.invalid_argument"), MessageUtils.params("message", e.getMessage()));
            return;
        }

        ItemInstance instance;
        try {
            // Need to convert Map<Key, Object> to Map<String, Object> for ItemFactory
            Map<String, Object> stringKeyModifiers = new java.util.HashMap<>();
            request.parsedModifiers().forEach((k, v) -> {
                Object val = v;
                if (val instanceof net.kyori.adventure.key.Key keyVal) {
                    val = keyVal.asString();
                }
                stringKeyModifiers.put(k.asString(), val);
            });

            instance = itemService.factory().create(request.itemKey(), stringKeyModifiers);
        } catch (Exception e) {
            sendMessage(sender, MSG_ERROR_ITEM_CREATION_FAILED, Map.of());
            return;
        }

        ItemStack itemStack = itemStackBridge.toItemStack(instance, receiver);
        if (itemStack == null) {
            sendMessage(sender, MSG_ERROR_ITEM_CREATION_FAILED, Map.of());
            return;
        }

        receiver.getInventory().addItem(itemStack);
        sendMessage(sender, MSG_ITEM_GIVEN, MessageUtils.params("id", request.itemKey().id().toString(), "player", receiver.getName()));
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            if (i18nService != null) {
                i18nService.send(sender, key, params);
            }
        }
    }
}
