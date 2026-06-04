package dev.darkblade.mbe.core.application.command.addon;

import dev.darkblade.mbe.core.application.service.addon.domain.AddonState;
import dev.darkblade.mbe.core.application.service.addon.domain.AddonInfo;



import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.application.service.addon.AddonLifecycleService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Routes {@code /mbe addons} subcommands.
 * <p>
 * Built-in subcommands: {@code list}, {@code status}.
 * Use {@link #registerSubcommand(AddonsSubcommand)} to add more without modifying this class.
 */
public final class AddonsCommandRouter {

    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_OVERVIEW_USAGE = MessageKey.of(ORIGIN, "addons.router.overview.usage");
    private static final MessageKey MSG_OVERVIEW_LIST = MessageKey.of(ORIGIN, "addons.router.overview.list");
    private static final MessageKey MSG_OVERVIEW_STATUS = MessageKey.of(ORIGIN, "addons.router.overview.status");

    private static final MessageKey MSG_LIST_TITLE = MessageKey.of(ORIGIN, "addons.router.list.title");
    private static final MessageKey MSG_LIST_ENTRY = MessageKey.of(ORIGIN, "addons.router.list.entry");
    private static final MessageKey MSG_LIST_NONE = MessageKey.of(ORIGIN, "addons.router.list.none");

    private static final MessageKey MSG_STATUS_TITLE = MessageKey.of(ORIGIN, "addons.router.status.title");
    private static final MessageKey MSG_STATUS_VERSION = MessageKey.of(ORIGIN, "addons.router.status.version");
    private static final MessageKey MSG_STATUS_STATE = MessageKey.of(ORIGIN, "addons.router.status.state");
    private static final MessageKey MSG_STATUS_DEPENDENCIES = MessageKey.of(ORIGIN, "addons.router.status.dependencies");
    private static final MessageKey MSG_STATUS_SERVICES_TITLE = MessageKey.of(ORIGIN, "addons.router.status.services_title");
    private static final MessageKey MSG_STATUS_SERVICE_ENTRY = MessageKey.of(ORIGIN, "addons.router.status.service_entry");
    private static final MessageKey MSG_STATUS_NO_SERVICES = MessageKey.of(ORIGIN, "addons.router.status.no_services");
    private static final MessageKey MSG_STATUS_NOT_FOUND = MessageKey.of(ORIGIN, "addons.router.status.not_found");

    private static final MessageKey MSG_ERROR_GENERIC = MessageKey.of(ORIGIN, "addons.router.error.generic");

    private final MultiBlockEngine plugin;
    private final Map<String, AddonsSubcommand> subcommands = new HashMap<>();

    public AddonsCommandRouter(MultiBlockEngine plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        registerSubcommand(new ListSubcommand());
        registerSubcommand(new StatusSubcommand());
    }

    /**
     * Register an additional subcommand. Duplicate names are rejected silently.
     */
    public void registerSubcommand(AddonsSubcommand subcommand) {
        Objects.requireNonNull(subcommand, "subcommand");
        String name = subcommand.name().trim().toLowerCase(Locale.ROOT);
        subcommands.putIfAbsent(name, subcommand);
    }

    public boolean handle(CommandSender sender, String label, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        // /mbe addons
        if (safeArgs.length <= 1) {
            sendOverview(sender, label);
            return true;
        }

        String op = safeArgs[1].trim().toLowerCase(Locale.ROOT);
        if (op.equals("help")) {
            sendOverview(sender, label);
            return true;
        }

        AddonsSubcommand sub = subcommands.get(op);
        if (sub != null) {
            List<String> remaining = safeArgs.length <= 2
                    ? List.of()
                    : List.of(java.util.Arrays.copyOfRange(safeArgs, 2, safeArgs.length));
            sub.execute(sender, label, remaining);
            return true;
        }

        send(sender, MSG_ERROR_GENERIC);
        sendOverview(sender, label);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        // /mbe addons <tab>
        if (safeArgs.length == 2) {
            List<String> names = new ArrayList<>(subcommands.keySet());
            names.add("help");
            return filter(names, safeArgs[1]);
        }

        // /mbe addons <sub> <tab...>
        if (safeArgs.length >= 3) {
            String op = safeArgs[1].trim().toLowerCase(Locale.ROOT);
            AddonsSubcommand sub = subcommands.get(op);
            if (sub != null) {
                List<String> remaining = List.of(java.util.Arrays.copyOfRange(safeArgs, 2, safeArgs.length));
                return filter(sub.tabComplete(sender, remaining), remaining.get(remaining.size() - 1));
            }
        }

        return Collections.emptyList();
    }

    // --- Built-in subcommands ---

    private final class ListSubcommand implements AddonsSubcommand {
        @Override
        public String name() {
            return "list";
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            List<AddonInfo> addons = plugin.getAddonLifecycleService().getAddonInfoList();
            if (addons.isEmpty()) {
                send(sender, MSG_LIST_NONE);
                return;
            }
            send(sender, MSG_LIST_TITLE, Map.of("count", addons.size()));
            for (AddonInfo info : addons) {
                send(sender, MSG_LIST_ENTRY, Map.of(
                        "id", info.id(),
                        "version", safe(info.version()),
                        "state", coloredState(info.state())
                ));
            }
        }
    }

    private final class StatusSubcommand implements AddonsSubcommand {
        @Override
        public String name() {
            return "status";
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty()) {
                send(sender, MSG_OVERVIEW_STATUS, Map.of("label", label));
                return;
            }
            String addonId = args.get(0).trim();
            Optional<AddonInfo> opt = plugin.getAddonLifecycleService().getAddonInfo(addonId);
            if (opt.isEmpty()) {
                send(sender, MSG_STATUS_NOT_FOUND, Map.of("id", addonId));
                return;
            }
            AddonInfo info = opt.get();
            send(sender, MSG_STATUS_TITLE, Map.of("id", info.id()));
            send(sender, MSG_STATUS_VERSION, Map.of("version", safe(info.version())));
            send(sender, MSG_STATUS_STATE, Map.of("state", coloredState(info.state())));
            String deps = info.dependencies().isEmpty() ? "none" : String.join(", ", info.dependencies());
            send(sender, MSG_STATUS_DEPENDENCIES, Map.of("deps", deps));
            if (info.serviceIds().isEmpty()) {
                send(sender, MSG_STATUS_NO_SERVICES);
            } else {
                send(sender, MSG_STATUS_SERVICES_TITLE, Map.of("count", info.serviceIds().size()));
                for (String svc : info.serviceIds()) {
                    send(sender, MSG_STATUS_SERVICE_ENTRY, Map.of("serviceId", svc));
                }
            }
        }

        @Override
        public List<String> tabComplete(CommandSender sender, List<String> args) {
            if (args.size() == 1) {
                List<String> ids = new ArrayList<>();
                for (AddonInfo info : plugin.getAddonLifecycleService().getAddonInfoList()) {
                    ids.add(info.id());
                }
                return ids;
            }
            return List.of();
        }
    }

    // --- Utilities ---

    private static String coloredState(AddonState state) {
        if (state == null) {
            return "&7UNKNOWN";
        }
        return switch (state) {
            case ENABLED -> "&a" + state.name();
            case FAILED -> "&c" + state.name();
            case DISABLED -> "&e" + state.name();
            case LOADED -> "&7" + state.name();
            case DISCOVERED -> "&7" + state.name();
        };
    }

    private void sendOverview(CommandSender sender, String label) {
        send(sender, MSG_OVERVIEW_USAGE);
        send(sender, MSG_OVERVIEW_LIST, Map.of("label", label));
        send(sender, MSG_OVERVIEW_STATUS, Map.of("label", label));
    }

    private void send(CommandSender sender, MessageKey key) {
        send(sender, key, Map.of());
    }

    private void send(CommandSender sender, MessageKey key, Map<String, Object> params) {
        if (sender == null || key == null) {
            return;
        }
        PlayerMessageService messageService = plugin.getAddonLifecycleService().getCoreService(PlayerMessageService.class);
        if (sender instanceof Player player && messageService != null) {
            messageService.send(player, new PlayerMessage(key, MessageChannel.CHAT, MessagePriority.NORMAL, params == null ? Map.of() : params));
            return;
        }
        I18nService i18n = plugin.getAddonLifecycleService().getCoreService(I18nService.class);
        if (i18n != null) {
            i18n.send(sender, key, params == null ? Map.of() : params);
        }
    }

    private static List<String> filter(List<String> list, String prefix) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s == null) {
                continue;
            }
            if (p.isEmpty() || s.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}

