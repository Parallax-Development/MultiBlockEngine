package dev.darkblade.mbe.core.application.command.blueprint;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.i18n.MessageUtils;
import dev.darkblade.mbe.blueprint.BlueprintItem;
import dev.darkblade.mbe.catalog.StructureCatalogService;
import dev.darkblade.mbe.core.application.command.MBESender;
import dev.darkblade.mbe.core.application.command.service.impl.BlueprintCommandService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlueprintCommand {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_BLUEPRINT_AVAILABLE_IDS = MessageKey.of(ORIGIN, "commands.blueprint.available_ids");
    private static final MessageKey MSG_ERROR_ITEM_CREATION_FAILED = MessageKey.of(ORIGIN, "commands.error.item_creation_failed");

    private final BlueprintCommandService blueprintCommands;
    private final StructureCatalogService catalogService;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;
    private final PlayerMessageService messageService;

    public BlueprintCommand(
            BlueprintCommandService blueprintCommands,
            StructureCatalogService catalogService,
            ItemService itemService,
            ItemStackBridge itemStackBridge,
            PlayerMessageService messageService
    ) {
        this.blueprintCommands = blueprintCommands;
        this.catalogService = catalogService;
        this.itemService = itemService;
        this.itemStackBridge = itemStackBridge;
        this.messageService = messageService;
    }

    @Command("mbe catalog")
    @Permission("multiblockengine.catalog")
    public void catalog(MBESender mbeSender) {
        if (mbeSender.getSender() instanceof Player player) {
            blueprintCommands.openCatalog(player);
        }
    }

    @Command("mbe blueprint list")
    @Permission("multiblockengine.blueprint")
    public void list(MBESender mbeSender) {
        CommandSender sender = mbeSender.getSender();
        List<String> ids = new ArrayList<>();
        for (MultiblockDefinition definition : catalogService.getAll()) {
            if (definition != null && definition.id() != null && !definition.id().isBlank()) {
                ids.add(definition.id());
            }
        }
        if (ids.isEmpty()) {
            sendMessage(sender, CoreMessageKeys.BLUEPRINT_NONE_LOADED, Map.of());
            return;
        }
        Collections.sort(ids);
        sendMessage(sender, CoreMessageKeys.BLUEPRINT_AVAILABLE_TITLE, MessageUtils.params("count", ids.size()));
        sendMessage(sender, MSG_BLUEPRINT_AVAILABLE_IDS, MessageUtils.params("ids", String.join(", ", ids)));
    }

    @Command("mbe blueprint give <id> [target]")
    @Permission("multiblockengine.blueprint")
    public void give(MBESender mbeSender, @Argument("id") String id, @Argument("target") Player targetArg) {
        CommandSender sender = mbeSender.getSender();
        Player receiver = targetArg;

        if (receiver == null) {
            if (sender instanceof Player p) {
                receiver = p;
            } else {
                sendMessage(sender, CoreMessageKeys.BLUEPRINT_CONSOLE_NEEDS_PLAYER, MessageUtils.params("label", "mbe blueprint give"));
                return;
            }
        }

        MultiblockDefinition definition = null;
        for (MultiblockDefinition candidate : catalogService.getAll()) {
            if (candidate != null && candidate.id() != null && candidate.id().equalsIgnoreCase(id)) {
                definition = candidate;
                break;
            }
        }

        if (definition == null) {
            sendMessage(sender, CoreMessageKeys.BLUEPRINT_NOT_FOUND, MessageUtils.params("id", id));
            return;
        }

        ItemStack blueprint = BlueprintItem.create(itemService, itemStackBridge, definition, receiver);
        if (blueprint == null) {
            sendMessage(sender, MSG_ERROR_ITEM_CREATION_FAILED, Map.of());
            return;
        }
        receiver.getInventory().addItem(blueprint);
        sendMessage(sender, CoreMessageKeys.BLUEPRINT_GIVEN, MessageUtils.params("id", id, "player", receiver.getName()));
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            org.bukkit.Bukkit.getLogger().info(key.fullKey() + " " + params.toString());
        }
    }
}
