package dev.darkblade.mbe.api.i18n;

import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.UUID;

public interface LocaleProvider {

    Locale localeOf(CommandSender sender);

    Locale localeOf(UUID playerId);

    default Locale fallbackLocale() {
        return Locale.forLanguageTag("en-US");
    }
}

