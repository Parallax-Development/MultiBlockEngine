package dev.darkblade.mbe.core.application.service.security;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrustedCommandServiceTest {

    @TempDir
    File tempDir;

    private TrustedCommandServiceImpl service;
    private File configFile;

    @BeforeEach
    void setUp() throws IOException {
        service = new TrustedCommandServiceImpl(tempDir);
        configFile = new File(tempDir, "trusted_commands.yml");
    }

    @Test
    void testDefaultConfigCreation() {
        service.onEnable();
        assertTrue(configFile.exists());
        
        // Default should allow "say *"
        assertTrue(service.isTrusted("say hello"));
        assertTrue(service.isTrusted("say 123"));
    }

    @Test
    void testWhitelistEnforcement() throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("trusted_commands", List.of(
                "give %player% diamond",
                "mbe debug *",
                "tp %player% 0 100 0"
        ));
        config.save(configFile);
        
        service.reload();

        // Valid player names
        assertTrue(service.isTrusted("give darkblade diamond"));
        assertTrue(service.isTrusted("give User_123 diamond"));
        
        // Wildcards
        assertTrue(service.isTrusted("mbe debug on"));
        assertTrue(service.isTrusted("mbe debug off"));
        
        // Placeholder with invalid name (injection attempt)
        assertFalse(service.isTrusted("give darkblade; op darkblade diamond"));
        assertFalse(service.isTrusted("give darkblade diamond 64")); // Strict matching
        
        // Case insensitivity
        assertTrue(service.isTrusted("GIVE darkblade DIAMOND"));
        
        // Leading slash
        assertTrue(service.isTrusted("/mbe debug on"));
    }

    @Test
    void testUntrustedCommands() {
        service.onEnable(); // Load defaults
        
        assertFalse(service.isTrusted("op darkblade"));
        assertFalse(service.isTrusted("stop"));
        assertFalse(service.isTrusted("ban darkblade"));
    }
}
