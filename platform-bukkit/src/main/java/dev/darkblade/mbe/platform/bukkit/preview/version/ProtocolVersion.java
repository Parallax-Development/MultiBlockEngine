package dev.darkblade.mbe.platform.bukkit.preview.version;

public enum ProtocolVersion {
    V1_19_4(762),
    V1_20(763),
    V1_20_2(764),
    V1_20_4(765),
    V1_20_5(766),
    V1_21(767),
    V1_21_2(768),
    V1_21_4(769),
    V1_21_5(770),
    V1_21_6(771),
    V1_21_7_OR_8(772),
    V1_21_9(773),
    V1_21_11(774),
    UNKNOWN(-1);

    private final int protocolId;

    ProtocolVersion(int protocolId) {
        this.protocolId = protocolId;
    }

    public int protocolId() {
        return protocolId;
    }

    public static ProtocolVersion fromProtocolId(int protocolId) {
        for (ProtocolVersion value : values()) {
            if (value.protocolId == protocolId) {
                return value;
            }
        }
        return UNKNOWN;
    }
}
