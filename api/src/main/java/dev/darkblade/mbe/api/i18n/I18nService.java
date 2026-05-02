package dev.darkblade.mbe.api.i18n;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;

public interface I18nService extends MessageResolver {

    LocaleProvider localeProvider();

    default String tr(CommandSender sender, MessageKey key) {
        Locale locale = localeProvider().localeOf(sender);
        return resolve(key, locale);
    }

    default String tr(CommandSender sender, MessageKey key, Map<String, ?> params) {
        Locale locale = localeProvider().localeOf(sender);
        return resolve(key, locale, params);
    }

    default String tr(CommandSender sender, MessageKey key, Object... params) {
        Locale locale = localeProvider().localeOf(sender);
        return resolve(key, locale, MessageUtils.params(params));
    }

    default String resolve(MessageKey key, CommandSender sender) {
        return tr(sender, key);
    }

    default String resolve(MessageKey key, CommandSender sender, Map<String, ?> params) {
        return tr(sender, key, params);
    }

    default void send(CommandSender sender, MessageKey key) {
        if (sender == null || key == null) {
            return;
        }
        String message = tr(sender, key);
        if (message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    default void send(CommandSender sender, MessageKey key, Map<String, ?> params) {
        if (sender == null || key == null) {
            return;
        }
        String message = params == null || params.isEmpty() ? tr(sender, key) : tr(sender, key, params);
        if (message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    void reload();
}
