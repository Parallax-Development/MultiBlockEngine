package dev.darkblade.mbe.core.application.service.lifecycle;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;
import dev.darkblade.mbe.api.platform.MBEPlayer;
import dev.darkblade.mbe.api.service.lifecycle.MultiblockLifecycleService;
import dev.darkblade.mbe.core.application.service.limit.MultiblockLimitService;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.action.Action;
import dev.darkblade.mbe.core.MultiBlockEngine;
import dev.darkblade.mbe.api.addon.AddonException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class DefaultMultiblockLifecycleService implements MultiblockLifecycleService {

    private static final MessageKey MSG_DISASSEMBLED = MessageKey.of("mbe", "core.wrench.disassembled");

    private final MultiblockRuntimeService runtimeService;
    private final MultiblockLimitService limitService;
    private final Consumer<dev.darkblade.mbe.api.event.MBEEvent> eventDispatcher;
    private final PlayerMessageService messageService;
    private final I18nService i18nService;

    public DefaultMultiblockLifecycleService(
            MultiblockRuntimeService runtimeService,
            MultiblockLimitService limitService,
            Consumer<dev.darkblade.mbe.api.event.MBEEvent> eventDispatcher,
            PlayerMessageService messageService,
            I18nService i18nService) {
        this.runtimeService = runtimeService;
        this.limitService = limitService;
        this.eventDispatcher = eventDispatcher;
        this.messageService = messageService;
        this.i18nService = i18nService;
    }

    @Override
    public boolean tryDisassemble(MultiblockInstance instance, @Nullable MBEPlayer actor) {
        MultiblockBreakEvent mbEvent = new MultiblockBreakEvent(instance, actor);
        eventDispatcher.accept(mbEvent);

        if (mbEvent.isCancelled()) {
            return false;
        }

        Player bukkitPlayer = null;
        if (actor != null) {
            bukkitPlayer = Bukkit.getPlayer(actor.getUniqueId());
        }

        for (Action action : instance.type().onBreakActions()) {
            executeActionSafely("BREAK", action, instance, bukkitPlayer);
        }

        unregisterLimit(instance, actor);
        runtimeService.destroyInstance(instance);
        sendDisassembledMessage(actor, instance);

        return true;
    }

    private void unregisterLimit(MultiblockInstance instance, @Nullable MBEPlayer actor) {
        if (instance == null || instance.type() == null) return;
        if (limitService == null) return;

        UUID ownerId = resolveOwnerId(instance, actor);
        if (ownerId == null) return;

        limitService.unregisterAssembly(ownerId, instance.type().id().toString());
    }

    private UUID resolveOwnerId(MultiblockInstance instance, @Nullable MBEPlayer actor) {
        if (actor != null) {
            return actor.getUniqueId();
        }
        Object owner = instance.getVariable("owner_uuid");
        if (owner == null) return null;
        try {
            return UUID.fromString(String.valueOf(owner));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void sendDisassembledMessage(@Nullable MBEPlayer actor, MultiblockInstance instance) {
        if (actor == null || instance == null || instance.type() == null) return;

        String typeId = instance.type().id() == null ? "" : instance.type().id().toString();
        Player bukkitPlayer = Bukkit.getPlayer(actor.getUniqueId());
        if (bukkitPlayer == null) return;

        if (messageService != null) {
            messageService.send(bukkitPlayer, new PlayerMessage(
                    MSG_DISASSEMBLED,
                    MessageChannel.CHAT,
                    MessagePriority.NORMAL,
                    Map.of("type", typeId)));
            return;
        }

        if (i18nService != null) {
            i18nService.send(bukkitPlayer, MSG_DISASSEMBLED, Map.of("type", typeId));
        }
    }

    private void executeActionSafely(String runtimePhase, Action action, MultiblockInstance instance, @Nullable Player player) {
        try {
            if (player != null) {
                action.execute(instance, player);
            } else {
                action.execute(instance);
            }
        } catch (Throwable t) {
            String ownerId = action != null ? action.ownerId() : null;
            String typeKey = action != null ? action.typeKey() : null;

            String actionName = "unknown";
            if (typeKey != null && !typeKey.isBlank()) {
                int idx = typeKey.lastIndexOf(':');
                actionName = idx >= 0 ? typeKey.substring(idx + 1) : typeKey;
            } else if (action != null) {
                actionName = action.getClass().getSimpleName();
            }

            Object counter = instance != null ? instance.getVariable("counter") : null;
            String msg = "[" + runtimePhase + "] Action '" + actionName + "' failed Context: counter=" + counter
                    + " Multiblock=" + (instance != null ? instance.type().id() : "unknown") + " Execution continued";

            if (ownerId != null && !ownerId.isBlank() && MultiBlockEngine.getInstance().getAddonLifecycleService() != null) {
                MultiBlockEngine.getInstance().getAddonLifecycleService().failAddon(ownerId, AddonException.Phase.RUNTIME, msg, t, false);
            } else {
                CoreLogger core = MultiBlockEngine.getInstance().getLoggingService() != null
                        ? MultiBlockEngine.getInstance().getLoggingService().core()
                        : null;
                if (core != null) {
                    core.logInternal(new LogScope.Core(), LogPhase.RUNTIME, LogLevel.ERROR, msg, t, new LogKv[]{
                            LogKv.kv("phase", runtimePhase),
                            LogKv.kv("multiblock", instance != null ? instance.type().id() : "unknown"),
                            LogKv.kv("action", actionName)
                    }, Set.of());
                } else {
                    MultiBlockEngine.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                            "[Runtime] " + msg + " Cause: " + t.getClass().getSimpleName() + ": " + t.getMessage(), t);
                }
            }
        }
    }
}
