package com.darkbladedev.engine.model;

import java.util.Objects;

public record MultiblockSource(Type type, String path) {

    public enum Type {
        CORE_DEFAULT,
        USER_DEFINED
    }

    public MultiblockSource {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(path, "path");
    }
}

