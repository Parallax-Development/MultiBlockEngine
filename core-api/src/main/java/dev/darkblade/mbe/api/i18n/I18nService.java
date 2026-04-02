package dev.darkblade.mbe.api.i18n;

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

    void reload();
}

