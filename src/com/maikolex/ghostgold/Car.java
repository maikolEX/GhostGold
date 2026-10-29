package com.maikolex.ghostgold;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.util.Locale;

/**
 * What GHOST GOLD knows about the truck: GPS speed, the odometer (dashboard reading set by the
 * driver plus GPS distance since), and the current trip. Written by {@link CarService}.
 */
final class Car {

    private Car() { }

    /** Average consumption shown by the CAN box (L/100 km), used for fuel estimates. */
    static final float L_PER_100 = 8.4f;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("car", Context.MODE_PRIVATE);
    }

    // ---------- Speed ----------

    static volatile float speed;    // km/h
    static volatile long speedAt;   // elapsedRealtime of the last good fix

    /** km/h, or 0 when no fix arrived lately (the GPS only reports while moving). */
    static int speed() {
        return SystemClock.elapsedRealtime() - speedAt > 6000 ? 0 : Math.round(speed);
    }

    // ---------- Odometer ----------

    static boolean odoSet(Context c) {
        return prefs(c).contains("odo_base");
    }

    static int km(Context c) {
        SharedPreferences p = prefs(c);
        return Math.round(p.getInt("odo_base", 0) + p.getFloat("odo_gps", 0));
    }

    static void setKm(Context c, int km) {
        prefs(c).edit().putInt("odo_base", km).putFloat("odo_gps", 0).apply();
    }

    static void addMeters(Context c, float m) {
        SharedPreferences p = prefs(c);
        p.edit().putFloat("odo_gps", p.getFloat("odo_gps", 0) + m / 1000f).apply();
    }

    static String fmt(int n) {
        return String.format(new Locale("es", "CR"), "%,d", n).replace(',', '.').replace(' ', '.');
    }

    // ---------- Trip ----------

    private static final long NEW_TRIP_AFTER = 20 * 60_000;

    static void tripMove(Context c, float meters, float kmh, float secs) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        SharedPreferences.Editor e = p.edit();
        boolean fresh = now - p.getLong("trip_last", 0) > NEW_TRIP_AFTER || p.getBoolean("trip_over", false);
        float m = fresh ? 0 : p.getFloat("trip_m", 0);
        float max = fresh ? 0 : p.getFloat("trip_max", 0);
        float mv = fresh ? 0 : p.getFloat("trip_s", 0);
        if (fresh) e.putLong("trip_start", now).putBoolean("trip_over", false).putBoolean("trip_shown", false);
        e.putFloat("trip_m", m + meters).putFloat("trip_max", Math.max(max, kmh)).putFloat("trip_s", mv + secs)
                .putLong("trip_last", now).apply();
    }

    static float tripKm(Context c) { return prefs(c).getFloat("trip_m", 0) / 1000f; }
    static int tripMax(Context c) { return Math.round(prefs(c).getFloat("trip_max", 0)); }
    static float tripMovingHours(Context c) { return prefs(c).getFloat("trip_s", 0) / 3600f; }
    static long tripMillis(Context c) {
        SharedPreferences p = prefs(c);
        return Math.max(0, p.getLong("trip_last", 0) - p.getLong("trip_start", 0));
    }
    static long tripLastMove(Context c) { return prefs(c).getLong("trip_last", 0); }
}
