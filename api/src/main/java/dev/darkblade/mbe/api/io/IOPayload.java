package dev.darkblade.mbe.api.io;

public interface IOPayload {

    ChannelType getType();

    Object getContent();
}
