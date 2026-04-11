package dev.darkblade.mbe.api.persistence.item;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ItemKeys {

    private ItemKeys() {
    }

    public static ItemKey of(NamespacedKey type, int damage, @Nullable String nbtHash) {
        Objects.requireNonNull(type, "type");
        return new ValueItemKey(type, damage, nbtHash == null || nbtHash.isBlank() ? null : nbtHash);
    }

    public static ItemKey of(String namespacedType, int damage, @Nullable String nbtHash) {
        Objects.requireNonNull(namespacedType, "namespacedType");
        return of(NamespacedKey.parse(namespacedType), damage, nbtHash);
    }

    static final class ValueItemKey implements ItemKey {
        private final NamespacedKey type;
        private final int damage;
        private final String nbtHash;

        ValueItemKey(NamespacedKey type, int damage, @Nullable String nbtHash) {
            this.type = Objects.requireNonNull(type, "type");
            this.damage = damage;
            this.nbtHash = nbtHash;
        }

        @Override
        public NamespacedKey type() {
            return type;
        }

        @Override
        public int damage() {
            return damage;
        }

        @Override
        public @Nullable String nbtHash() {
            return nbtHash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ItemKey other)) {
                return false;
            }
            if (damage != other.damage()) {
                return false;
            }
            if (!type.equals(other.type())) {
                return false;
            }
            String b = other.nbtHash();
            return Objects.equals(nbtHash, b);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + damage;
            result = 31 * result + (nbtHash == null ? 0 : nbtHash.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "ItemKey{" +
                    "type=" + type +
                    ", damage=" + damage +
                    ", nbtHash=" + (nbtHash == null ? "null" : "'" + nbtHash + "'") +
                    '}';
        }
    }
}
