package com.darkbladedev.engine.logging;

final class Throwables {

    private Throwables() {
    }

    static Throwable rootCause(Throwable t) {
        if (t == null) {
            return null;
        }
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }
}

