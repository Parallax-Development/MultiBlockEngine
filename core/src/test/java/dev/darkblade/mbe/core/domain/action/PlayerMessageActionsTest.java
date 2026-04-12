package dev.darkblade.mbe.core.domain.action;

import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import dev.darkblade.mbe.api.message.PlayerMessageService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMessageActionsTest {

    @AfterEach
    void tearDown() {
        PlayerMessageServiceLocator.override(null);
    }

    @Test
    void sendMessageActionUsesPlayerMessageService() {
        RecordingMessageService service = new RecordingMessageService();
        PlayerMessageServiceLocator.override(service);
        Player player = player();
        MessageKey key = MessageKey.of("mbe", "core.message.test");

        new SendMessageAction(key, Map.of(), null).execute(null, player);

        assertEquals(1, service.sent.size());
        PlayerMessage message = service.sent.getFirst().message();
        assertEquals(key.fullKey(), message.key().fullKey());
        assertEquals(MessageChannel.CHAT, message.channel());
        assertEquals(MessagePriority.NORMAL, message.priority());
    }

    @Test
    void actionBarActionUsesPlayerMessageService() {
        RecordingMessageService service = new RecordingMessageService();
        PlayerMessageServiceLocator.override(service);
        Player player = player();
        MessageKey key = MessageKey.of("mbe", "core.actionbar.test");

        new ActionBarAction(key, Map.of(), null).execute(null, player);

        assertEquals(1, service.sent.size());
        PlayerMessage message = service.sent.getFirst().message();
        assertEquals(key.fullKey(), message.key().fullKey());
        assertEquals(MessageChannel.ACTION_BAR, message.channel());
        assertEquals(MessagePriority.LOW, message.priority());
    }

    @Test
    void titleActionUsesPlayerMessageServiceForTitleAndSubtitle() {
        RecordingMessageService service = new RecordingMessageService();
        PlayerMessageServiceLocator.override(service);
        Player player = player();
        MessageKey title = MessageKey.of("mbe", "core.title.main");
        MessageKey subtitle = MessageKey.of("mbe", "core.title.sub");

        new TitleAction(title, subtitle, Map.of(), null).execute(null, player);

        assertEquals(2, service.sent.size());
        PlayerMessage titleMessage = service.sent.get(0).message();
        PlayerMessage subtitleMessage = service.sent.get(1).message();
        assertEquals(MessageChannel.TITLE, titleMessage.channel());
        assertEquals(MessagePriority.CRITICAL, titleMessage.priority());
        assertEquals(MessageChannel.SUBTITLE, subtitleMessage.channel());
        assertEquals(MessagePriority.CRITICAL, subtitleMessage.priority());
    }

    private Player player() {
        UUID uuid = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) {
                        return uuid;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0.0F;
        }
        if (type == Double.TYPE) {
            return 0.0D;
        }
        if (type == Character.TYPE) {
            return '\0';
        }
        return null;
    }

    private static final class RecordingMessageService implements PlayerMessageService {
        private final List<SentMessage> sent = new ArrayList<>();

        @Override
        public String getServiceId() {
            return "mbe:test.player_message";
        }

        @Override
        public void send(Player player, PlayerMessage message) {
            sent.add(new SentMessage(player, message));
        }
    }

    private record SentMessage(Player player, PlayerMessage message) {
    }
}
