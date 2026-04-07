package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.internal.tooling.PlayerResolver;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public record SendMessageAction(MessageKey key, Map<String, Object> params, Object targetSelector) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player contextPlayer) {
        Collection<Player> players = PlayerResolver.resolve(targetSelector, instance, contextPlayer);
        if (players.isEmpty() || key == null) {
            return;
        }
        MultiBlockEngine plugin = MultiBlockEngine.getInstance();
        if (plugin == null || plugin.getAddonLifecycleService() == null) {
            return;
        }
        I18nService i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        if (i18n == null) {
            return;
        }
        boolean hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

        for (Player p : players) {
            Map<String, Object> resolvedParams = resolveParams(instance);
            String processed = i18n.tr(p, key, resolvedParams);
            if (isMissingTranslation(processed)) {
                String fallback = literalFallback();
                if (fallback.isBlank()) {
                    continue;
                }
                processed = fallback;
            }
            if (processed == null || processed.isBlank()) {
                continue;
            }
            if (hasPapi) {
                processed = PlaceholderAPI.setPlaceholders(p, processed);
            }
            p.sendMessage(StringUtil.legacyText(processed));
        }
    }

    private boolean isMissingTranslation(String value) {
        if (value == null || value.isBlank() || key == null) {
            return true;
        }
        String full = key.fullKey();
        return value.equals(full) || value.equals("??" + full + "??");
    }

    private String literalFallback() {
        String path = key == null || key.path() == null ? "" : key.path().trim();
        if (path.isBlank()) {
            return "";
        }
        if (path.matches("[a-z0-9_.-]+")) {
            return "";
        }
        return path;
    }

    private Map<String, Object> resolveParams(MultiblockInstance instance) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String text) {
                out.put(entry.getKey(), StringUtil.parsePlaceholders(text, instance));
                continue;
            }
            out.put(entry.getKey(), value);
        }
        return Map.copyOf(out);
    }
}
