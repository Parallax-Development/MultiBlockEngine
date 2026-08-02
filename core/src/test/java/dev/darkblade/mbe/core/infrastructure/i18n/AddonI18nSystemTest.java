package dev.darkblade.mbe.core.infrastructure.i18n;

import dev.darkblade.mbe.api.i18n.AddonI18n;
import dev.darkblade.mbe.api.i18n.I18nService;
import dev.darkblade.mbe.api.i18n.LocaleProvider;
import dev.darkblade.mbe.api.logging.CoreLogger;
import dev.darkblade.mbe.api.logging.LogKv;
import dev.darkblade.mbe.api.logging.LogLevel;
import dev.darkblade.mbe.api.logging.LogPhase;
import dev.darkblade.mbe.api.logging.LogScope;

import dev.darkblade.mbe.api.service.InjectService;
import dev.darkblade.mbe.api.service.ServiceScope;
import dev.darkblade.mbe.api.service.UnifiedServiceRegistry;
import dev.darkblade.mbe.core.application.service.DefaultResolutionPolicy;
import dev.darkblade.mbe.core.application.service.DefaultServiceDescriptor;
import dev.darkblade.mbe.core.application.service.DefaultUnifiedServiceRegistry;
import dev.darkblade.mbe.core.application.service.ServiceInjector;
import dev.darkblade.mbe.core.internal.tooling.StringUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AddonI18nSystemTest {

    @TempDir
    Path tempDir;

    private YamlI18nService yamlI18nService;
    private CoreLogger mockLogger;
    private LocaleProvider localeProvider;

    @BeforeEach
    void setUp() throws IOException {
        File coreLangDir = new File(tempDir.toFile(), "lang");
        coreLangDir.mkdirs();

        File enFile = new File(coreLangDir, "en_us.yml");
        try (FileWriter writer = new FileWriter(enFile)) {
            writer.write("core:\n  welcome: \"Welcome to MBE {name}\"\n");
        }

        File addonDir = new File(tempDir.toFile(), "addons/test_addon/lang");
        addonDir.mkdirs();
        File addonEnFile = new File(addonDir, "en_us.yml");
        try (FileWriter writer = new FileWriter(addonEnFile)) {
            writer.write("messages:\n  hello: \"<green>Hello</green> &b{player}\"\n  lore:\n    - \"&aLine 1\"\n    - \"<gold>Line 2 {val}</gold>\"\n");
        }

        mockLogger = testLogger();

        localeProvider = new LocaleProvider() {
            @Override
            public Locale localeOf(CommandSender sender) {
                return Locale.US;
            }

            @Override
            public Locale localeOf(java.util.UUID playerId) {
                return Locale.US;
            }
        };

        YamlI18nService.I18nSource addonSource = new YamlI18nService.I18nSource("test_addon", new File(tempDir.toFile(), "addons/test_addon"));
        yamlI18nService = new YamlI18nService(
                tempDir.toFile(),
                () -> List.of(addonSource),
                mockLogger,
                localeProvider,
                () -> false
        );
    }

    @Test
    void testDefaultAddonI18nPreBoundOrigin() {
        AddonI18n addonI18n = new DefaultAddonI18n(yamlI18nService, "test_addon");
        assertEquals("test_addon", addonI18n.addonId());

        String rendered = addonI18n.tr(null, "messages.hello", Map.of("player", "Player1"));
        assertNotNull(rendered);
        assertTrue(rendered.contains("Player1"));

        List<String> lore = addonI18n.trList(null, "messages.lore", Map.of("val", "100"));
        assertEquals(2, lore.size());
        assertTrue(lore.get(1).contains("Line 2 100"));
    }

    @Test
    void testServiceInjectorAddonI18nField() {
        UnifiedServiceRegistry registry = new DefaultUnifiedServiceRegistry(new DefaultResolutionPolicy());
        registry.registerService(new DefaultServiceDescriptor<>(
                I18nService.class.getName(),
                "mbe",
                I18nService.class,
                yamlI18nService,
                ServiceScope.GLOBAL,
                0,
                false,
                true
        ));

        ServiceInjector injector = new ServiceInjector(registry, mockLogger);
        TestAddonComponent component = new TestAddonComponent();

        injector.inject(component, "my_custom_addon");

        assertNotNull(component.i18n);
        assertEquals("my_custom_addon", component.i18n.addonId());

        assertTrue(component.optionalI18n.isPresent());
        assertEquals("my_custom_addon", component.optionalI18n.get().addonId());
    }

    @Test
    void testDualMiniMessageAndLegacyColorFormatting() {
        Component comp = StringUtil.parseFormattedComponent("&a[MBE] <red><bold>Status:</bold></red> Active");
        assertNotNull(comp);

        String legacy = StringUtil.parseFormattedLegacy("&a[MBE] <red><bold>Status:</bold></red> Active");
        assertNotNull(legacy);
        assertTrue(legacy.contains("§a") || legacy.contains("§c") || legacy.contains("[MBE]"));
    }

    @Test
    void testI18nPlaceholderResolver() {
        I18nPlaceholderResolver resolver = new I18nPlaceholderResolver(yamlI18nService);

        String input = "Header: %i18n:test_addon:messages.hello%";
        String output = resolver.resolveInlinePlaceholders(input, null, "test_addon", Map.of("player", "Alice"));

        assertNotNull(output);
        assertTrue(output.contains("Alice"));
    }

    private static CoreLogger testLogger() {
        dev.darkblade.mbe.api.logging.LogBackend backend = entry -> {};
        dev.darkblade.mbe.api.logging.LoggingConfig config = new dev.darkblade.mbe.api.logging.LoggingConfig(LogLevel.INFO, false, false, false, Set.of());
        return new CoreLogger("test", backend, config);
    }

    static class TestAddonComponent {
        @InjectService
        private AddonI18n i18n;

        @InjectService
        private Optional<AddonI18n> optionalI18n;
    }
}
