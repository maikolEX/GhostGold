package com.maikolex.ghostgold;

import android.content.Context;
import android.content.SharedPreferences;

/** Driver settings: name, plate digit, preferred map app. */
final class Store {

    private Store() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("ghostgold", Context.MODE_PRIVATE);
    }

    static String name(Context c) {
        return prefs(c).getString("name", "Maikol").trim();
    }

    static void setName(Context c, String n) {
        prefs(c).edit().putString("name", n.trim()).apply();
    }

    /** Last digit of the plate (8 for the Rich 6). */
    static int plate(Context c) {
        return prefs(c).getInt("plate", 8);
    }

    static void setPlate(Context c, int d) {
        prefs(c).edit().putInt("plate", d).apply();
    }

    static final String WAZE = "com.waze", GMAPS = "com.google.android.apps.maps";

    static String maps(Context c) {
        return prefs(c).getString("maps", WAZE);
    }

    static void setMaps(Context c, String pkg) {
        prefs(c).edit().putString("maps", pkg).apply();
    }

    static String greeting(Context c) {
        int h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String g = h < 12 ? "Buenos días" : h < 19 ? "Buenas tardes" : "Buenas noches";
        String n = name(c);
        return n.isEmpty() ? g : g + ", " + n;
    }
}
