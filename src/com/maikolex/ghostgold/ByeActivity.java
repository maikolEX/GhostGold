package com.maikolex.ghostgold;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Switch-off goodbye: the trip in numbers and what to remember for tomorrow. Fades out by itself. */
public class ByeActivity extends Activity {

    private final Handler h = new Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        float km = Car.tripKm(this), hours = Car.tripMovingHours(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(G.VOID);
        LinearLayout c = G.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.addView(new Emblem(this), new LinearLayout.LayoutParams(G.dp(this, 64), G.dp(this, 64)));
        String n = Store.name(this);
        TextView bye = G.label(this, n.isEmpty() ? "Hasta pronto" : "Hasta pronto, " + n);
        bye.setTextSize(13);
        bye.setLetterSpacing(0.35f);
        bye.setTextColor(G.GOLD);
        bye.setPadding(0, G.dp(this, 18), 0, 0);
        c.addView(bye);
        TextView k = G.label(this, "Resumen del viaje");
        k.setPadding(0, G.dp(this, 8), 0, 0);
        c.addView(k);

        LinearLayout big = new LinearLayout(this);
        big.setGravity(Gravity.BOTTOM);
        TextView d = G.value(this, String.format(Locale.US, "%.1f", km), 88, G.IGNITION);
        d.setShadowLayer(G.dp(this, 18), 0, 0, 0x59FFD700);
        big.addView(d);
        TextView u = G.label(this, "km");
        u.setPadding(G.dp(this, 10), 0, 0, G.dp(this, 16));
        big.addView(u);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-2, -2);
        blp.topMargin = G.dp(this, 18);
        c.addView(big, blp);

        LinearLayout stats = new LinearLayout(this);
        stats.addView(stat("Duración", duration(Car.tripMillis(this))));
        stats.addView(stat("Promedio", (hours > 0 ? Math.round(km / hours) : 0) + " km/h"));
        stats.addView(stat("Máxima", Car.tripMax(this) + " km/h"));
        stats.addView(stat("Combustible aprox.", String.format(Locale.US, "%.1f L", km * Car.L_PER_100 / 100f)));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-2, -2);
        slp.topMargin = G.dp(this, 26);
        c.addView(stats, slp);

        View line = G.hairline(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(G.dp(this, 520), G.dp(this, 1));
        llp.setMargins(0, G.dp(this, 26), 0, G.dp(this, 18));
        c.addView(line, llp);
        boolean tomorrow = Rules.tomorrow(this);
        TextView t = G.text(this, tomorrow ? "Mañana no circulas en San José (6:00 a 19:00)" : "Mañana circulas sin restricción",
                17, tomorrow ? G.AMBER : G.TEXT);
        c.addView(t);
        List<Garage.Job> due = Garage.pending(this);
        if (!due.isEmpty()) {
            TextView m = G.text(this, due.get(0).name + " · " + Garage.status(this, due.get(0)), 15, G.CHAMPAGNE);
            m.setPadding(0, G.dp(this, 8), 0, 0);
            c.addView(m);
        }
        root.addView(c, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.setOnClickListener(v -> finish());
        setContentView(root);

        c.setAlpha(0f);
        c.setTranslationY(G.dp(this, 16));
        c.animate().alpha(1f).translationY(0).setDuration(900).start();
        h.postDelayed(() -> c.animate().alpha(0f).setDuration(1500).withEndAction(this::finish).start(), 14_000);
    }

    private View stat(String label, String value) {
        LinearLayout s = G.column(this);
        s.setGravity(Gravity.CENTER_HORIZONTAL);
        s.setPadding(G.dp(this, 26), 0, G.dp(this, 26), 0);
        s.addView(G.label(this, label));
        TextView v = G.value(this, value, 28, G.WHITE);
        v.setPadding(0, G.dp(this, 8), 0, 0);
        s.addView(v);
        return s;
    }

    private static String duration(long ms) {
        long m = ms / 60_000;
        return m < 60 ? m + " min" : (m / 60) + " h " + (m % 60) + " min";
    }

    @Override protected void onDestroy() { super.onDestroy(); h.removeCallbacksAndMessages(null); }
}
