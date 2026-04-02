package dev.darkblade.mbe.api.logging;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.i18n.MessageResolver;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MBEException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, ?> params;

    public MBEException(ErrorCode code) {
        this(code, null, Map.of());
    }

    public MBEException(ErrorCode code, Map<String, ?> params) {
        this(code, null, params);
    }

    public MBEException(ErrorCode code, Throwable cause) {
        this(code, cause, Map.of());
    }

    public MBEException(ErrorCode code, Throwable cause, Map<String, ?> params) {
        super(code == null ? "unknown" : code.id(), cause);
        this.code = code;
        this.params = params == null ? Map.of() : Map.copyOf(params);
    }

    public ErrorCode code() {
        return code;
    }

    public Map<String, ?> params() {
        return params;
    }

    public MessageKey messageKey() {
        return code == null ? MessageKey.of("unknown", "unknown") : Objects.requireNonNullElse(code.messageKey(), MessageKey.of("unknown", "unknown"));
    }

    public String resolve(MessageResolver resolver, Locale locale) {
        try {
            if (resolver == null) {
                return code == null ? "unknown" : code.id();
            }
            Locale safeLocale = locale == null ? Locale.forLanguageTag("en-US") : locale;
            return resolver.resolve(messageKey(), safeLocale, params);
        } catch (Throwable t) {
            return code == null ? "unknown" : code.id();
        }
    }

    public String resolve(I18nService i18n, CommandSender sender) {
        try {
            if (i18n == null) {
                return code == null ? "unknown" : code.id();
            }
            return i18n.tr(sender, messageKey(), params);
        } catch (Throwable t) {
            return code == null ? "unknown" : code.id();
        }
    }
}

