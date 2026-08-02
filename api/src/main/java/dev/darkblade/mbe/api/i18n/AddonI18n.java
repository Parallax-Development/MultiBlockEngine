package dev.darkblade.mbe.api.i18n;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public interface AddonI18n {

    String addonId();

    String tr(CommandSender sender, String path);

    String tr(CommandSender sender, String path, Map<String, ?> params);

    default String tr(CommandSender sender, String path, Object... params) {
        return tr(sender, path, MessageUtils.params(params));
    }

    List<String> trList(CommandSender sender, String path);

    List<String> trList(CommandSender sender, String path, Map<String, ?> params);

    default List<String> trList(CommandSender sender, String path, Object... params) {
        return trList(sender, path, MessageUtils.params(params));
    }

    void send(CommandSender sender, String path);

    void send(CommandSender sender, String path, Map<String, ?> params);

    default void send(CommandSender sender, String path, Object... params) {
        send(sender, path, MessageUtils.params(params));
    }
}
