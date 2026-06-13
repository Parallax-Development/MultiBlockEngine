package dev.darkblade.mbe.platform.bukkit.preview.impl.v1_21.base;

import dev.darkblade.mbe.platform.bukkit.preview.bridge.PacketEventsAdapter;
import dev.darkblade.mbe.platform.bukkit.preview.impl.AbstractPacketEventsRenderer;

public abstract class AbstractRenderer_1_21 extends AbstractPacketEventsRenderer {
    protected AbstractRenderer_1_21(PacketEventsAdapter adapter) {
        super(adapter, 23, 8, 9, 10, 11);
    }
}
