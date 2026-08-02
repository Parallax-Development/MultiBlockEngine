package dev.darkblade.mbe.api.i18n;

import java.util.Locale;
import java.util.Map;

public interface MessageResolver {

    String resolve(MessageKey key, Locale locale);

    String resolve(MessageKey key, Locale locale, Map<String, ?> params);

    default String resolve(MessageKey key, Locale locale, Object... params) {
        return resolve(key, locale, MessageUtils.params(params));
    }

    default java.util.List<String> resolveList(MessageKey key, Locale locale) {
        return resolveList(key, locale, Map.of());
    }

    default java.util.List<String> resolveList(MessageKey key, Locale locale, Map<String, ?> params) {
        String res = resolve(key, locale, params);
        return res == null || res.isEmpty() ? java.util.List.of() : java.util.List.of(res);
    }

    default java.util.List<String> resolveList(MessageKey key, Locale locale, Object... params) {
        return resolveList(key, locale, MessageUtils.params(params));
    }
}

