package com.darkbladedev.engine.command.services;

import com.darkbladedev.engine.MultiBlockEngine;
import com.darkbladedev.engine.api.command.MbeCommandService;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogKv;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ServicesCommandRouter {

    private final CoreLogger log;
    private final ServiceCallParser parser = new ServiceCallParser();
    private final ServiceRegistry registry = new ServiceRegistry();

    public ServicesCommandRouter(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.log = plugin.getLoggingManager().core();
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
            sendError(sender, "Servicio no especificado", List.of("Usa: /" + label + " services list"));
            return true;
        }

        Optional<MbeCommandService> svcOpt = resolveService(sid);
        if (svcOpt.isEmpty()) {
            sendError(sender, "Servicio no encontrado: " + sid, List.of("Usa: /" + label + " services list"));
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
            sendError(sender, "Fallo ejecutando el servicio: " + svc.id(), List.of("Revisa consola para detalles."));
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
        if (parsed instanceof ServiceCallParser.ParseResult.Error err) {
            sendError(sender, err.message(), err.hints());
            return true;
        }

        ServiceCallParser.ServiceCall call = ((ServiceCallParser.ParseResult.Ok) parsed).call();
        Optional<MbeCommandService> svcOpt = resolveService(call.serviceId());
        if (svcOpt.isEmpty()) {
            sendError(sender, "Servicio no encontrado: " + call.serviceId(), List.of("Usa: /mbe services list"));
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
            sendError(sender, "Fallo ejecutando el servicio: " + svc.id(), List.of("Revisa consola para detalles."));
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

        Map<String, RegisteredServiceProvider<MbeCommandService>> external = loadExternalServices();
        RegisteredServiceProvider<MbeCommandService> rsp = external.get(key.toLowerCase(Locale.ROOT));
        return rsp == null ? Optional.empty() : Optional.ofNullable(rsp.getProvider());
    }

    private List<String> mergedIds() {
        List<String> out = new ArrayList<>(registry.ids());
        out.addAll(loadExternalServices().keySet());
        out.sort(String::compareToIgnoreCase);
        return List.copyOf(out);
    }

    private Map<String, RegisteredServiceProvider<MbeCommandService>> loadExternalServices() {
        var regs = Bukkit.getServicesManager().getRegistrations(MbeCommandService.class);
        Map<String, RegisteredServiceProvider<MbeCommandService>> byId = new HashMap<>();
        if (regs == null) {
            return Map.of();
        }

        for (RegisteredServiceProvider<MbeCommandService> rsp : regs) {
            if (rsp == null) {
                continue;
            }
            MbeCommandService svc = rsp.getProvider();
            if (svc == null || svc.id() == null || svc.id().isBlank()) {
                continue;
            }
            String id = svc.id().trim().toLowerCase(Locale.ROOT);
            if (registry.resolve(id).isPresent()) {
                continue;
            }

            RegisteredServiceProvider<MbeCommandService> existing = byId.get(id);
            if (existing == null) {
                byId.put(id, rsp);
                continue;
            }

            ServicePriority prev = existing.getPriority();
            ServicePriority next = rsp.getPriority();
            if (next.ordinal() > prev.ordinal()) {
                byId.put(id, rsp);
            }
        }

        return Map.copyOf(byId);
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Uso: /" + label + " <inspect|reload|status|debug|services>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Servicios: /" + label + " services call <servicio> <info|execute> <argumentos>", NamedTextColor.YELLOW));
    }

    private void sendServicesOverview(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Uso:", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " services list", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/" + label + " services call <servicio> <info|execute> <argumentos>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Ejemplo: /" + label + " services call ui info", NamedTextColor.GRAY));
    }

    private void sendServicesList(CommandSender sender) {
        List<MbeCommandService> all = new ArrayList<>(registry.list());
        for (RegisteredServiceProvider<MbeCommandService> rsp : loadExternalServices().values()) {
            MbeCommandService svc = rsp.getProvider();
            if (svc != null) {
                all.add(svc);
            }
        }

        all.sort((a, b) -> a.id().compareToIgnoreCase(b.id()));
        sender.sendMessage(Component.text("Servicios disponibles (" + all.size() + "):", NamedTextColor.YELLOW));
        for (MbeCommandService svc : all) {
            sender.sendMessage(Component.text("- " + svc.id() + " : " + safe(svc.description()), NamedTextColor.GRAY));
        }
    }

    private void sendError(CommandSender sender, String message, List<String> hints) {
        sender.sendMessage(Component.text(message == null ? "Error" : message, NamedTextColor.RED));
        for (String hint : hints == null ? List.<String>of() : hints) {
            if (hint != null && !hint.isBlank()) {
                sender.sendMessage(Component.text(hint, NamedTextColor.GRAY));
            }
        }
    }

    private void audit(CommandSender sender, String label, MbeCommandService svc, ServiceCallParser.CallMode mode, List<String> args, boolean ok, Throwable error) {
        String senderType = sender == null ? "?" : sender.getClass().getSimpleName();
        String senderName = sender == null ? "?" : sender.getName();
        String joined = args == null || args.isEmpty() ? "" : String.join(" ", args);

        log.info(
                "Service command",
                LogKv.kv("cmd", "/" + label),
                LogKv.kv("sub", "services"),
                LogKv.kv("service", svc == null ? "?" : svc.id()),
                LogKv.kv("mode", mode == null ? "?" : mode.name().toLowerCase(Locale.ROOT)),
                LogKv.kv("args", joined),
                LogKv.kv("senderType", senderType),
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
}
