package dev.darkblade.mbe.core.infrastructure.i18n;

import dev.darkblade.mbe.api.i18n.AddonI18n;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DefaultAddonI18n implements AddonI18n {

    private final I18nService i18n;
    private final String addonId;

    public DefaultAddonI18n(I18nService i18n, String addonId) {
        this.i18n = Objects.requireNonNull(i18n, "i18n");
        this.addonId = MessageKey.normalizeOrigin(addonId);
    }

    @Override
    public String addonId() {
        return addonId;
    }

    @Override
    public String tr(CommandSender sender, String path) {
        return i18n.tr(sender, MessageKey.of(addonId, path));
    }

    @Override
    public String tr(CommandSender sender, String path, Map<String, ?> params) {
        return i18n.tr(sender, MessageKey.of(addonId, path), params);
    }

    @Override
    public List<String> trList(CommandSender sender, String path) {
        return i18n.trList(sender, MessageKey.of(addonId, path));
    }

    @Override
    public List<String> trList(CommandSender sender, String path, Map<String, ?> params) {
        return i18n.trList(sender, MessageKey.of(addonId, path), params);
    }

    @Override
    public void send(CommandSender sender, String path) {
        i18n.send(sender, MessageKey.of(addonId, path));
    }

    @Override
    public void send(CommandSender sender, String path, Map<String, ?> params) {
        i18n.send(sender, MessageKey.of(addonId, path), params);
    }
}
