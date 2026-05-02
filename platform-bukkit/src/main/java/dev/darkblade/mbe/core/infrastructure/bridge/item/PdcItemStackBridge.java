package dev.darkblade.mbe.core.infrastructure.bridge.item;

import dev.darkblade.mbe.api.item.ItemDefinition;
import dev.darkblade.mbe.api.item.ItemInstance;
import dev.darkblade.mbe.api.item.ItemKey;
import dev.darkblade.mbe.api.item.ItemKeys;
import dev.darkblade.mbe.api.item.ItemService;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class PdcItemStackBridge implements ItemStackBridge {

    private static final NamespacedKey KEY_ID = NamespacedKey.fromString("multiblockengine:mbe_item_id");
    private static final NamespacedKey KEY_VERSION = NamespacedKey.fromString("multiblockengine:mbe_item_version");
    private static final NamespacedKey KEY_UID = NamespacedKey.fromString("multiblockengine:mbe_item_uid");
    private static final NamespacedKey KEY_DATA = NamespacedKey.fromString("multiblockengine:mbe_item_data");

    private static final String DATA_UID_KEY = "_uid";
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final ItemService items;

    public PdcItemStackBridge(ItemService items) {
        this.items = Objects.requireNonNull(items, "items");
    }

    @Override
    public ItemStack toItemStack(ItemInstance instance) {
        return toItemStack(instance, (Locale) null);
    }

    @Override
    public ItemStack toItemStack(ItemInstance instance, CommandSender sender) {
        if (sender == null) return toItemStack(instance);
        I18nService i18n = resolveI18n();
        Locale locale = i18n != null && i18n.localeProvider() != null ? i18n.localeProvider().localeOf(sender) : null;
        return toItemStack(instance, locale);
    }

    @Override
    public ItemStack toItemStack(ItemInstance instance, Locale locale) {
        Objects.requireNonNull(instance, "instance");
        ItemDefinition def = Objects.requireNonNull(instance.definition(), "instance.definition()");
        ItemKey key = Objects.requireNonNull(def.key(), "definition.key()");

        Material material = resolveMaterial(def);
        ItemStack stack = new ItemStack(material);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = resolveText(def.displayName(), locale);
            if (name != null && !name.isBlank()) {
                meta.displayName(Component.text(name, NamedTextColor.WHITE));
            }

            Map<String, Object> props = def.properties();
            if (props != null) {
                Object loreRaw = props.get("lore");
                List<Component> lore = parseLore(loreRaw, locale);
                if (!lore.isEmpty()) {
                    meta.lore(lore);
                }
                Object customModelRaw = props.get("custom-model-data");
                if (customModelRaw instanceof Number number) {
                    int customModelData = Math.max(0, number.intValue());
                    meta.setCustomModelData(customModelData);
                }
                List<ItemFlag> flags = parseFlags(props.get("flags"));
                if (!flags.isEmpty()) {
                    meta.addItemFlags(flags.toArray(ItemFlag[]::new));
                }
            }

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (KEY_ID != null) {
                pdc.set(KEY_ID, PersistentDataType.STRING, key.id().toString());
            }
            if (KEY_VERSION != null) {
                pdc.set(KEY_VERSION, PersistentDataType.INTEGER, Math.max(0, key.version()));
            }

            props = def.properties();
            Map<String, Object> data = instance.data();
            if (data == null) {
                data = Map.of();
            }

            if (isStorageDisk(props)) {
                long used = readDiskUsed(data);
                long total = readDiskTotal(props, data);
                meta.lore(List.of(
                        StringUtil.legacyText("&7Disco de almacenamiento"),
                        StringUtil.legacyText("&fCapacidad: " + used + "/" + total)
                ));
            }

            if (KEY_UID != null && props != null && Boolean.TRUE.equals(props.get("unstackable"))) {
                Object existing = data.get(DATA_UID_KEY);
                String uid = existing instanceof String s && !s.isBlank() ? s : null;
                if (uid == null) {
                    uid = UUID.randomUUID().toString();
                    if (instance.data() != null) {
                        instance.data().put(DATA_UID_KEY, uid);
                    }
                }
                pdc.set(KEY_UID, PersistentDataType.STRING, uid);
            }

            if (KEY_DATA != null && !data.isEmpty()) {
                try {
                    byte[] encoded = encodeData(data);
                    if (encoded.length > 0) {
                        pdc.set(KEY_DATA, PersistentDataType.BYTE_ARRAY, encoded);
                    } else {
                        pdc.remove(KEY_DATA);
                    }
                } catch (RuntimeException ex) {
                    pdc.remove(KEY_DATA);
                }
            } else if (KEY_DATA != null) {
                pdc.remove(KEY_DATA);
            }
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private static boolean isStorageDisk(Map<String, Object> props) {
        if (props == null || props.isEmpty()) {
            return false;
        }
        Object type = props.get("type");
        return "storage_disk".equalsIgnoreCase(Objects.toString(type, ""));
    }

    private static long readDiskTotal(Map<String, Object> props, Map<String, Object> data) {
        if (props != null) {
            Object capProp = props.get("capacity_items");
            if (capProp instanceof Number n) {
                return Math.max(0L, n.longValue());
            }
        }
        Object cap = data == null ? null : data.get("capacity");
        if (cap instanceof Number n) {
            return Math.max(0L, n.longValue());
        }
        return 0L;
    }

    private static long readDiskUsed(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return 0L;
        }
        Object snapRaw = data.get("storage_snapshot");
        if (!(snapRaw instanceof Map<?, ?> snap) || snap.isEmpty()) {
            return 0L;
        }
        Object itemsRaw = snap.get("items");
        if (!(itemsRaw instanceof Map<?, ?> items) || items.isEmpty()) {
            return 0L;
        }
        long used = 0L;
        for (Object v : items.values()) {
            if (v instanceof Number n) {
                used += Math.max(0L, n.longValue());
            }
        }
        return used;
    }

    private List<Component> parseLore(Object raw, Locale locale) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof String s) {
            String resolved = resolveText(s, locale);
            if (resolved == null || resolved.isBlank()) {
                return List.of();
            }
            return List.of(StringUtil.legacyText(resolved));
        }
        if (raw instanceof List<?> list) {
            List<Component> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = resolveText(String.valueOf(o), locale);
                if (s == null || s.isBlank()) {
                    continue;
                }
                out.add(StringUtil.legacyText(s));
            }
            return out;
        }
        return List.of();
    }

    private List<ItemFlag> parseFlags(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }
        List<ItemFlag> out = new ArrayList<>();
        for (Object value : list) {
            if (value == null) {
                continue;
            }
            String candidate = String.valueOf(value).trim();
            if (candidate.isBlank()) {
                continue;
            }
            try {
                out.add(ItemFlag.valueOf(candidate.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private String resolveText(String raw, Locale locale) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        MessageKey key = parseMessageKey(raw);
        if (key == null) {
            return raw;
        }
        I18nService i18n = resolveI18n();
        if (i18n == null) {
            return raw;
        }
        try {
            Locale target = locale != null ? locale : (i18n.localeProvider() != null ? i18n.localeProvider().fallbackLocale() : Locale.US);
            String translated = i18n.resolve(key, target);
            if (translated == null || translated.isBlank()) {
                return raw;
            }
            if (translated.equals(key.fullKey())) {
                return raw;
            }
            if (translated.equals("??" + key.fullKey() + "??")) {
                return raw;
            }
            return translated;
        } catch (Throwable ignored) {
            return raw;
        }
    }

    private MessageKey parseMessageKey(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            return null;
        }
        int idx = value.indexOf(':');
        if (idx <= 0 || idx >= value.length() - 1) {
            return null;
        }
        String origin = value.substring(0, idx).trim();
        String path = value.substring(idx + 1).trim();
        if (origin.isBlank() || path.isBlank()) {
            return null;
        }
        if (!origin.matches("[a-z0-9_.-]+")) {
            return null;
        }
        if (!path.matches("[a-zA-Z0-9_.-]+")) {
            return null;
        }
        try {
            return MessageKey.of(origin, path);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private I18nService resolveI18n() {
        try {
            MultiBlockEngine plugin = MultiBlockEngine.getInstance();
            if (plugin == null || plugin.getAddonLifecycleService() == null) {
                return null;
            }
            return plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public ItemInstance fromItemStack(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc == null) {
            return null;
        }

        String id = KEY_ID == null ? null : pdc.get(KEY_ID, PersistentDataType.STRING);
        Integer version = KEY_VERSION == null ? null : pdc.get(KEY_VERSION, PersistentDataType.INTEGER);
        if (id == null || id.isBlank()) {
            return null;
        }

        ItemKey key;
        try {
            key = ItemKeys.of(id.trim(), Math.max(0, version == null ? 0 : version));
        } catch (RuntimeException ex) {
            return null;
        }

        if (!items.registry().exists(key)) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        if (KEY_DATA != null) {
            byte[] raw = pdc.get(KEY_DATA, PersistentDataType.BYTE_ARRAY);
            if (raw != null && raw.length > 0) {
                try {
                    Map<String, Object> decoded = decodeData(raw);
                    if (decoded != null && !decoded.isEmpty()) {
                        data.putAll(decoded);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }

        if (KEY_UID != null) {
            String uid = pdc.get(KEY_UID, PersistentDataType.STRING);
            if (uid != null && !uid.isBlank()) {
                data.put(DATA_UID_KEY, uid);
            }
        }

        return items.factory().create(key, data);
    }

    private static byte[] encodeData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }
        String json = GSON.toJson(data);
        if (json == null || json.isBlank()) {
            return new byte[0];
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out);
                 OutputStreamWriter w = new OutputStreamWriter(gzip, StandardCharsets.UTF_8)) {
                w.write(json);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> decodeData(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return Map.of();
        }
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(raw);
            try (GZIPInputStream gzip = new GZIPInputStream(in);
                 InputStreamReader r = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
                Map<String, Object> map = GSON.fromJson(r, MAP_TYPE);
                return map == null ? Map.of() : map;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Material resolveMaterial(ItemDefinition def) {
        Map<String, Object> props = def == null ? null : def.properties();
        if (props != null) {
            Object v = props.get("material");
            if (v instanceof String s && !s.isBlank()) {
                Material m = Material.matchMaterial(s.trim().toUpperCase(Locale.ROOT));
                if (m != null) {
                    return m;
                }
            }
        }
        return Material.PAPER;
    }
}
