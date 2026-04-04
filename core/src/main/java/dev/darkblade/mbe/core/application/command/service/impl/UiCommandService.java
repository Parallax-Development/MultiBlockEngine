package dev.darkblade.mbe.core.application.command.service.impl;

import dev.darkblade.mbe.api.command.MbeCommandService;
import dev.darkblade.mbe.api.ui.PanelViewService;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.application.service.editor.EditorSessionManager;
import dev.darkblade.mbe.core.application.service.ui.LinkPanelSession;
import dev.darkblade.mbe.core.application.service.ui.PanelBindingService;
import dev.darkblade.mbe.core.application.service.ui.PanelViewResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class UiCommandService implements MbeCommandService {
    private final EditorSessionManager sessions;
    private final PanelBindingService bindings;
    private final MultiblockRuntimeService multiblocks;

    public UiCommandService(MultiBlockEngine plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.sessions = plugin.getAddonLifecycleService().getCoreService(EditorSessionManager.class);
        this.bindings = plugin.getAddonLifecycleService().getCoreService(PanelBindingService.class);
        this.multiblocks = plugin.getManager();
    }

    @Override
    public String id() {
        return "ui";
    }

    @Override
    public String description() {
        return "Binding entre paneles UI y controllers de multiblocks";
    }

    @Override
    public List<String> infoUsage() {
        return List.of("/mbe services call ui info");
    }

    @Override
    public List<String> executeUsage() {
        return List.of(
                "/mbe services call ui execute link <panelId>",
                "/mbe services call ui execute cancel",
                "/mbe services call ui execute list"
        );
    }

    @Override
    public void info(CommandSender sender, List<String> args) {
        sender.sendMessage(Component.text("Servicio: ui", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(description(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("PanelViewService: " + (resolvePanelService() != null ? "disponible" : "no disponible"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Bindings registrados: " + (bindings == null ? 0 : bindings.all().size()), NamedTextColor.GRAY));
    }

    @Override
    public void execute(CommandSender sender, List<String> args) {
        if (args == null || args.isEmpty()) {
            for (String line : executeUsage()) {
                sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
            }
            return;
        }
        String action = args.get(0).toLowerCase(Locale.ROOT);
        if (action.equals("link")) {
            executeLink(sender, args);
            return;
        }
        if (action.equals("cancel")) {
            executeCancel(sender);
            return;
        }
        if (action.equals("list")) {
            executeList(sender);
            return;
        }
        sender.sendMessage(Component.text("Subcomando desconocido: " + action, NamedTextColor.RED));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String mode, List<String> args) {
        if (!"execute".equalsIgnoreCase(mode)) {
            return List.of();
        }
        if (args == null || args.isEmpty()) {
            return List.of("link", "cancel", "list");
        }
        if (args.size() == 1) {
            String typed = args.get(0).toLowerCase(Locale.ROOT);
            return List.of("link", "cancel", "list").stream().filter(v -> v.startsWith(typed)).toList();
        }
        return List.of();
    }

    private void executeLink(CommandSender sender, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden iniciar sesiones de link.", NamedTextColor.RED));
            return;
        }
        if (sessions == null || bindings == null) {
            sender.sendMessage(Component.text("Servicios de editor/binding no disponibles.", NamedTextColor.RED));
            return;
        }
        if (args.size() < 2) {
            sender.sendMessage(Component.text("/mbe services call ui execute link <panelId>", NamedTextColor.GRAY));
            return;
        }
        PanelViewService panelService;
        try {
            panelService = resolvePanelService();
        } catch (Throwable t) {
            sender.sendMessage(Component.text("Error al resolver PanelViewService.", NamedTextColor.RED));
            return;
        }
        if (panelService == null) {
            sender.sendMessage(Component.text("No hay PanelViewService disponible. Verifica que el addon UI lo registre.", NamedTextColor.RED));
            return;
        }
        String panelId = args.get(1);
        try {
            if (!panelService.panelExists(panelId)) {
                sender.sendMessage(Component.text("No existe el panel: " + panelId, NamedTextColor.RED));
                return;
            }
        } catch (Throwable t) {
            sender.sendMessage(Component.text("El addon UI falló al validar el panel: " + panelId, NamedTextColor.RED));
            return;
        }
        LinkPanelSession session = new LinkPanelSession(player.getUniqueId(), panelId, sessions, bindings, multiblocks);
        sessions.startSession(player, session);
        sender.sendMessage(Component.text("Sesión iniciada. Haz click en el controller_block del multiblock.", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Usa /mbe services call ui execute cancel para cancelar.", NamedTextColor.GRAY));
    }

    private void executeCancel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden cancelar sesiones.", NamedTextColor.RED));
            return;
        }
        if (sessions == null) {
            sender.sendMessage(Component.text("EditorSessionManager no disponible.", NamedTextColor.RED));
            return;
        }
        sessions.cancelSession(player.getUniqueId());
    }

    private void executeList(CommandSender sender) {
        if (bindings == null) {
            sender.sendMessage(Component.text("PanelBindingService no disponible.", NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text("Bindings: " + bindings.all().size(), NamedTextColor.YELLOW));
        bindings.all().stream().limit(20).forEach(binding ->
                sender.sendMessage(Component.text(
                        binding.panelId() + " @ " + binding.world() + ":" + binding.x() + "," + binding.y() + "," + binding.z() + " [" + binding.triggerType() + "]",
                        NamedTextColor.GRAY
                )));
    }

    private PanelViewService resolvePanelService() {
        return PanelViewResolver.resolve();
    }
}
