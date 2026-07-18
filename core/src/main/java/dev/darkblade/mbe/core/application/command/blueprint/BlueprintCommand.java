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
import dev.darkblade.mbe.core.application.command.MBECommandManager;
import dev.darkblade.mbe.core.application.command.service.impl.BlueprintCommandService;
import dev.darkblade.mbe.core.infrastructure.bridge.item.ItemStackBridge;
import dev.darkblade.mbe.preview.MultiblockDefinition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BlueprintCommand {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_BLUEPRINT_AVAILABLE_IDS = MessageKey.of(ORIGIN, "commands.blueprint.available_ids");
    private static final MessageKey MSG_ERROR_ITEM_CREATION_FAILED = MessageKey.of(ORIGIN, "commands.error.item_creation_failed");

    private final MBECommandManager manager;
    private final BlueprintCommandService blueprintCommands;
    private final StructureCatalogService catalogService;
    private final ItemService itemService;
    private final ItemStackBridge itemStackBridge;
    private final PlayerMessageService messageService;

    public BlueprintCommand(
            MBECommandManager manager,
            BlueprintCommandService blueprintCommands,
            StructureCatalogService catalogService,
            ItemService itemService,
            ItemStackBridge itemStackBridge,
            PlayerMessageService messageService
    ) {
        this.manager = manager;
        this.blueprintCommands = blueprintCommands;
        this.catalogService = catalogService;
        this.itemService = itemService;
        this.itemStackBridge = itemStackBridge;
        this.messageService = messageService;
    }

    public void register() {
        Command.Builder<dev.darkblade.mbe.core.application.command.MBESender> builder = manager.commandBuilder("mbe", "multiblock")
                .literal("blueprint")
                .permission("multiblockengine.blueprint");

        manager.command(builder.literal("catalog")
                .handler(context -> {
                    if (context.sender().isPlayer()) {
                        blueprintCommands.openCatalog(context.sender().getPlayer());
                    }
                })
        );

        manager.command(builder.literal("list")
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
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
                })
        );

        manager.command(builder.literal("give")
                .required("id", StringParser.stringParser())
                .optional("target", org.incendo.cloud.bukkit.parser.PlayerParser.playerParser())
                .handler(context -> {
                    CommandSender sender = context.sender().getSender();
                    String id = context.get("id");
                    Player receiver = context.getOrDefault("target", null);

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
                })
        );
    }

    private void sendMessage(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender instanceof Player p) {
            messageService.send(p, new PlayerMessage(key, MessageChannel.SYSTEM, MessagePriority.NORMAL, params));
        } else {
            // For console, just send the raw key for now
            org.bukkit.Bukkit.getLogger().info(key.fullKey() + " " + params.toString());
        }
    }
}
