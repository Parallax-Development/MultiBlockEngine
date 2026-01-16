package com.darkbladedev.engine.i18n;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MessageTemplate {

    private final String[] literals;
    private final String[] keys;

    private MessageTemplate(String[] literals, String[] keys) {
        this.literals = literals;
        this.keys = keys;
    }

    static MessageTemplate compile(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new MessageTemplate(new String[] { "" }, new String[0]);
        }

        int len = raw.length();
        List<String> literals = new ArrayList<>();
        List<String> keys = new ArrayList<>();

        StringBuilder cur = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            char c = raw.charAt(i);
            if (c != '{') {
                cur.append(c);
                i++;
                continue;
            }

            int close = raw.indexOf('}', i + 1);
            if (close < 0) {
                cur.append(c);
                i++;
                continue;
            }

            String inside = raw.substring(i + 1, close);
            String key = normalizePlaceholder(inside);
            if (key == null) {
                cur.append(raw, i, close + 1);
                i = close + 1;
                continue;
            }

            literals.add(cur.toString());
            cur.setLength(0);
            keys.add(key);
            i = close + 1;
        }

        literals.add(cur.toString());

        if (keys.isEmpty()) {
            return new MessageTemplate(new String[] { raw }, new String[0]);
        }

        return new MessageTemplate(literals.toArray(String[]::new), keys.toArray(String[]::new));
    }

    String render(Locale locale, Map<String, ?> params) {
        if (keys.length == 0) {
            return literals.length == 0 ? "" : literals[0];
        }
        Map<String, ?> safeParams = params == null ? Map.of() : params;
        Locale safeLocale = locale == null ? Locale.ROOT : locale;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            out.append(literals[i]);
            String k = keys[i];
            Object v = safeParams.get(k);
            if (v == null) {
                out.append('{').append(k).append('}');
            } else {
                out.append(formatValue(safeLocale, v));
            }
        }
        out.append(literals[literals.length - 1]);
        return out.toString();
    }

    String render(Map<String, ?> params) {
        return render(Locale.ROOT, params);
    }

    private static String formatValue(Locale locale, Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Number n) {
            try {
                NumberFormat nf = NumberFormat.getInstance(locale);
                return nf.format(n);
            } catch (Throwable ignored) {
                return String.valueOf(v);
            }
        }
        return String.valueOf(v);
    }

    private static String normalizePlaceholder(String raw) {
        if (raw == null) {
            return null;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return null;
        }
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (!ok) {
                return null;
            }
        }
        return v;
    }
}
