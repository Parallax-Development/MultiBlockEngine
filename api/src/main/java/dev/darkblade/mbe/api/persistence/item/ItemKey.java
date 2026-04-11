package dev.darkblade.mbe.api.persistence.item;

import org.jetbrains.annotations.Nullable;

public interface ItemKey extends Comparable<ItemKey> {

    NamespacedKey type();

    int damage();

    @Nullable String nbtHash();

    @Override
    default int compareTo(ItemKey o) {
        if (o == null) {
            return 1;
        }
        int t = type().compareTo(o.type());
        if (t != 0) {
            return t;
        }
        int d = Integer.compare(damage(), o.damage());
        if (d != 0) {
            return d;
        }
        String a = nbtHash();
        String b = o.nbtHash();
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }
}
