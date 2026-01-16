package com.darkbladedev.engine.i18n;

import com.darkbladedev.engine.api.i18n.LocaleProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class BukkitLocaleProvider implements LocaleProvider {

    private final Locale fallback;

    public BukkitLocaleProvider(Locale fallback) {
        this.fallback = fallback == null ? Locale.forLanguageTag("en-US") : fallback;
    }

    @Override
    public Locale localeOf(CommandSender sender) {
        try {
            if (sender instanceof Player player) {
                Locale locale = player.locale();
                return locale == null ? fallback : locale;
            }
            return fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    @Override
    public Locale localeOf(UUID playerId) {
        try {
            if (playerId == null) {
                return fallback;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return fallback;
            }
            Locale locale = player.locale();
            return locale == null ? fallback : locale;
        } catch (Throwable t) {
            return fallback;
        }
    }

    @Override
    public Locale fallbackLocale() {
        return fallback;
    }
}
