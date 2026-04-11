package dev.darkblade.mbe.api.i18n;

import java.util.Locale;
import java.util.Map;

public interface MessageResolver {

    String resolve(MessageKey key, Locale locale);

    String resolve(MessageKey key, Locale locale, Map<String, ?> params);

    default String resolve(MessageKey key, Locale locale, Object... params) {
        return resolve(key, locale, MessageUtils.params(params));
    }
}

