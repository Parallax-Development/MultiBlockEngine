package dev.darkblade.mbe.core.application.service.messaging;

import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.LocaleProvider;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.api.message.MessageChannel;
import dev.darkblade.mbe.api.message.MessagePriority;
import dev.darkblade.mbe.api.message.PlayerMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerMessageServiceImplTest {
    private static final MessageKey KEY_MODE = MessageKey.of("mbe", "tool.mode.changed");
    private static final MessageKey KEY_ERROR = MessageKey.of("mbe", "tool.error.no_permission");
    private static final MessageKey KEY_ALERT = MessageKey.of("mbe", "tool.alert.critical");

    @Test
    void resolvesMessageWithI18nAndSendsToChat() {
        PlayerMessageServiceImpl service = new PlayerMessageServiceImpl(new TestI18nService(Map.of(
                KEY_MODE.fullKey(), "Mode: {mode}"
        )));
        PlayerProbe probe = new PlayerProbe();

        service.send(probe.player(), new PlayerMessage(
                KEY_MODE,
                MessageChannel.CHAT,
                MessagePriority.NORMAL,
                Map.of("mode", "configure")
        ));

        assertEquals(1, probe.chat().size());
        assertEquals("Mode: configure", probe.chatText(0));
    }

    @Test
    void actionBarDoesNotSpamRepeatedMessagesWithinDebounceWindow() {
        PlayerMessageServiceImpl service = new PlayerMessageServiceImpl(new TestI18nService(Map.of(
                KEY_MODE.fullKey(), "Mode: {mode}"
        )));
        PlayerProbe probe = new PlayerProbe();
        PlayerMessage message = new PlayerMessage(
                KEY_MODE,
                MessageChannel.ACTION_BAR,
                MessagePriority.LOW,
                Map.of("mode", "link")
        );

        service.send(probe.player(), message);
        service.send(probe.player(), message);

        assertEquals(1, probe.actionBar().size());
        assertEquals("Mode: link", probe.actionBarText(0));
    }

    @Test
    void routesByPriorityWhenChannelIsNull() {
        PlayerMessageServiceImpl service = new PlayerMessageServiceImpl(new TestI18nService(Map.of(
                KEY_MODE.fullKey(), "Mode: {mode}",
                KEY_ERROR.fullKey(), "No permission",
                KEY_ALERT.fullKey(), "Critical failure"
        )));
        PlayerProbe probe = new PlayerProbe();

        service.send(probe.player(), new PlayerMessage(
                KEY_MODE,
                null,
                MessagePriority.LOW,
                Map.of("mode", "inspect")
        ));
        service.send(probe.player(), new PlayerMessage(
                KEY_ERROR,
                null,
                MessagePriority.NORMAL,
                Map.of()
        ));
        service.send(probe.player(), new PlayerMessage(
                KEY_ALERT,
                null,
                MessagePriority.CRITICAL,
                Map.of()
        ));

        assertEquals(1, probe.actionBar().size());
        assertEquals("Mode: inspect", probe.actionBarText(0));
        assertEquals(1, probe.chat().size());
        assertEquals("No permission", probe.chatText(0));
        assertEquals(1, probe.titles().size());
        assertEquals("Critical failure", probe.titleText(0));
    }

    private static final class PlayerProbe {
        private final UUID uuid = UUID.randomUUID();
        private final List<Component> chat = new ArrayList<>();
        private final List<Component> actionBar = new ArrayList<>();
        private final List<Title> titles = new ArrayList<>();
        private final Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getUniqueId")) {
                        return uuid;
                    }
                    if (name.equals("sendMessage") && args != null && args.length == 1 && args[0] instanceof Component component) {
                        chat.add(component);
                        return null;
                    }
                    if (name.equals("sendActionBar") && args != null && args.length == 1 && args[0] instanceof Component component) {
                        actionBar.add(component);
                        return null;
                    }
                    if (name.equals("showTitle") && args != null && args.length == 1 && args[0] instanceof Title title) {
                        titles.add(title);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                }
        );

        private Player player() {
            return player;
        }

        private List<Component> chat() {
            return chat;
        }

        private List<Component> actionBar() {
            return actionBar;
        }

        private List<Title> titles() {
            return titles;
        }

        private String chatText(int index) {
            return PlainTextComponentSerializer.plainText().serialize(chat.get(index));
        }

        private String actionBarText(int index) {
            return PlainTextComponentSerializer.plainText().serialize(actionBar.get(index));
        }

        private String titleText(int index) {
            return PlainTextComponentSerializer.plainText().serialize(titles.get(index).title());
        }
    }

    private static final class TestI18nService implements I18nService {
        private final Map<String, String> translations;

        private TestI18nService(Map<String, String> translations) {
            this.translations = translations;
        }

        @Override
        public LocaleProvider localeProvider() {
            return new LocaleProvider() {
                @Override
                public Locale localeOf(CommandSender sender) {
                    return Locale.US;
                }

                @Override
                public Locale localeOf(UUID playerId) {
                    return Locale.US;
                }

                @Override
                public Locale fallbackLocale() {
                    return Locale.US;
                }
            };
        }

        @Override
        public String resolve(MessageKey key, Locale locale) {
            return resolve(key, locale, Map.of());
        }

        @Override
        public String resolve(MessageKey key, Locale locale, Map<String, ?> params) {
            if (key == null) {
                return "";
            }
            String template = translations.getOrDefault(key.fullKey(), key.fullKey());
            String out = template;
            for (Map.Entry<String, ?> entry : params.entrySet()) {
                out = out.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
            return out;
        }

        @Override
        public void reload() {
        }
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
        if (type == Duration.class) {
            return Duration.ZERO;
        }
        return null;
    }
}
