package dev.darkblade.mbe.core.infrastructure.i18n;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class I18nPlaceholderResolver {

    private static final Pattern I18N_PLACEHOLDER_PATTERN = Pattern.compile("%i18n:([a-zA-Z0-9_.-]+)(?::([a-zA-Z0-9_.-]+))?%");

    private final I18nService i18nService;

    public I18nPlaceholderResolver(I18nService i18nService) {
        this.i18nService = Objects.requireNonNull(i18nService, "i18nService");
    }

    public String resolveInlinePlaceholders(String text, CommandSender sender, String defaultOrigin) {
        return resolveInlinePlaceholders(text, sender, defaultOrigin, Map.of());
    }

    public String resolveInlinePlaceholders(String text, CommandSender sender, String defaultOrigin, Map<String, ?> params) {
        if (text == null || text.isBlank() || !text.contains("%i18n:")) {
            return text == null ? "" : text;
        }

        String fallbackOrigin = defaultOrigin == null || defaultOrigin.isBlank() ? YamlI18nService.CORE_ORIGIN : defaultOrigin;
        Matcher matcher = I18N_PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String part1 = matcher.group(1);
            String part2 = matcher.group(2);

            String origin;
            String path;

            if (part2 != null && !part2.isBlank()) {
                origin = part1;
                path = part2;
            } else {
                origin = fallbackOrigin;
                path = part1;
            }

            MessageKey key = MessageKey.of(origin, path);
            String resolved = i18nService.tr(sender, key, params);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
