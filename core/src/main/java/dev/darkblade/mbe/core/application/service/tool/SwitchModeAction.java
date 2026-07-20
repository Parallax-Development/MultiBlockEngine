package dev.darkblade.mbe.core.application.service.tool;

import dev.darkblade.mbe.api.command.WrenchContext;
import dev.darkblade.mbe.api.command.WrenchResult;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.message.CoreMessageKeys;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.tool.ActionId;
import dev.darkblade.mbe.api.tool.Tool;
import dev.darkblade.mbe.api.tool.ToolAction;
import dev.darkblade.mbe.api.tool.ToolMode;
import dev.darkblade.mbe.api.tool.ToolRegistry;
import dev.darkblade.mbe.api.tool.ToolState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class SwitchModeAction implements ToolAction {

    private static final MessageKey MSG_SWITCHED = CoreMessageKeys.TOOL_MODE_SWITCHED;

    private final ToolStateResolver stateResolver;
    private final ToolRegistry toolRegistry;
    private final PlayerMessageService messageService;
    private final dev.darkblade.mbe.api.i18n.I18nService i18nService;

    public SwitchModeAction(ToolStateResolver stateResolver, ToolRegistry toolRegistry, PlayerMessageService messageService, dev.darkblade.mbe.api.i18n.I18nService i18nService) {
        this.stateResolver = Objects.requireNonNull(stateResolver, "stateResolver");
        this.toolRegistry = Objects.requireNonNull(toolRegistry, "toolRegistry");
        this.messageService = messageService;
        this.i18nService = i18nService;
    }

    @Override
    public ActionId id() {
        return WrenchActions.SWITCH_MODE;
    }

    @Override
    public WrenchResult execute(WrenchContext context) {
        ToolState state = stateResolver.resolve(context.item());
        if (state == null) {
            return WrenchResult.noop();
        }
        Tool tool = toolRegistry.get(state.toolId());
        if (tool == null) {
            return WrenchResult.noop();
        }
        List<ToolMode> modes = new ArrayList<>(tool.modes());
        if (modes.isEmpty()) {
            return WrenchResult.noop();
        }
        String current = normalize(state.modeId());
        int currentIndex = 0;
        for (int i = 0; i < modes.size(); i++) {
            if (normalize(modes.get(i).id()).equals(current)) {
                currentIndex = i;
                break;
            }
        }
        ToolMode nextMode = modes.get((currentIndex + 1) % modes.size());
        ToolState nextState = new ToolState(tool.id(), nextMode.id());
        stateResolver.save(context.item(), nextState);
        
        updateItemLore(context.item(), tool, nextMode, context.player());
        
        if (messageService != null && context.player() != null) {
            String translatedMode = getTranslatedMode(nextMode.id(), context.player());
            messageService.send(context.player(), new PlayerMessage(
                    MSG_SWITCHED,
                    MessageChannel.ACTION_BAR,
                    MessagePriority.LOW,
                    Map.of("mode", translatedMode)
            ));
        }
        return WrenchResult.success(MSG_SWITCHED.path(), Map.of("mode", nextMode.id()));
    }

    private String getTranslatedMode(String modeId, org.bukkit.command.CommandSender sender) {
        if (i18nService == null) return modeId;
        Locale locale = i18nService.localeProvider() != null ? i18nService.localeProvider().localeOf(sender) : Locale.US;
        MessageKey key = MessageKey.of("mbe", "core.tool.mode." + modeId);
        String translated = i18nService.resolve(key, locale, Map.of());
        if (translated != null && !translated.isEmpty() && !translated.equals(key.path())) {
            return translated;
        }
        return modeId;
    }
    
    private void updateItemLore(org.bukkit.inventory.ItemStack item, Tool tool, ToolMode mode, org.bukkit.entity.Player player) {
        if (item == null || !item.hasItemMeta() || i18nService == null) return;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        
        Locale locale = i18nService.localeProvider() != null ? i18nService.localeProvider().localeOf(player) : Locale.US;
        
        String toolId = tool.id();
        String namespace = "mbe";
        String value = toolId;
        if (toolId.contains(":")) {
            String[] split = toolId.split(":");
            namespace = split[0];
            value = split[1];
        }
        
        MessageKey loreKey = MessageKey.of(namespace, "core.items." + value + ".lore.mode");
        
        String translatedMode = getTranslatedMode(mode.id(), player);
        String translatedLoreLine = i18nService.resolve(loreKey, locale, Map.of("mode", translatedMode));
        
        if (translatedLoreLine != null && !translatedLoreLine.equals(loreKey.path())) {
            String formattedLine = org.bukkit.ChatColor.translateAlternateColorCodes('&', translatedLoreLine);
            if (!lore.isEmpty()) {
                lore.set(0, formattedLine);
            } else {
                lore.add(formattedLine);
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
