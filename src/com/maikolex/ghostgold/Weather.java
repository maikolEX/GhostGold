package com.maikolex.ghostgold;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/** Current weather from Open-Meteo (free, no key) at the truck's position, cached for offline starts. */
final class Weather {

    interface Done { void on(Weather w); }

    final int temp, max, min, code;
    final boolean day;
    final String place;

    private Weather(int temp, int max, int min, int code, boolean day, String place) {
        this.temp = temp; this.max = max; this.min = min; this.code = code; this.day = day; this.place = place;
    }

    /** WMO weather code in Spanish. */
    String sky() {
        if (code == 0) return day ? "Despejado" : "Noche despejada";
        if (code <= 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code <= 48) return "Neblina";
        if (code <= 57) return "Llovizna";
        if (code <= 67 || (code >= 80 && code <= 82)) return "Lluvia";
        if (code >= 95) return "Tormenta";
        return "Nublado";
    }

    int glyph() {
        if (code == 0) return day ? Glyph.SUN : Glyph.MOON;
        if (code <= 3) return Glyph.CLOUD;
        if (code <= 48) return Glyph.FOG;
        if (code >= 95) return Glyph.STORM;
        if (code >= 51) return Glyph.RAIN;
        return Glyph.CLOUD;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("weather", Context.MODE_PRIVATE);
    }

    static Weather cached(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.contains("temp")) return null;
        return new Weather(p.getInt("temp", 0), p.getInt("max", 0), p.getInt("min", 0), p.getInt("code", 3),
                p.getBoolean("day", true), p.getString("place", "San José"));
    }

    static void fetch(Context ctx, Done done) {
        Context c = ctx.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                double lat = 9.9281, lon = -84.0907; // San José until there is a fix
                Location l = last(c);
                if (l != null) { lat = l.getLatitude(); lon = l.getLongitude(); }
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min"
                        + "&timezone=auto&forecast_days=1";
                JSONObject j = new JSONObject(get(url));
                JSONObject cur = j.getJSONObject("current");
                JSONObject d = j.getJSONObject("daily");
                String place = place(c, lat, lon);
                Weather w = new Weather((int) Math.round(cur.getDouble("temperature_2m")),
                        (int) Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)),
                        (int) Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)),
                        cur.getInt("weather_code"), cur.getInt("is_day") == 1, place);
                prefs(c).edit().putInt("temp", w.temp).putInt("max", w.max).putInt("min", w.min)
                        .putInt("code", w.code).putBoolean("day", w.day).putString("place", w.place).apply();
                main.post(() -> done.on(w));
            } catch (Exception ignored) {
            }
        }).start();
    }

    private static Location last(Context c) {
        if (c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return null;
        LocationManager lm = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        for (String p : lm.getProviders(true)) {
            try {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException ignored) { }
        }
        return best;
    }

    private static String place(Context c, double lat, double lon) {
        try {
            java.util.List<android.location.Address> a = new android.location.Geocoder(c, new java.util.Locale("es", "CR"))
                    .getFromLocation(lat, lon, 1);
            if (a != null && !a.isEmpty()) {
                String s = a.get(0).getLocality();
                if (s == null) s = a.get(0).getSubAdminArea();
                if (s != null) return s;
            }
        } catch (Exception ignored) { }
        return prefs(c).getString("place", "San José");
    }

    private static String get(String u) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
        h.setConnectTimeout(8000);
        h.setReadTimeout(8000);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(h.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally {
            h.disconnect();
        }
    }
}
