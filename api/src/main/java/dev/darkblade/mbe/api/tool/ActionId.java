package dev.darkblade.mbe.api.tool;

public record ActionId(String namespace, String value) {

    public static ActionId of(String namespace, String value) {
        return new ActionId(namespace, value);
    }

    @Override
    public String toString() {
        return namespace + ":" + value;
    }
}
