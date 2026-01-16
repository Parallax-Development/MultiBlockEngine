package com.darkbladedev.engine.i18n;

import java.util.Locale;

final class LocaleParsing {

    private LocaleParsing() {
    }

    static Locale parseMinecraftLocale(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim();
        v = v.replace('-', '_');
        String[] parts = v.split("_", 3);
        if (parts.length == 0) {
            return null;
        }
        String lang = parts[0].toLowerCase(Locale.ROOT);
        if (lang.isBlank()) {
            return null;
        }
        String country = parts.length >= 2 ? parts[1].toUpperCase(Locale.ROOT) : "";
        if (country.isBlank()) {
            return new Locale(lang);
        }
        return new Locale(lang, country);
    }

    static String toLocaleKey(Locale locale) {
        if (locale == null) {
            return "en_us";
        }
        String lang = locale.getLanguage();
        if (lang == null || lang.isBlank()) {
            return "en_us";
        }
        lang = lang.toLowerCase(Locale.ROOT);
        String country = locale.getCountry();
        if (country == null || country.isBlank()) {
            return lang;
        }
        return (lang + "_" + country.toLowerCase(Locale.ROOT));
    }
}

