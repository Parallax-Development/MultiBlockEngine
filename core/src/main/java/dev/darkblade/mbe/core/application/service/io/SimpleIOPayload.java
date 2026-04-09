package dev.darkblade.mbe.core.application.service.io;

import dev.darkblade.mbe.api.io.ChannelType;
import dev.darkblade.mbe.api.io.IOPayload;

import java.util.Objects;

public final class SimpleIOPayload implements IOPayload {

    private final ChannelType type;
    private final Object content;

    public SimpleIOPayload(ChannelType type, Object content) {
        this.type = Objects.requireNonNull(type, "type");
        this.content = content;
    }

    @Override
    public ChannelType getType() {
        return type;
    }

    @Override
    public Object getContent() {
        return content;
    }
}
