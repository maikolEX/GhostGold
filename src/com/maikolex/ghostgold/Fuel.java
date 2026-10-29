package com.maikolex.ghostgold;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Fuel log: every fill-up (odometer, litres, colones, full tank or not). Consumption is measured
 * between two full tanks: distance driven / litres put in since the previous full tank.
 */
final class Fuel {

    static final class Fill {
        final long time;
        final int km;
        final float litres;
        final int colones;
        final boolean full;
        Fill(long time, int km, float litres, int colones, boolean full) {
            this.time = time; this.km = km; this.litres = litres; this.colones = colones; this.full = full;
        }
        float pricePerLitre() { return litres > 0 ? colones / litres : 0; }
    }

    private Fuel() { }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("fuel", Context.MODE_PRIVATE);
    }

    /** All fill-ups, oldest first (by odometer). */
    static List<Fill> all(Context c) {
        List<Fill> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("fills", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                l.add(new Fill(o.getLong("t"), o.getInt("km"), (float) o.getDouble("l"), o.getInt("crc"), o.getBoolean("full")));
            }
        } catch (Exception ignored) { }
        Collections.sort(l, (x, y) -> x.km != y.km ? Integer.compare(x.km, y.km) : Long.compare(x.time, y.time));
        return l;
    }

    private static void save(Context c, List<Fill> l) {
        JSONArray a = new JSONArray();
        try {
            for (Fill f : l) {
                a.put(new JSONObject().put("t", f.time).put("km", f.km).put("l", f.litres).put("crc", f.colones).put("full", f.full));
            }
        } catch (Exception ignored) { }
        p(c).edit().putString("fills", a.toString()).apply();
    }

    static void add(Context c, Fill f) {
        List<Fill> l = all(c);
        l.add(f);
        save(c, l);
        // The pump odometer reading is the best one we get: keep the GPS odometer in step.
        if (f.km > 0 && (!Car.odoSet(c) || Math.abs(f.km - Car.km(c)) > 1)) Car.setKm(c, f.km);
    }

    static void remove(Context c, Fill f) {
        List<Fill> l = all(c);
        for (int i = 0; i < l.size(); i++) {
            Fill x = l.get(i);
            if (x.time == f.time && x.km == f.km) { l.remove(i); break; }
        }
        save(c, l);
    }

    /** {km/L of the last full-to-full stretch, km/L over all of them}; 0 when not enough data. */
    static float[] economy(Context c) {
        List<Fill> l = all(c);
        float lastKmL = 0, dist = 0, litres = 0;
        int prevFull = -1;
        float since = 0;
        for (Fill f : l) {
            if (prevFull < 0) { if (f.full) prevFull = f.km; continue; }
            since += f.litres;
            if (f.full) {
                int d = f.km - prevFull;
                if (d > 0 && since > 0) {
                    lastKmL = d / since;
                    dist += d;
                    litres += since;
                }
                prevFull = f.km;
                since = 0;
            }
        }
        return new float[]{lastKmL, litres > 0 ? dist / litres : 0};
    }

    /** Colones spent this calendar month. */
    static int monthSpend(Context c) {
        Calendar now = Calendar.getInstance(), t = Calendar.getInstance();
        int sum = 0;
        for (Fill f : all(c)) {
            t.setTimeInMillis(f.time);
            if (t.get(Calendar.YEAR) == now.get(Calendar.YEAR) && t.get(Calendar.MONTH) == now.get(Calendar.MONTH)) sum += f.colones;
        }
        return sum;
    }

    /** L/100 km for estimates: measured average if known, else the CAN box figure. */
    static float lPer100(Context c) {
        float avg = economy(c)[1];
        return avg > 0 ? 100f / avg : Car.L_PER_100;
    }
}
