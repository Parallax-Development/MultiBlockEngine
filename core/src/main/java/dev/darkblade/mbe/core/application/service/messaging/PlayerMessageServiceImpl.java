package dev.darkblade.mbe.core.application.service.messaging;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.service.InjectService;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMessageServiceImpl implements PlayerMessageService {
    private static final long ACTION_BAR_DEBOUNCE_MS = 500L;

    @InjectService
    private I18nService i18n;

    private final Map<UUID, ActionBarState> actionBarStates = new ConcurrentHashMap<>();

    public PlayerMessageServiceImpl() {
    }

    public PlayerMessageServiceImpl(I18nService i18n) {
        this.i18n = i18n;
    }

    @Override
    public String getServiceId() {
        return "mbe:player_message";
    }

    @Override
    public void send(Player player, PlayerMessage message) {
        if (player == null || message == null || message.key() == null || i18n == null) {
            return;
        }
        String resolved = i18n.tr(player, message.key(), safePlaceholders(message.placeholders()));
        if (isMissingTranslation(resolved, message.key())) {
            String fallback = literalFallback(message.key());
            if (fallback.isBlank()) {
                return;
            }
            resolved = fallback;
        }
        if (resolved == null || resolved.isBlank()) {
            return;
        }
        if (isPlaceholderApiPresent()) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        MessageChannel channel = resolveChannel(message);
        switch (channel) {
            case CHAT, SYSTEM -> player.sendMessage(StringUtil.legacyText(resolved));
            case ACTION_BAR -> sendActionBar(player, resolved);
            case TITLE -> player.showTitle(Title.title(StringUtil.legacyText(resolved), Component.empty()));
            case SUBTITLE -> player.showTitle(Title.title(Component.empty(), StringUtil.legacyText(resolved)));
        }
    }

    @Override
    public void onDisable() {
        actionBarStates.clear();
    }

    private MessageChannel resolveChannel(PlayerMessage message) {
        if (message.channel() != null) {
            return message.channel();
        }
        MessagePriority priority = message.priority() == null ? MessagePriority.NORMAL : message.priority();
        return switch (priority) {
            case LOW -> MessageChannel.ACTION_BAR;
            case NORMAL, HIGH -> MessageChannel.CHAT;
            case CRITICAL -> MessageChannel.TITLE;
        };
    }

    private void sendActionBar(Player player, String message) {
        ActionBarState state = actionBarStates.computeIfAbsent(player.getUniqueId(), ignored -> new ActionBarState());
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (message.equals(state.lastMessage) && (now - state.lastTimestamp) < ACTION_BAR_DEBOUNCE_MS) {
                return;
            }
            state.lastMessage = message;
            state.lastTimestamp = now;
        }
        player.sendActionBar(StringUtil.legacyText(message));
    }

    private boolean isPlaceholderApiPresent() {
        try {
            return Bukkit.getServer() != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Map<String, Object> safePlaceholders(Map<String, Object> placeholders) {
        return placeholders == null || placeholders.isEmpty() ? Map.of() : placeholders;
    }

    private boolean isMissingTranslation(String value, MessageKey key) {
        if (value == null || value.isBlank() || key == null) {
            return true;
        }
        String full = key.fullKey();
        return value.equals(full) || value.equals("??" + full + "??");
    }

    private String literalFallback(MessageKey key) {
        if (key == null || key.path() == null) {
            return "";
        }
        String path = key.path().trim();
        if (path.isBlank()) {
            return "";
        }
        if (path.matches("[a-z0-9_.-]+")) {
            return "";
        }
        return path;
    }

    private static final class ActionBarState {
        private String lastMessage;
        private long lastTimestamp;
    }
}
