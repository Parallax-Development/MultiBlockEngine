package com.mbe.ui.api.menu;

public record MenuId(String namespace, String name) {
    public MenuId {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace is blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is blank");
        }
    }

    @Override
    public String toString() {
        return namespace + ":" + name;
    }
}

