package com.darkbladedev.engine.util;

import com.darkbladedev.engine.model.MultiblockInstance;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("<variable:([a-zA-Z0-9_]+)>");

    public static String parsePlaceholders(String text, MultiblockInstance instance) {
        if (text == null || text.isEmpty()) return text;

        // Replace basic coordinate placeholders
        String result = text
                .replace("%x%", String.valueOf(instance.anchorLocation().getBlockX()))
                .replace("%y%", String.valueOf(instance.anchorLocation().getBlockY()))
                .replace("%z%", String.valueOf(instance.anchorLocation().getBlockZ()))
                .replace("%world%", instance.anchorLocation().getWorld().getName());

        // Replace <variable:name>
        Matcher matcher = VARIABLE_PATTERN.matcher(result);
        StringBuilder sb = new StringBuilder();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = instance.getVariable(varName);
            
            // If variable exists, replace with value. Otherwise, keep placeholder or empty?
            // Usually keeping placeholder or "null" is better for debugging, 
            // but for user display, maybe "0" or "undefined"?
            // Let's use value.toString() or "null" if missing.
            String replacement = (value != null) ? value.toString() : "0";
            
            // Format numbers nicely if integer (remove .0)
            if (value instanceof Number n) {
                if (n.doubleValue() == n.longValue()) {
                    replacement = String.valueOf(n.longValue());
                }
            }
            
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}
