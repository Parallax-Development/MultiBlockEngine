package dev.darkblade.mbe.core.application.command.service;

import dev.darkblade.mbe.api.command.MbeCommandService;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicCommandServiceRegistryIntegrationTest {

    @Test
    void reflectsDynamicServiceChangesAfterRefreshWithoutRestart() {
        List<MbeCommandService> dynamicSource = new ArrayList<>();
        DynamicCommandServiceRegistry dynamicRegistry = new DynamicCommandServiceRegistry(() -> List.copyOf(dynamicSource));
        MbeCommandService service = new DynamicService();

        dynamicRegistry.refresh();
        assertFalse(dynamicRegistry.ids().contains(service.id()));

        dynamicSource.add(service);
        dynamicRegistry.refresh();
        assertTrue(dynamicRegistry.ids().contains(service.id()));
        assertTrue(dynamicRegistry.resolve(service.id()).isPresent());

        dynamicSource.clear();
        dynamicRegistry.refresh();
        assertFalse(dynamicRegistry.ids().contains(service.id()));
        assertFalse(dynamicRegistry.resolve(service.id()).isPresent());
    }

    private static final class DynamicService implements MbeCommandService {
        @Override
        public String id() {
            return "mbe_dynamic";
        }

        @Override
        public String description() {
            return "dynamic";
        }

        @Override
        public List<String> infoUsage() {
            return List.of();
        }

        @Override
        public List<String> executeUsage() {
            return List.of();
        }

        @Override
        public void info(CommandSender sender, List<String> args) {
        }

        @Override
        public void execute(CommandSender sender, List<String> args) {
        }
    }
}
