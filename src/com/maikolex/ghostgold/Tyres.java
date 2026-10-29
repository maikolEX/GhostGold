package com.maikolex.ghostgold;

/**
 * Tyre pressures (PSI) and temperatures (°C) from the CAN box, front-left, front-right,
 * rear-left, rear-right. The factory CAN app keeps them to itself; until its interface is
 * known, nothing writes here and the home screen shows dashes plus a shortcut to its TPMS page.
 */
final class Tyres {

    private Tyres() { }

    static final String[] NAME = {"Del. izq.", "Del. der.", "Tras. izq.", "Tras. der."};
    static volatile float[] psi;   // null = unknown
    static volatile int[] temp;
    static final float LOW = 28f, HIGH = 44f;

    static boolean known() {
        return psi != null;
    }

    /** 0 fine, 1 check (low/high), for one tyre. */
    static int level(int i) {
        if (psi == null) return 0;
        return psi[i] < LOW || psi[i] > HIGH ? 1 : 0;
    }

    static String value(int i) {
        return psi == null ? "--" : String.valueOf(Math.round(psi[i]));
    }
}
