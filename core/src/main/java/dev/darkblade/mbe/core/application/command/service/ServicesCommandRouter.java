package dev.darkblade.mbe.core.application.command.service;

import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.api.event.ComponentAvailabilityEvent;
import dev.darkblade.mbe.api.event.ComponentKind;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ServicesCommandRouter implements Listener {
    private static final String ORIGIN = "mbe";
    private static final MessageKey MSG_ERROR_SERVICE_NOT_SPECIFIED = MessageKey.of(ORIGIN, "services.router.error.service_not_specified");
    private static final MessageKey MSG_ERROR_SERVICE_NOT_FOUND = MessageKey.of(ORIGIN, "services.router.error.service_not_found");
    private static final MessageKey MSG_ERROR_SERVICE_EXECUTION_FAILED = MessageKey.of(ORIGIN, "services.router.error.execution_failed");
    private static final MessageKey MSG_HINT_USE_SERVICES_LIST = MessageKey.of(ORIGIN, "services.router.hint.use_services_list");
    private static final MessageKey MSG_HINT_CHECK_CONSOLE = MessageKey.of(ORIGIN, "services.router.hint.check_console");
    private static final MessageKey MSG_HELP_USAGE = MessageKey.of(ORIGIN, "services.router.help.usage");
    private static final MessageKey MSG_HELP_SERVICES_USAGE = MessageKey.of(ORIGIN, "services.router.help.services_usage");
    private static final MessageKey MSG_OVERVIEW_USAGE = MessageKey.of(ORIGIN, "services.router.overview.usage");
    private static final MessageKey MSG_OVERVIEW_LIST = MessageKey.of(ORIGIN, "services.router.overview.list");
    private static final MessageKey MSG_OVERVIEW_CALL = MessageKey.of(ORIGIN, "services.router.overview.call");
    private static final MessageKey MSG_OVERVIEW_EXAMPLE = MessageKey.of(ORIGIN, "services.router.overview.example");
    private static final MessageKey MSG_LIST_TITLE = MessageKey.of(ORIGIN, "services.router.list.title");
    private static final MessageKey MSG_LIST_ENTRY = MessageKey.of(ORIGIN, "services.router.list.entry");
    private static final MessageKey MSG_ERROR_GENERIC = MessageKey.of(ORIGIN, "services.router.error.generic");

    private final MultiBlockEngine plugin;

    private final CoreLogger log;
    private final ServiceCallParser parser = new ServiceCallParser();
    private final ServiceRegistry registry = new ServiceRegistry();
    private final DynamicCommandServiceRegistry externalRegistry;

    public ServicesCommandRouter(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.plugin = plugin;
        this.log = plugin.getLoggingService().core();
        this.externalRegistry = new DynamicCommandServiceRegistry(this::loadExternalCommandServices);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        refreshExternalCommandServices();
    }

    public void registerInternal(MbeCommandService service) {
        registry.register(service);
    }

    public List<String> serviceIds() {
        return mergedIds();
    }

    public Optional<MbeCommandService> resolve(String idOrAlias) {
        return resolveService(idOrAlias);
    }

    public boolean dispatch(CommandSender sender, String label, String serviceId, String mode, List<String> args) {
        Objects.requireNonNull(sender, "sender");
        String sid = serviceId == null ? "" : serviceId.trim();
        if (sid.isEmpty()) {
            sendError(sender, MSG_ERROR_SERVICE_NOT_SPECIFIED, Map.of(), List.of(Map.entry(MSG_HINT_USE_SERVICES_LIST, Map.of("label", label))));
            return true;
        }

        Optional<MbeCommandService> svcOpt = resolveService(sid);
        if (svcOpt.isEmpty()) {
            sendError(sender, MSG_ERROR_SERVICE_NOT_FOUND, Map.of("service", sid), List.of(Map.entry(MSG_HINT_USE_SERVICES_LIST, Map.of("label", label))));
            return true;
        }

        MbeCommandService svc = svcOpt.get();
        ServiceCallParser.CallMode m = "info".equalsIgnoreCase(mode)
                ? ServiceCallParser.CallMode.INFO
                : ServiceCallParser.CallMode.EXECUTE;

        List<String> safeArgs = args == null ? List.of() : List.copyOf(args);
        try {
            if (m == ServiceCallParser.CallMode.INFO) {
                svc.info(sender, safeArgs);
                audit(sender, label, svc, m, safeArgs, true, null);
            } else {
                svc.execute(sender, safeArgs);
                audit(sender, label, svc, m, safeArgs, true, null);
            }
        } catch (Throwable t) {
            audit(sender, label, svc, m, safeArgs, false, t);
            sendError(sender, MSG_ERROR_SERVICE_EXECUTION_FAILED, Map.of("service", svc.id()), List.of(Map.entry(MSG_HINT_CHECK_CONSOLE, Map.of())));
        }
        return true;
    }

    public List<String> tabCompleteService(CommandSender sender, String serviceId, String mode, List<String> args) {
        Objects.requireNonNull(sender, "sender");
        Optional<MbeCommandService> svcOpt = resolveService(serviceId);
        if (svcOpt.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> remaining = args == null ? List.of() : args;
        List<String> suggestions;
        try {
            suggestions = svcOpt.get().tabComplete(sender, mode, remaining);
        } catch (Throwable ignored) {
            suggestions = List.of();
        }
        String last = remaining.isEmpty() ? "" : remaining.get(remaining.size() - 1);
        return filter(suggestions, last);
    }

    public void sendServicesListPublic(CommandSender sender) {
        sendServicesList(sender);
    }

    public boolean handle(CommandSender sender, String label, String[] args) {
        Objects.requireNonNull(sender, "sender");
        String[] safeArgs = args == null ? new String[0] : args;

        if (safeArgs.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        if (!"services".equalsIgnoreCase(safeArgs[0])) {
            sendHelp(sender, label);
            return true;
        }

        if (safeArgs.length == 1) {
            sendServicesOverview(sender, label);
            return true;
        }

        String op = safeArgs[1].toLowerCase(Locale.ROOT);
        if (op.equals("list")) {
            sendServicesList(sender);
            return true;
        }

        if (op.equals("help")) {
            sendServicesOverview(sender, label);
            return true;
        }

        ServiceCallParser.ParseResult parsed = parser.parseServicesCall(safeArgs);
        if (parsed instanceof ServiceCallParser.ParseResult.Error) {
            sendError(sender, MSG_ERROR_GENERIC, Map.of(), List.of(Map.entry(MSG_HINT_USE_SERVICES_LIST, Map.of("label", label))));
            return true;
        }

        ServiceCallParser.ServiceCall call = ((ServiceCallParser.ParseResult.Ok) parsed).call();
        Optional<MbeCommandService> svcOpt = resolveService(call.serviceId());
        if (svcOpt.isEmpty()) {
            sendError(sender, MSG_ERROR_SERVICE_NOT_FOUND, Map.of("service", call.serviceId()), List.of(Map.entry(MSG_HINT_USE_SERVICES_LIST, Map.of("label", "mbe"))));
            return true;
        }

        MbeCommandService svc = svcOpt.get();
        try {
            if (call.mode() == ServiceCallParser.CallMode.INFO) {
                svc.info(sender, call.args());
                audit(sender, label, svc, call.mode(), call.args(), true, null);
            } else {
                svc.execute(sender, call.args());
                audit(sender, label, svc, call.mode(), call.args(), true, null);
            }
        } catch (Throwable t) {
            audit(sender, label, svc, call.mode(), call.args(), false, t);
            sendError(sender, MSG_ERROR_SERVICE_EXECUTION_FAILED, Map.of("service", svc.id()), List.of(Map.entry(MSG_HINT_CHECK_CONSOLE, Map.of())));
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        String[] safeArgs = args == null ? new String[0] : args;

        if (safeArgs.length == 1) {
            return filter(List.of("inspect", "reload", "status", "stats", "debug", "services"), safeArgs[0]);
        }

        if (!"services".equalsIgnoreCase(safeArgs[0])) {
            return Collections.emptyList();
        }

        if (safeArgs.length == 2) {
            return filter(List.of("call", "list", "help"), safeArgs[1]);
        }

        if (safeArgs.length == 3 && "call".equalsIgnoreCase(safeArgs[1])) {
            return filter(mergedIds(), safeArgs[2]);
        }

        if (safeArgs.length == 4 && "call".equalsIgnoreCase(safeArgs[1])) {
            return filter(List.of("info", "execute"), safeArgs[3]);
        }

        if (safeArgs.length >= 5 && "call".equalsIgnoreCase(safeArgs[1])) {
            Optional<MbeCommandService> svcOpt = resolveService(safeArgs[2]);
            if (svcOpt.isEmpty()) {
                return Collections.emptyList();
            }
            String mode = safeArgs[3];
            List<String> remaining = safeArgs.length <= 4 ? List.of() : Arrays.asList(Arrays.copyOfRange(safeArgs, 4, safeArgs.length));
            List<String> suggestions;
            try {
                suggestions = svcOpt.get().tabComplete(sender, mode, remaining);
            } catch (Throwable ignored) {
                suggestions = List.of();
            }
            String last = remaining.isEmpty() ? "" : remaining.get(remaining.size() - 1);
            return filter(suggestions, last);
        }

        return Collections.emptyList();
    }

    private Optional<MbeCommandService> resolveService(String idOrAlias) {
        Optional<MbeCommandService> internal = registry.resolve(idOrAlias);
        if (internal.isPresent()) {
            return internal;
        }

        String key = idOrAlias == null ? "" : idOrAlias.trim();
        if (key.isEmpty()) {
            return Optional.empty();
        }

        return externalRegistry.resolve(key);
    }

    private List<String> mergedIds() {
        List<String> out = new ArrayList<>(registry.ids());
        out.addAll(externalRegistry.ids());
        out.sort(String::compareToIgnoreCase);
        return List.copyOf(new LinkedHashSet<>(out));
    }

    public synchronized void refreshExternalCommandServices() {
        this.externalRegistry.refresh();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServiceRegistered(ServiceRegisterEvent event) {
        if (event == null || event.getProvider() == null || !MbeCommandService.class.equals(event.getProvider().getService())) {
            return;
        }
        refreshExternalCommandServices();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServiceUnregistered(ServiceUnregisterEvent event) {
        if (event == null || event.getProvider() == null || !MbeCommandService.class.equals(event.getProvider().getService())) {
            return;
        }
        refreshExternalCommandServices();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onComponentAvailability(ComponentAvailabilityEvent event) {
        if (event == null) {
            return;
        }
        if (event.getComponentKind() == ComponentKind.COMMAND_SERVICE || event.getComponentKind() == ComponentKind.SERVICE) {
            refreshExternalCommandServices();
        }
    }

    private List<MbeCommandService> loadExternalCommandServices() {
        var regs = Bukkit.getServicesManager().getRegistrations(MbeCommandService.class);
        if (regs == null || regs.isEmpty()) {
            return List.of();
        }

        Map<String, List<RegisteredServiceProvider<MbeCommandService>>> byId = new HashMap<>();
        for (RegisteredServiceProvider<MbeCommandService> rsp : regs) {
            if (rsp == null) {
                continue;
            }
            MbeCommandService svc = rsp.getProvider();
            if (svc == null || svc.id() == null || svc.id().isBlank()) {
                continue;
            }
            String id = svc.id().trim().toLowerCase(Locale.ROOT);
            byId.computeIfAbsent(id, ignored -> new ArrayList<>()).add(rsp);
        }

        List<MbeCommandService> resolved = new ArrayList<>();
        for (Map.Entry<String, List<RegisteredServiceProvider<MbeCommandService>>> entry : byId.entrySet()) {
            String id = entry.getKey();
            List<RegisteredServiceProvider<MbeCommandService>> providers = new ArrayList<>(entry.getValue());
            providers.sort((a, b) -> Integer.compare(b.getPriority().ordinal(), a.getPriority().ordinal()));

            boolean collidesWithInternal = registry.resolve(id).isPresent();
            boolean ambiguousExternal = providers.size() > 1;

            if (!collidesWithInternal && !ambiguousExternal) {
                MbeCommandService single = providers.getFirst().getProvider();
                if (single != null) {
                    resolved.add(single);
                }
                continue;
            }

            Set<String> usedQualifiedIds = new HashSet<>();
            for (RegisteredServiceProvider<MbeCommandService> provider : providers) {
                MbeCommandService service = provider.getProvider();
                if (service == null) {
                    continue;
                }
                String qualifiedId = buildQualifiedExternalId(provider, id, usedQualifiedIds);
                resolved.add(new QualifiedExternalCommandService(qualifiedId, service));
            }
        }

        return List.copyOf(resolved);
    }

    private static String buildQualifiedExternalId(RegisteredServiceProvider<MbeCommandService> provider, String shortId, Set<String> usedQualifiedIds) {
        Plugin plugin = provider == null ? null : provider.getPlugin();
        String pluginName = plugin == null ? "external" : plugin.getName();
        String namespace = normalizeNamespace(pluginName);
        String base = namespace + ":" + shortId;
        String candidate = base;
        int suffix = 2;
        while (usedQualifiedIds.contains(candidate)) {
            candidate = base + "-" + suffix++;
        }
        usedQualifiedIds.add(candidate);
        return candidate;
    }

    private static String normalizeNamespace(String raw) {
        String lower = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (lower.isBlank()) {
            return "external";
        }
        String sanitized = lower.replaceAll("[^a-z0-9_.-]", "-");
        if (sanitized.isBlank()) {
            return "external";
        }
        if (sanitized.startsWith("-")) {
            sanitized = "external" + sanitized;
        }
        return sanitized;
    }

    private static final class QualifiedExternalCommandService implements MbeCommandService {
        private final String id;
        private final MbeCommandService delegate;

        private QualifiedExternalCommandService(String id, MbeCommandService delegate) {
            this.id = id;
            this.delegate = delegate;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String description() {
            return delegate.description();
        }

        @Override
        public List<String> infoUsage() {
            return delegate.infoUsage();
        }

        @Override
        public List<String> executeUsage() {
            return delegate.executeUsage();
        }

        @Override
        public void info(CommandSender sender, List<String> args) {
            delegate.info(sender, args);
        }

        @Override
        public void execute(CommandSender sender, List<String> args) {
            delegate.execute(sender, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
            return delegate.tabComplete(sender, mode, args);
        }

        @Override
        public List<String> aliases() {
            return List.of();
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        send(sender, MSG_HELP_USAGE, Map.of("label", label));
        send(sender, MSG_HELP_SERVICES_USAGE, Map.of("label", label));
    }

    private void sendServicesOverview(CommandSender sender, String label) {
        send(sender, MSG_OVERVIEW_USAGE);
        send(sender, MSG_OVERVIEW_LIST, Map.of("label", label));
        send(sender, MSG_OVERVIEW_CALL, Map.of("label", label));
        send(sender, MSG_OVERVIEW_EXAMPLE, Map.of("label", label));
    }

    private void sendServicesList(CommandSender sender) {
        List<MbeCommandService> all = new ArrayList<>(registry.list());
        all.addAll(externalRegistry.services());

        all.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        send(sender, MSG_LIST_TITLE, Map.of("count", all.size()));
        for (MbeCommandService svc : all) {
            send(sender, MSG_LIST_ENTRY, Map.of("id", svc.id(), "description", safe(svc.description())));
        }
    }

    private void sendError(CommandSender sender, MessageKey messageKey, Map<String, Object> params, List<Map.Entry<MessageKey, Map<String, Object>>> hints) {
        if (messageKey != null) {
            send(sender, messageKey, params);
        } else {
            send(sender, MSG_ERROR_GENERIC);
        }
        for (Map.Entry<MessageKey, Map<String, Object>> hint : hints == null ? List.<Map.Entry<MessageKey, Map<String, Object>>>of() : hints) {
            if (hint != null && hint.getKey() != null) {
                send(sender, hint.getKey(), hint.getValue() == null ? Map.of() : hint.getValue());
            }
        }
    }

    private void audit(CommandSender sender, String label, MbeCommandService svc, ServiceCallParser.CallMode mode, List<String> args, boolean ok, Throwable error) {
        String senderName = sender == null ? "?" : sender.getName();
        String joined = args == null || args.isEmpty() ? "" : String.join(" ", args);

        log.info(
                "Service command",
                LogKv.kv("cmd", "/" + label),
                LogKv.kv("sub", "services"),
                LogKv.kv("service", svc == null ? "?" : svc.id()),
                LogKv.kv("mode", mode == null ? "?" : mode.name().toLowerCase(Locale.ROOT)),
                LogKv.kv("args", joined),
                LogKv.kv("sender", senderName),
                LogKv.kv("ok", ok)
        );

        if (error != null) {
            log.error("Service command failed", error, LogKv.kv("service", svc == null ? "?" : svc.id()));
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

}
