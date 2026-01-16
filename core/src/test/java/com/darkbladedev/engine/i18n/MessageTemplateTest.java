package com.darkbladedev.engine.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTemplateTest {

    @Test
    void rendersPlaceholdersAndKeepsMissingOnes() {
        MessageTemplate t = MessageTemplate.compile("Used {used}/{max} blocks");
        assertEquals("Used 3/10 blocks", t.render(Map.of("used", 3, "max", 10)));
        assertEquals("Used 3/{max} blocks", t.render(Map.of("used", 3)));
    }

    @Test
    void invalidPlaceholdersRemainLiteral() {
        MessageTemplate t = MessageTemplate.compile("Hello {user name} and {user-name}!");
        assertEquals("Hello {user name} and x!", t.render(Map.of("user-name", "x")));
    }

    @Test
    void localeParsingMatchesMinecraftLocaleAndNormalizesKey() {
        Locale es = LocaleParsing.parseMinecraftLocale("es_es");
        assertEquals(new Locale("es", "ES"), es);
        assertEquals("es_es", LocaleParsing.toLocaleKey(es));
        assertEquals("en_us", LocaleParsing.toLocaleKey(null));
        assertEquals("es", LocaleParsing.toLocaleKey(new Locale("es")));
    }
}
