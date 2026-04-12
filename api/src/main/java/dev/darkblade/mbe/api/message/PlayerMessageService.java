package dev.darkblade.mbe.api.message;

import dev.darkblade.mbe.api.service.MBEService;
import org.bukkit.entity.Player;

public interface PlayerMessageService extends MBEService {

    void send(Player player, PlayerMessage message);
}
