package com.maikolex.ghostgold;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/** What needs the driver's attention now, most urgent first. */
final class Alerts {

    static final class Alert {
        final String title, detail;
        final int level; // 1 soon, 2 now
        Alert(String t, String d, int l) { title = t; detail = d; level = l; }
    }

    private Alerts() { }

    static List<Alert> now(Context c) {
        List<Alert> l = new ArrayList<>();
        if (Rules.now(c)) l.add(new Alert("Restricción", "Hoy no circulas en San José hasta las 19:00", 2));
        for (Garage.Job j : Garage.pending(c)) l.add(new Alert(j.name, Garage.status(c, j), Garage.level(c, j)));
        return l;
    }

    static int worst(Context c) {
        int w = 0;
        for (Alert a : now(c)) w = Math.max(w, a.level);
        return w;
    }
}
