package dev.darkblade.mbe.platform.bukkit.preview.version;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.entity.Player;

public final class VersionResolver {
    private final ProtocolManager protocolManager;

    public VersionResolver() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public ProtocolVersion resolve(Player player) {
        if (player == null || protocolManager == null) {
            return ProtocolVersion.UNKNOWN;
        }
        try {
            int protocolId = protocolManager.getProtocolVersion(player);
            return map(protocolId);
        } catch (RuntimeException ex) {
            org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.WARNING, "[MBE Preview] Protocol resolution failed: " + ex.getMessage());
            return ProtocolVersion.UNKNOWN;
        }
    }

    private ProtocolVersion map(int protocolId) {
        return switch (protocolId) {
            case 762 -> ProtocolVersion.V1_19_4;
            case 763 -> ProtocolVersion.V1_20;
            case 764 -> ProtocolVersion.V1_20_2;
            case 765 -> ProtocolVersion.V1_20_4;
            case 766 -> ProtocolVersion.V1_20_5;
            case 767 -> ProtocolVersion.V1_21;
            case 768 -> ProtocolVersion.V1_21_2;
            case 769 -> ProtocolVersion.V1_21_4;
            case 770 -> ProtocolVersion.V1_21_5;
            case 771 -> ProtocolVersion.V1_21_6;
            case 772 -> ProtocolVersion.V1_21_7_OR_8;
            case 773 -> ProtocolVersion.V1_21_9;
            case 774 -> ProtocolVersion.V1_21_11;
            default -> protocolId > 774 ? ProtocolVersion.V1_21_11 : ProtocolVersion.UNKNOWN;
        };
    }
}
