package com.maikolex.ghostgold;

import java.util.Calendar;

/**
 * Costa Rica legal holidays (the San José plate restriction does not apply on them) and the
 * periods the MOPT usually lifts it: Holy Week and the year-end break. 2026 from the published
 * calendar; 2027 by the fixed dates of the law (Holy Week from Easter). The usual suspensions are
 * "probable": the MOPT announces them each year.
 */
final class Feriados {

    private Feriados() { }

    static final class Day {
        final String name;
        final boolean probable; // usual suspension, to be confirmed by the MOPT
        Day(String name, boolean probable) { this.name = name; this.probable = probable; }
    }

    // {year, month (1-12), day, name}
    private static final Object[][] HOLIDAYS = {
            {2026, 1, 1, "Año Nuevo"}, {2026, 4, 2, "Jueves Santo"}, {2026, 4, 3, "Viernes Santo"},
            {2026, 4, 11, "Juan Santamaría"}, {2026, 5, 1, "Día del Trabajador"}, {2026, 7, 25, "Anexión de Nicoya"},
            {2026, 8, 2, "Virgen de los Ángeles"}, {2026, 8, 15, "Día de la Madre"},
            {2026, 8, 31, "Día de la Persona Negra"}, {2026, 9, 15, "Independencia"},
            {2026, 12, 1, "Abolición del Ejército"}, {2026, 12, 25, "Navidad"},
            {2027, 1, 1, "Año Nuevo"}, {2027, 3, 25, "Jueves Santo"}, {2027, 3, 26, "Viernes Santo"},
            {2027, 4, 11, "Juan Santamaría"}, {2027, 5, 1, "Día del Trabajador"}, {2027, 7, 25, "Anexión de Nicoya"},
            {2027, 8, 2, "Virgen de los Ángeles"}, {2027, 8, 15, "Día de la Madre"},
            {2027, 8, 31, "Día de la Persona Negra"}, {2027, 9, 15, "Independencia"},
            {2027, 12, 1, "Abolición del Ejército"}, {2027, 12, 25, "Navidad"},
    };

    // {fromY, fromM, fromD, toY, toM, toD, name}: weekdays the MOPT usually lifts the restriction.
    private static final Object[][] BREAKS = {
            {2026, 3, 30, 2026, 4, 3, "Semana Santa"},
            {2026, 12, 21, 2027, 1, 1, "vacaciones de fin de año"},
            {2027, 3, 22, 2027, 3, 26, "Semana Santa"},
            {2027, 12, 20, 2027, 12, 31, "vacaciones de fin de año"},
    };

    private static int key(int y, int m, int d) { return y * 10000 + m * 100 + d; }

    private static int key(Calendar c) {
        return key(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** The holiday or usual suspension on that day, or null. */
    static Day on(Calendar c) {
        int k = key(c);
        for (Object[] h : HOLIDAYS) {
            if (key((int) h[0], (int) h[1], (int) h[2]) == k) return new Day((String) h[3], false);
        }
        for (Object[] b : BREAKS) {
            if (k >= key((int) b[0], (int) b[1], (int) b[2]) && k <= key((int) b[3], (int) b[4], (int) b[5]))
                return new Day((String) b[6], true);
        }
        return null;
    }

    /** True when the calendar above covers that year (otherwise only weekdays are used). */
    static boolean known(Calendar c) {
        int y = c.get(Calendar.YEAR);
        return y == 2026 || y == 2027;
    }
}
