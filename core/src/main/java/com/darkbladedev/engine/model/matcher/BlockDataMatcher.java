package com.darkbladedev.engine.model.matcher;

import com.darkbladedev.engine.model.BlockMatcher;
import com.darkbladedev.engine.model.MatchResult;
import com.darkbladedev.engine.model.PatternBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record BlockDataMatcher(PatternBlock pattern) implements BlockMatcher {

    private static final Set<String> DEFAULT_IGNORED_PROPERTIES = Set.of(
        "waterlogged",
        "powered",
        "lit",
        "open",
        "age",
        "level",
        "signal_fire",
        "moisture",
        "bites",
        "honey_level"
    );

    @Override
    public boolean matches(Block block) {
        return match(block).success();
    }

    @Override
    public MatchResult match(Block block) {
        if (block == null) {
            return MatchResult.fail("block is null");
        }
        if (pattern == null || pattern.material() == null) {
            return MatchResult.fail("pattern is null");
        }

        Material expectedMaterial = pattern.material();
        Material foundMaterial = block.getType();
        if (expectedMaterial != foundMaterial) {
            return MatchResult.fail("material mismatch");
        }

        Map<String, String> foundProps = propertiesOf(block);
        for (Map.Entry<String, String> req : pattern.requiredProperties().entrySet()) {
            String key = req.getKey();
            String expected = req.getValue();
            String found = foundProps.get(key);
            if (found == null) {
                return MatchResult.fail("property '" + key + "' missing");
            }
            if (!expected.equalsIgnoreCase(found)) {
                return MatchResult.fail("property '" + key + "' mismatch");
            }
        }

        return MatchResult.ok();
    }

    public Map<String, String> ignoredPropertiesOf(Block block) {
        if (block == null) {
            return Map.of();
        }
        Map<String, String> foundProps = propertiesOf(block);
        if (foundProps.isEmpty()) {
            return Map.of();
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : foundProps.entrySet()) {
            String k = e.getKey();
            if (k == null) {
                continue;
            }
            String kk = k.toLowerCase(Locale.ROOT);
            if (!DEFAULT_IGNORED_PROPERTIES.contains(kk)) {
                continue;
            }
            if (pattern != null && pattern.requiredProperties().containsKey(kk)) {
                continue;
            }
            out.put(kk, e.getValue());
        }
        return Map.copyOf(out);
    }

    private static Map<String, String> propertiesOf(Block block) {
        if (block == null) {
            return Map.of();
        }

        String asString;
        try {
            asString = block.getBlockData().getAsString();
        } catch (Throwable t) {
            return Map.of();
        }

        if (asString == null) {
            return Map.of();
        }

        int start = asString.indexOf('[');
        if (start < 0) {
            return Map.of();
        }
        int end = asString.lastIndexOf(']');
        if (end < start) {
            return Map.of();
        }

        String inner = asString.substring(start + 1, end).trim();
        if (inner.isEmpty()) {
            return Map.of();
        }

        Map<String, String> props = new LinkedHashMap<>();
        for (String part : inner.split(",")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                continue;
            }
            String k = trimmed.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String v = trimmed.substring(eq + 1).trim().toLowerCase(Locale.ROOT);
            if (!k.isEmpty()) {
                props.put(k, v);
            }
        }
        return props.isEmpty() ? Map.of() : Map.copyOf(props);
    }
}
