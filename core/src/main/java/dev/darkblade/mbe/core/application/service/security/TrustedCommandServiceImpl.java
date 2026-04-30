package dev.darkblade.mbe.core.application.service.security;

import dev.darkblade.mbe.api.service.security.TrustedCommandService;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TrustedCommandServiceImpl implements TrustedCommandService {

    private final File configFile;
    private List<Pattern> trustedPatterns = new ArrayList<>();

    public TrustedCommandServiceImpl(File dataFolder) {
        this.configFile = new File(Objects.requireNonNull(dataFolder, "dataFolder"), "trusted_commands.yml");
    }

    @Override
    public String getServiceId() {
        return "mbe:security.trusted_commands";
    }

    @Override
    public void onEnable() {
        reload();
    }

    @Override
    public boolean isTrusted(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        
        String cleanCommand = command.trim();
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.substring(1);
        }

        for (Pattern pattern : trustedPatterns) {
            if (pattern.matcher(cleanCommand).matches()) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void reload() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> rawPatterns = config.getStringList("trusted_commands");
        
        this.trustedPatterns = rawPatterns.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(this::toPattern)
                .collect(Collectors.toList());
    }

    private Pattern toPattern(String raw) {
        // More restrictive placeholders:
        // %player% and %owner% should only match valid Minecraft names
        // * is still a wildcard but we might want to escape it better
        
        String regex = Pattern.quote(raw)
                .replace("*", "\\E.*\\Q")
                .replace("%player%", "\\E[a-zA-Z0-9_]{3,16}\\Q")
                .replace("%owner%", "\\E[a-zA-Z0-9_]{3,16}\\Q");
        
        return Pattern.compile("^" + regex + "$", Pattern.CASE_INSENSITIVE);
    }

    private void createDefaultConfig() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("trusted_commands", List.of(
                    "say *",
                    "give %player% *",
                    "msg %player% *",
                    "mbe debug *"
            ));
            config.save(configFile);
        } catch (Exception ignored) {
        }
    }
}
