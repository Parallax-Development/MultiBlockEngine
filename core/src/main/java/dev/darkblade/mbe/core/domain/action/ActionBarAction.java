package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.internal.tooling.PlayerResolver;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public record ActionBarAction(MessageKey key, Map<String, Object> params, Object target) implements Action {
    @Override
    public void execute(MultiblockInstance instance, Player player) {
        Collection<Player> targets = PlayerResolver.resolve(target, instance, player);
        if (targets.isEmpty() || key == null) {
            return;
        }
        PlayerMessageService messageService = PlayerMessageServiceLocator.resolve();
        if (messageService == null) {
            return;
        }

        Map<String, Object> resolvedParams = resolveParams(instance);
        for (Player p : targets) {
            messageService.send(p, new PlayerMessage(
                    key,
                    MessageChannel.ACTION_BAR,
                    MessagePriority.LOW,
                    resolvedParams
            ));
        }
    }

    private Map<String, Object> resolveParams(MultiblockInstance instance) {
        if (params == null || params.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry == null || entry.getKey() == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String text) {
                out.put(entry.getKey(), StringUtil.parsePlaceholders(text, instance));
                continue;
            }
            out.put(entry.getKey(), value);
        }
        return Map.copyOf(out);
    }
}
