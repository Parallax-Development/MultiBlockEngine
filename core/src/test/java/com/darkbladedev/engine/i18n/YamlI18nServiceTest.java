package com.darkbladedev.engine.i18n;

import com.darkbladedev.engine.api.i18n.LocaleProvider;
import com.darkbladedev.engine.api.i18n.MessageKey;
import com.darkbladedev.engine.api.logging.CoreLogger;
import com.darkbladedev.engine.api.logging.LogBackend;
import com.darkbladedev.engine.api.logging.LogLevel;
import com.darkbladedev.engine.api.logging.LoggingConfig;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class YamlI18nServiceTest {

    private static CoreLogger testLogger() {
        LogBackend backend = entry -> {
        };
        LoggingConfig config = new LoggingConfig(LogLevel.INFO, false, false, false, Set.of());
        return new CoreLogger("test", backend, config);
    }

    private static LocaleProvider fixedLocale(Locale fixed) {
        return new LocaleProvider() {
            @Override
            public Locale localeOf(CommandSender sender) {
                return fixed;
            }

            @Override
            public Locale localeOf(UUID playerId) {
                return fixed;
            }

            @Override
            public Locale fallbackLocale() {
                return fixed;
            }
        };
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    @Test
    void debugMissingKeysWrapsFallback(@TempDir Path dir) throws Exception {
        write(dir.resolve("lang/en_us/core.yml"), "{}\n");
        YamlI18nService svc = new YamlI18nService(
                dir.toFile(),
                List::of,
                testLogger(),
                fixedLocale(Locale.US),
                () -> true
        );

        assertEquals("??mbe:missing.key??", svc.resolve(MessageKey.of("mbe", "missing.key"), Locale.US));
    }

    @Test
    void fallsBackToEnUsWhenLocaleMissing(@TempDir Path dir) throws Exception {
        write(dir.resolve("lang/en_us/core.yml"), "greeting: \"Hello\"\n");
        YamlI18nService svc = new YamlI18nService(
                dir.toFile(),
                List::of,
                testLogger(),
                fixedLocale(Locale.US),
                () -> false
        );

        assertEquals("Hello", svc.resolve(MessageKey.of("mbe", "greeting"), Locale.forLanguageTag("fr-FR")));
    }

    @Test
    void selectsPluralCategoryForRussian(@TempDir Path dir) throws Exception {
        write(dir.resolve("lang/ru_ru/items.yml"),
                "items:\n" +
                "  count:\n" +
                "    one: \"{count} предмет\"\n" +
                "    few: \"{count} предмета\"\n" +
                "    many: \"{count} предметов\"\n" +
                "    other: \"{count} предмета\"\n"
        );
        YamlI18nService svc = new YamlI18nService(
                dir.toFile(),
                List::of,
                testLogger(),
                fixedLocale(Locale.forLanguageTag("ru-RU")),
                () -> false
        );

        MessageKey key = MessageKey.of("mbe", "items.count");
        assertEquals("1 предмет", svc.resolve(key, Locale.forLanguageTag("ru-RU"), Map.of("count", 1)));
        assertEquals("2 предмета", svc.resolve(key, Locale.forLanguageTag("ru-RU"), Map.of("count", 2)));
        assertEquals("5 предметов", svc.resolve(key, Locale.forLanguageTag("ru-RU"), Map.of("count", 5)));
    }

    @Test
    void formatsNumbersUsingLocale(@TempDir Path dir) throws Exception {
        write(dir.resolve("lang/es_es/core.yml"), "fmt: \"Valor: {n}\"\n");
        Locale es = Locale.forLanguageTag("es-ES");
        YamlI18nService svc = new YamlI18nService(
                dir.toFile(),
                List::of,
                testLogger(),
                fixedLocale(es),
                () -> false
        );

        String expected = "Valor: " + NumberFormat.getInstance(es).format(1234);
        assertEquals(expected, svc.resolve(MessageKey.of("mbe", "fmt"), es, Map.of("n", 1234)));
    }

    @Test
    void fallsBackToCoreOriginWhenAddonMissingKey(@TempDir Path dir) throws Exception {
        Path addonDir = dir.resolve("addon");

        write(dir.resolve("lang/en_us/core.yml"), "shared: \"Core\"\n");
        write(addonDir.resolve("lang/en_us/core.yml"), "{}\n");

        YamlI18nService svc = new YamlI18nService(
                dir.toFile(),
                () -> List.of(new YamlI18nService.I18nSource("addon1", addonDir.toFile())),
                testLogger(),
                fixedLocale(Locale.US),
                () -> false
        );

        assertEquals("Core", svc.resolve(MessageKey.of("addon1", "shared"), Locale.US));
    }
}
