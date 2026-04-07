package dev.darkblade.mbe.core.platform.listener;

import dev.darkblade.mbe.api.event.MultiblockBreakEvent;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.LocaleProvider;
import dev.darkblade.mbe.api.i18n.MessageKey;
import dev.darkblade.mbe.core.application.service.MultiblockRuntimeService;
import dev.darkblade.mbe.core.domain.DisplayNameConfig;
import dev.darkblade.mbe.core.domain.MultiblockInstance;
import dev.darkblade.mbe.core.domain.MultiblockType;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class MultiblockListenerBreakEventTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void breakEventIsFiredAndInstanceDestroyedWhenNotCancelled() {
        Location loc = new Location(world, 10, 64, 10);

        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        Consumer<Event> caller = events::add;
        MultiblockListener listener = new MultiblockListener(manager, caller);

        Block block = world.getBlockAt(10, 64, 10);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);

        listener.onBlockBreak(breakEvent);

        assertTrue(manager.destroyed);
        assertFalse(breakEvent.isCancelled());
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MultiblockBreakEvent);
        MultiblockBreakEvent fired = (MultiblockBreakEvent) events.get(0);
        assertSame(instance, fired.getMultiblock());
        assertFalse(fired.isCancelled());
    }

    @Test
    void breakCancellationPreventsDestroy() {
        Location loc = new Location(world, 10, 64, 10);

        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);

        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        Consumer<Event> caller = e -> {
            events.add(e);
            if (e instanceof MultiblockBreakEvent mbe) {
                mbe.setCancelled(true);
            }
        };
        MultiblockListener listener = new MultiblockListener(manager, caller);

        Block block = world.getBlockAt(10, 64, 10);
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);

        listener.onBlockBreak(breakEvent);

        assertFalse(manager.destroyed);
        assertTrue(breakEvent.isCancelled());
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MultiblockBreakEvent);
        assertTrue(((MultiblockBreakEvent) events.get(0)).isCancelled());
    }

    @Test
    void disassembledMessageUsesEnglishTranslation() {
        Location loc = new Location(world, 10, 64, 10);
        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);
        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        TestI18nService i18n = new TestI18nService(Map.of(
                "en_us", "Structure destroyed: {type}",
                "es_es", "Estructura destruida: {type}"
        ));
        i18n.setLocale(player.getUniqueId(), Locale.forLanguageTag("en-US"));

        MultiblockListener listener = new MultiblockListener(manager, events::add, null, null, i18n);
        BlockBreakEvent breakEvent = new BlockBreakEvent(world.getBlockAt(10, 64, 10), player);

        listener.onBlockBreak(breakEvent);

        assertEquals("Structure destroyed: storage:disk", ChatColor.stripColor(player.nextMessage()));
        assertEquals(MessageKey.of("mbe", "core.wrench.disassembled").fullKey(), i18n.lastKey);
        assertEquals("storage:disk", i18n.lastParams.get("type"));
    }

    @Test
    void disassembledMessageUsesSpanishTranslation() {
        Location loc = new Location(world, 10, 64, 10);
        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);
        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        TestI18nService i18n = new TestI18nService(Map.of(
                "en_us", "Structure destroyed: {type}",
                "es_es", "Estructura destruida: {type}"
        ));
        i18n.setLocale(player.getUniqueId(), Locale.forLanguageTag("es-ES"));

        MultiblockListener listener = new MultiblockListener(manager, events::add, null, null, i18n);
        BlockBreakEvent breakEvent = new BlockBreakEvent(world.getBlockAt(10, 64, 10), player);

        listener.onBlockBreak(breakEvent);

        assertEquals("Estructura destruida: storage:disk", ChatColor.stripColor(player.nextMessage()));
        assertEquals(MessageKey.of("mbe", "core.wrench.disassembled").fullKey(), i18n.lastKey);
        assertEquals("storage:disk", i18n.lastParams.get("type"));
    }

    @Test
    void disassembledMessageFallsBackWhenTranslationMissing() {
        Location loc = new Location(world, 10, 64, 10);
        MultiblockInstance instance = new MultiblockInstance(dummyType("storage:disk"), loc, BlockFace.NORTH);
        TestManager manager = new TestManager(instance);
        List<Event> events = new ArrayList<>();
        TestI18nService i18n = new TestI18nService(Map.of());
        i18n.setLocale(player.getUniqueId(), Locale.forLanguageTag("es-ES"));

        MultiblockListener listener = new MultiblockListener(manager, events::add, null, null, i18n);
        BlockBreakEvent breakEvent = new BlockBreakEvent(world.getBlockAt(10, 64, 10), player);

        listener.onBlockBreak(breakEvent);

        assertEquals("mbe:core.wrench.disassembled", ChatColor.stripColor(player.nextMessage()));
        assertEquals(MessageKey.of("mbe", "core.wrench.disassembled").fullKey(), i18n.lastKey);
        assertEquals("storage:disk", i18n.lastParams.get("type"));
    }

    private static MultiblockType dummyType(String id) {
        return new MultiblockType(
                id,
                "1.0",
                new Vector(0, 0, 0),
                block -> false,
                List.of(),
                false,
                java.util.Map.of(),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new DisplayNameConfig("", false, "hologram"),
                20,
                List.of()
        );
    }

    private static final class TestManager extends MultiblockRuntimeService {
        private MultiblockInstance instance;
        private boolean destroyed;

        private TestManager(MultiblockInstance instance) {
            this.instance = instance;
        }

        @Override
        public Optional<MultiblockInstance> getInstanceAt(Location loc) {
            return Optional.ofNullable(instance);
        }

        @Override
        public void destroyInstance(MultiblockInstance instance) {
            this.destroyed = true;
            this.instance = null;
        }
    }

    private static final class TestI18nService implements I18nService {
        private final Map<String, String> translations;
        private final Map<UUID, Locale> locales = new ConcurrentHashMap<>();
        private String lastKey;
        private Map<String, ?> lastParams = Map.of();

        private TestI18nService(Map<String, String> translations) {
            this.translations = translations;
        }

        private void setLocale(UUID playerId, Locale locale) {
            locales.put(playerId, locale);
        }

        @Override
        public LocaleProvider localeProvider() {
            return new LocaleProvider() {
                @Override
                public Locale localeOf(CommandSender sender) {
                    if (sender instanceof PlayerMock p) {
                        return locales.getOrDefault(p.getUniqueId(), Locale.US);
                    }
                    return Locale.US;
                }

                @Override
                public Locale localeOf(UUID playerId) {
                    return locales.getOrDefault(playerId, Locale.US);
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
            lastKey = key == null ? "" : key.fullKey();
            lastParams = params == null ? Map.of() : Map.copyOf(params);
            String localeKey = locale == null ? "en_us" : locale.toLanguageTag().replace('-', '_').toLowerCase(Locale.ROOT);
            String template = translations.get(localeKey);
            if (template == null || template.isBlank()) {
                return key == null ? "" : key.fullKey();
            }
            Object typeValue = params == null ? null : params.get("type");
            String type = typeValue == null ? "" : String.valueOf(typeValue);
            return template.replace("{type}", type);
        }

        @Override
        public void reload() {
        }
    }
}
