package com.maikolex.ghostgold;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** GHOST GOLD radio: big frequency, aircraft-style tuning tape, seek, and the saved stations. */
public class RadioActivity extends Activity {

    private final Handler h = new Handler();
    private final Tuner tuner = new Tuner();
    private TextView freq, unit, rds, bandChip, listen;
    private Dial dial;
    private LinearLayout presets;
    private String presetKey = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = G.column(this);
        root.setPadding(G.dp(this, 28), G.dp(this, 18), G.dp(this, 28), G.dp(this, 18));
        root.setBackgroundColor(G.VOID);
        LinearLayout head = G.header(this, "Ghost Gold · Audio", "Radio");
        bandChip = G.chip(this, "FM1", () -> { tuner.band(); later(); });
        head.addView(bandChip);
        listen = G.chip(this, "Escuchar radio", () -> { tuner.select(this); h.postDelayed(this::show, 2500); });
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-2, -2);
        llp.leftMargin = G.dp(this, 12);
        head.addView(listen, llp);
        root.addView(head);

        LinearLayout big = G.row(this);
        big.setGravity(Gravity.CENTER);
        freq = G.value(this, "--.-", 120, G.WHITE);
        freq.setShadowLayer(G.dp(this, 20), 0, 0, 0x40FFD700);
        big.addView(freq);
        unit = G.label(this, "MHz");
        unit.setTextSize(14);
        unit.setTextColor(G.GOLD);
        unit.setPadding(G.dp(this, 14), G.dp(this, 40), 0, 0);
        big.addView(unit);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.topMargin = G.dp(this, 10);
        root.addView(big, blp);
        rds = G.label(this, "");
        rds.setGravity(Gravity.CENTER);
        rds.setTextSize(12);
        rds.setTextColor(G.CHAMPAGNE);
        root.addView(rds, new LinearLayout.LayoutParams(-1, -2));

        dial = new Dial(this);
        dial.onTune(f -> {
            Tuner.Station s = tuner.current();
            tuner.tune(s == null ? 0 : s.band, f, -1);
            later();
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, G.dp(this, 96));
        dlp.topMargin = G.dp(this, 12);
        root.addView(dial, dlp);

        LinearLayout keys = G.row(this);
        keys.setGravity(Gravity.CENTER);
        keys.addView(G.button(this, Glyph.SEEK_DOWN, 56, false, () -> { tuner.seek(false); later(); }));
        keys.addView(gap());
        keys.addView(G.button(this, Glyph.PREV, 56, false, () -> { tuner.step(false); later(); }));
        keys.addView(gap());
        keys.addView(G.button(this, Glyph.NEXT, 56, false, () -> { tuner.step(true); later(); }));
        keys.addView(gap());
        keys.addView(G.button(this, Glyph.SEEK_UP, 56, false, () -> { tuner.seek(true); later(); }));
        LinearLayout.LayoutParams klp = new LinearLayout.LayoutParams(-1, -2);
        klp.topMargin = G.dp(this, 10);
        root.addView(keys, klp);

        TextView pl = G.label(this, "Emisoras guardadas");
        pl.setPadding(0, G.dp(this, 18), 0, G.dp(this, 10));
        root.addView(pl);
        presets = G.row(this);
        root.addView(presets, new LinearLayout.LayoutParams(-1, G.dp(this, 70)));
        setContentView(root);
        tuner.bind(this);
    }

    private android.view.View gap() {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(G.dp(this, 28), 1));
        return v;
    }

    private void later() {
        h.postDelayed(this::show, 350);
        h.postDelayed(this::show, 1500);
    }

    private void show() {
        Tuner.Station s = tuner.current();
        boolean on = Tuner.isSource(this);
        listen.setVisibility(on ? android.view.View.GONE : android.view.View.VISIBLE);
        if (s == null) {
            rds.setText(Head.isNwd(this) ? "Conectando con la radio…" : "Esta radio no tiene el sintonizador NWD");
            return;
        }
        freq.setText(s.freq());
        unit.setText(s.am() ? "kHz" : "MHz");
        rds.setText(s.name.isEmpty() ? (on ? "En el aire" : "Radio en pausa · toca Escuchar radio") : s.name);
        bandChip.setText(s.band());
        if (!s.am()) dial.setFrequency(s.freq);
        dial.setVisibility(s.am() ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
        showPresets(s);
    }

    private void showPresets(Tuner.Station cur) {
        List<Tuner.Station> list = tuner.presets();
        StringBuilder key = new StringBuilder(cur.band + ":" + cur.freq);
        for (Tuner.Station p : list) key.append(',').append(p.freq);
        if (key.toString().equals(presetKey)) return;
        presetKey = key.toString();
        presets.removeAllViews();
        List<Integer> pips = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Tuner.Station p = list.get(i);
            pips.add(p.freq);
            final int slot = i;
            boolean here = p.freq == cur.freq;
            LinearLayout cell = G.column(this);
            cell.setGravity(Gravity.CENTER);
            cell.setBackground(G.pill(this, here ? G.TINT : 0, here ? G.GOLD : G.HAIR));
            TextView f = G.value(this, p.freq(), 24, here ? G.IGNITION : G.WHITE);
            cell.addView(f);
            TextView n = G.label(this, p.name.isEmpty() ? "P" + (i + 1) : p.name);
            n.setTextSize(8);
            n.setPadding(0, G.dp(this, 4), 0, 0);
            cell.addView(n);
            G.press(cell);
            cell.setOnClickListener(v -> { tuner.tune(p.band, p.freq, slot); later(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            lp.setMargins(G.dp(this, 5), 0, G.dp(this, 5), 0);
            presets.addView(cell, lp);
        }
        dial.setPresets(pips);
    }

    private final Runnable poll = new Runnable() {
        public void run() {
            show();
            h.postDelayed(this, 1000);
        }
    };

    @Override protected void onResume() { super.onResume(); h.postDelayed(poll, 300); }
    @Override protected void onPause() { super.onPause(); h.removeCallbacks(poll); }
    @Override protected void onDestroy() { super.onDestroy(); h.removeCallbacksAndMessages(null); tuner.unbind(this); }
}
