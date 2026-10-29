package com.maikolex.ghostgold;

import android.content.Context;

import java.util.Calendar;

/**
 * San José vehicle restriction (inside the Circunvalación, Mon-Fri 6:00-19:00) by the last
 * plate digit: Mon 1-2, Tue 3-4, Wed 5-6, Thu 7-8, Fri 9-0. Holidays and MOPT suspensions
 * are not known to the app.
 */
final class Rules {

    static final int FROM = 6, UNTIL = 19;
    private static final String[] DAY = {"", "domingos", "lunes", "martes", "miércoles", "jueves", "viernes", "sábados"};

    private Rules() { }

    static int day(int digit) {
        int d = ((digit % 10) + 10) % 10;
        if (d == 0) return Calendar.FRIDAY;
        return Calendar.MONDAY + (d - 1) / 2; // 1-2 Mon, 3-4 Tue, 5-6 Wed, 7-8 Thu, 9 Fri
    }

    private static boolean on(Context c, Calendar when) {
        return when.get(Calendar.DAY_OF_WEEK) == day(Store.plate(c));
    }

    static boolean today(Context c) {
        return on(c, Calendar.getInstance());
    }

    static boolean tomorrow(Context c) {
        Calendar t = Calendar.getInstance();
        t.add(Calendar.DAY_OF_MONTH, 1);
        return on(c, t);
    }

    static boolean now(Context c) {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return today(c) && h >= FROM && h < UNTIL;
    }

    static String dayName(Context c) {
        return DAY[day(Store.plate(c))];
    }

    static String line(Context c) {
        if (!today(c)) return "Hoy circulas · tu restricción: " + dayName(c);
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < FROM) return "Hoy no circulas en San José · desde las 6:00";
        if (h < UNTIL) return "Hoy no circulas en San José · hasta las 19:00";
        return "Restricción terminada · ya puedes circular";
    }
}
