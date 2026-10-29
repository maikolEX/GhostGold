package com.maikolex.ghostgold;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Maintenance log. Kilometre services (oil, brakes, tyre rotation, filters) and the yearly
 * Costa Rica paperwork: marchamo (due 31 December, collection opens in November) and the RTV
 * in the month of the plate's last digit (1 = January ... 9 = September, 0 = October; to be
 * confirmed with Dekra).
 */
final class Garage {

    static final String OIL = "oil", MARCHAMO = "marchamo", RTV = "rtv";
    static final String[] MONTH = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
            "septiembre", "octubre", "noviembre", "diciembre"};

    static final class Job {
        final String id, name;
        final int every; // km; 0 for the yearly items
        final int warn;  // km before due to start warning
        Job(String id, String name, int every, int warn) {
            this.id = id; this.name = name; this.every = every; this.warn = warn;
        }
        boolean yearly() { return every == 0; }
    }

    static final Job[] JOBS = {
            new Job(OIL, "Cambio de aceite", 5000, 500),
            new Job("brakes", "Revisión de frenos", 20000, 1000),
            new Job("rotation", "Rotación de llantas", 10000, 1000),
            new Job("air", "Filtro de aire", 20000, 1000),
            new Job("cabin", "Filtro de cabina (A/C)", 15000, 1000),
            new Job("fuel", "Filtro de combustible", 40000, 2000),
            new Job(MARCHAMO, "Marchamo", 0, 0),
            new Job(RTV, "Revisión técnica (RTV)", 0, 0),
    };

    private Garage() { }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("garage", Context.MODE_PRIVATE);
    }

    // ---------- Kilometres ----------

    static int every(Context c, Job j) { return p(c).getInt(j.id + ".every", j.every); }
    static void setEvery(Context c, Job j, int km) { p(c).edit().putInt(j.id + ".every", Math.max(500, km)).apply(); }

    /** Dashboard km of the last service (defaults to today's reading the first time it is asked). */
    static int last(Context c, Job j) {
        SharedPreferences s = p(c);
        if (!s.contains(j.id + ".last") && Car.odoSet(c)) s.edit().putInt(j.id + ".last", Car.km(c)).apply();
        return s.getInt(j.id + ".last", Car.km(c));
    }

    static void setLast(Context c, Job j, int km) { p(c).edit().putInt(j.id + ".last", km).apply(); }

    static int left(Context c, Job j) {
        return last(c, j) + every(c, j) - Car.km(c);
    }

    // ---------- Yearly ----------

    /** Marchamo: the year the paid marchamo covers. RTV: the year it was passed. 0 = unknown. */
    static int year(Context c, Job j) { return p(c).getInt(j.id + ".year", 0); }
    static void setYear(Context c, Job j, int y) { p(c).edit().putInt(j.id + ".year", y).apply(); }

    static int rtvMonth(Context c) {
        int d = Store.plate(c) % 10;
        return d == 0 ? Calendar.OCTOBER : Calendar.JANUARY + d - 1;
    }

    static Calendar due(Context c, Job j) {
        int y = Calendar.getInstance().get(Calendar.YEAR);
        Calendar d = Calendar.getInstance();
        d.clear();
        if (MARCHAMO.equals(j.id)) {
            d.set(year(c, j) == 0 ? y : year(c, j), Calendar.DECEMBER, 31);
        } else {
            int done = year(c, j);
            d.set(done >= y ? done + 1 : y, rtvMonth(c), 1);
            d.add(Calendar.MONTH, 1);
            d.add(Calendar.DAY_OF_MONTH, -1);
        }
        return d;
    }

    static int days(Context c, Job j) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return (int) Math.round((due(c, j).getTimeInMillis() - today.getTimeInMillis()) / 86_400_000.0);
    }

    // ---------- Status ----------

    /** 0 fine, 1 soon, 2 overdue. */
    static int level(Context c, Job j) {
        if (!j.yearly()) {
            if (!Car.odoSet(c)) return 0;
            int l = left(c, j);
            return l < 0 ? 2 : l <= j.warn ? 1 : 0;
        }
        if (RTV.equals(j.id) && year(c, j) == 0) return 0;
        int d = days(c, j);
        if (d < 0) return 2;
        if (MARCHAMO.equals(j.id) && Calendar.getInstance().get(Calendar.MONTH) >= Calendar.NOVEMBER) return 1;
        return d <= 30 ? 1 : 0;
    }

    static String status(Context c, Job j) {
        if (!j.yearly()) {
            if (!Car.odoSet(c)) return "Pon el kilometraje del tablero";
            int l = left(c, j);
            return l < 0 ? "Vencido hace " + Car.fmt(-l) + " km" : "Faltan " + Car.fmt(l) + " km · cada " + Car.fmt(every(c, j)) + " km";
        }
        if (RTV.equals(j.id) && year(c, j) == 0) {
            return "Tu mes: " + MONTH[rtvMonth(c)] + " · marca cuándo la pasaste · confírmalo con Dekra";
        }
        Calendar d = due(c, j);
        String date = d.get(Calendar.DAY_OF_MONTH) + " de " + MONTH[d.get(Calendar.MONTH)] + " " + d.get(Calendar.YEAR);
        int n = days(c, j);
        String s = n < 0 ? "Vencido desde el " + date : n == 0 ? "Vence hoy" : "Faltan " + n + " días · " + date;
        if (MARCHAMO.equals(j.id) && Calendar.getInstance().get(Calendar.MONTH) >= Calendar.NOVEMBER && n >= 0)
            s = "Cobro abierto · " + s;
        return s;
    }

    static void done(Context c, Job j) {
        Calendar now = Calendar.getInstance();
        int y = now.get(Calendar.YEAR);
        if (!j.yearly()) setLast(c, j, Car.km(c));
        else if (MARCHAMO.equals(j.id)) setYear(c, j, now.get(Calendar.MONTH) >= Calendar.NOVEMBER ? y + 1 : y);
        else setYear(c, j, y);
    }

    static List<Job> pending(Context c) {
        List<Job> l = new ArrayList<>();
        for (int lv = 2; lv >= 1; lv--) for (Job j : JOBS) if (level(c, j) == lv) l.add(j);
        return l;
    }

    static Job oil() { return JOBS[0]; }
}
